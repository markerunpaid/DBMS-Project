package com.quickcommerce.service;

import com.quickcommerce.dto.CustomerDtos.FulfillmentPlanDto;
import com.quickcommerce.dto.CustomerDtos.FulfillmentPlanPackage;
import com.quickcommerce.dto.CustomerDtos.MultiStorePlaceOrderRequest;
import com.quickcommerce.dto.CustomerDtos.NearestStoreDto;
import com.quickcommerce.dto.CustomerDtos.OrderItemPreview;
import com.quickcommerce.dto.CustomerDtos.PlanOrderRequest;
import com.quickcommerce.dto.OrderDtos.OrderDto;
import com.quickcommerce.dto.OrderDtos.OrderLine;
import com.quickcommerce.dto.OrderDtos.PlaceOrderRequest;
import com.quickcommerce.dto.OrderDtos.ReceiptDto;
import com.quickcommerce.entity.*;
import com.quickcommerce.exception.ApiException;
import com.quickcommerce.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Customer side of the order life cycle: place, view, receipt, cancel. */
@Service
public class OrderService {

    private static final EnumSet<OrderStatus> CANCELLABLE = EnumSet.of(OrderStatus.PLACED, OrderStatus.PACKED);

    private final OrderRepository orders;
    private final OrderProductRepository orderProducts;
    private final OrderTimerRepository timers;
    private final PaymentRecordRepository payments;
    private final CustomerRepository customers;
    private final DarkStoreRepository stores;
    private final ProductRepository products;
    private final InventoryRepository inventory;
    private final CouponRepository coupons;
    private final CustomerCouponRepository customerCoupons;
    private final CustomerService customerService;
    private final DispatchService dispatch;
    private final DeliveryRules rules;
    private final OrderMapper mapper;

    public OrderService(OrderRepository orders, OrderProductRepository orderProducts, OrderTimerRepository timers,
                        PaymentRecordRepository payments, CustomerRepository customers, DarkStoreRepository stores,
                        ProductRepository products, InventoryRepository inventory, CouponRepository coupons,
                        CustomerCouponRepository customerCoupons, CustomerService customerService,
                        DispatchService dispatch, DeliveryRules rules, OrderMapper mapper) {
        this.orders = orders;
        this.orderProducts = orderProducts;
        this.timers = timers;
        this.payments = payments;
        this.customers = customers;
        this.stores = stores;
        this.products = products;
        this.inventory = inventory;
        this.coupons = coupons;
        this.customerCoupons = customerCoupons;
        this.customerService = customerService;
        this.dispatch = dispatch;
        this.rules = rules;
        this.mapper = mapper;
    }

    /**
     * One transaction: any failure (out of stock, bad coupon, ...) rolls back every
     * inventory decrement and insert made so far.
     */
    @Transactional
    public OrderDto place(Long customerId, PlaceOrderRequest req) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> ApiException.notFound("Customer not found"));
        Address deliverTo = customerService.requireOwnAddress(customerId, req.addressId()).getAddress();
        DarkStore store = stores.findById(req.storeId())
                .orElseThrow(() -> ApiException.notFound("Store not found"));

        double km = DeliveryRules.distanceKm(store.getAddress(), deliverTo);
        if (km > DeliveryRules.DELIVERY_RADIUS_KM) {
            throw ApiException.badRequest(String.format(
                    "%s is %.1f km away; we only deliver within %.0f km", store.getName(), km,
                    DeliveryRules.DELIVERY_RADIUS_KM));
        }
        if (!rules.isOpen(store.getId(), now)) {
            throw ApiException.conflict(store.getName() + " is closed right now");
        }

        // merge repeated lines for the same product
        Map<Long, Integer> wanted = new LinkedHashMap<>();
        for (OrderLine line : req.items()) {
            wanted.merge(line.productId(), line.quantity(), Integer::sum);
        }

        // 1. take stock + price the cart
        BigDecimal subtotal = BigDecimal.ZERO;
        Map<Product, Integer> lines = new LinkedHashMap<>();
        for (var e : wanted.entrySet()) {
            Product p = products.findById(e.getKey())
                    .orElseThrow(() -> ApiException.notFound("Product " + e.getKey() + " not found"));
            if (inventory.decrement(store.getId(), p.getId(), e.getValue()) == 0) {
                int left = inventory.findById(new Inventory.Key(store.getId(), p.getId()))
                        .map(Inventory::getQuantity).orElse(0);
                throw ApiException.conflict("Only " + left + " x " + p.getName() + " left at " + store.getName());
            }
            lines.put(p, e.getValue());
            subtotal = subtotal.add(p.getPrice().multiply(BigDecimal.valueOf(e.getValue())));
        }

        // 2. coupon
        Coupon coupon = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (req.couponCode() != null && !req.couponCode().isBlank()) {
            coupon = coupons.findByCodeIgnoreCase(req.couponCode().trim())
                    .orElseThrow(() -> ApiException.badRequest("Coupon code not recognised"));
            CustomerCoupon owned = customerCoupons.findById(new CustomerCoupon.Key(customerId, coupon.getId()))
                    .orElseThrow(() -> ApiException.badRequest("This coupon isn't available on your account"));
            if (owned.getRedeemedAt() != null) {
                throw ApiException.badRequest("You have already used this coupon");
            }
            if (!coupon.isValidAt(now)) {
                throw ApiException.badRequest("This coupon has expired or isn't active yet");
            }
            discount = coupon.getDiscountType() == DiscountType.PERCENTAGE
                    ? subtotal.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : coupon.getValue();
            discount = discount.min(subtotal);
            owned.setRedeemedAt(now);
        }

        // 3. order + line items (price frozen as price_at_order)
        Order order = new Order();
        order.setCustomer(customer);
        order.setDarkStore(store);
        order.setDeliveryAddress(deliverTo);
        order.setCoupon(coupon);
        order.setAmount(subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP));
        order.setStatus(OrderStatus.PLACED);
        orders.save(order);

        for (var e : lines.entrySet()) {
            OrderProduct op = new OrderProduct();
            op.setId(new OrderProduct.Key(order.getId(), e.getKey().getId()));
            op.setOrder(order);
            op.setProduct(e.getKey());
            op.setQuantity(e.getValue());
            op.setPriceAtOrder(e.getKey().getPrice());
            order.getItems().add(orderProducts.save(op));
        }

        // 4. order timer -- the clock starts now
        OrderTimer timer = new OrderTimer();
        timer.setOrder(order);
        timer.setPlacedAt(now);
        timer.setExpectedDeliveryAt(now.plusMinutes(DeliveryRules.etaMinutes(km)));
        timers.save(timer);
        order.setTimer(timer);

        // 5. payment (online modes are simulated as instantly successful; cash is collected on delivery)
        PaymentRecord pay = new PaymentRecord();
        pay.setOrder(order);
        pay.setAmount(order.getAmount());
        pay.setMode(req.paymentMode());
        pay.setStatus(req.paymentMode() == PaymentMode.CASH ? PaymentStatus.PENDING : PaymentStatus.SUCCESS);
        pay.setPaidAt(now);
        payments.save(pay);
        order.setPayment(pay);

        // 6. hand it straight to a free delivery partner of this store (or it waits for the next free one)
        dispatch.assign(order);

        return mapper.toDto(order);
    }

    /**
     * Determines how a customer's items can be fulfilled across open deliverable dark stores.
     * Tries single-store fulfillment first, or splits across stores if inventory is distributed.
     */
    @Transactional(readOnly = true)
    public FulfillmentPlanDto buildPlan(Long customerId, PlanOrderRequest req) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> ApiException.notFound("Customer not found"));
        Address deliverTo = customerService.requireOwnAddress(customerId, req.addressId()).getAddress();

        List<NearestStoreDto> openStores = customerService.getDeliverableStores(customerId, req.addressId());
        if (openStores.isEmpty()) {
            throw ApiException.badRequest("No dark stores are currently open and deliverable to your address");
        }

        Map<Long, Integer> wanted = new LinkedHashMap<>();
        for (OrderLine line : req.items()) {
            if (line.quantity() > 0) {
                wanted.merge(line.productId(), line.quantity(), Integer::sum);
            }
        }
        if (wanted.isEmpty()) {
            throw ApiException.badRequest("Cart is empty");
        }

        Map<Long, Product> productMap = new LinkedHashMap<>();
        for (Long pId : wanted.keySet()) {
            Product p = products.findById(pId)
                    .orElseThrow(() -> ApiException.notFound("Product " + pId + " not found"));
            productMap.put(pId, p);
        }

        List<Long> storeIds = openStores.stream().map(NearestStoreDto::id).toList();
        List<Inventory> invRows = inventory.findAvailableInStores(storeIds);
        Map<Long, Map<Long, Integer>> stockByStore = new HashMap<>();
        for (Inventory inv : invRows) {
            stockByStore.computeIfAbsent(inv.getId().getDarkStoreId(), k -> new HashMap<>())
                    .put(inv.getId().getProductId(), inv.getQuantity());
        }

        // 1. Try single-store fulfillment first (closest store that can fulfill ALL wanted items)
        NearestStoreDto singleStoreMatch = null;
        for (NearestStoreDto s : openStores) {
            Map<Long, Integer> storeStock = stockByStore.getOrDefault(s.id(), Map.of());
            boolean canFulfillAll = true;
            for (var entry : wanted.entrySet()) {
                if (storeStock.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                    canFulfillAll = false;
                    break;
                }
            }
            if (canFulfillAll) {
                singleStoreMatch = s;
                break;
            }
        }

        List<FulfillmentPlanPackage> packages = new ArrayList<>();
        BigDecimal totalSubtotal = BigDecimal.ZERO;
        int totalItemsCount = 0;

        if (singleStoreMatch != null) {
            List<OrderItemPreview> items = new ArrayList<>();
            BigDecimal pkgSubtotal = BigDecimal.ZERO;
            for (var entry : wanted.entrySet()) {
                Product p = productMap.get(entry.getKey());
                int qty = entry.getValue();
                BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(qty));
                items.add(new OrderItemPreview(p.getId(), p.getName(), p.getUnit(), qty, p.getPrice(), lineTotal));
                pkgSubtotal = pkgSubtotal.add(lineTotal);
                totalItemsCount += qty;
            }
            totalSubtotal = pkgSubtotal;
            packages.add(new FulfillmentPlanPackage(
                    singleStoreMatch.id(), singleStoreMatch.name(), singleStoreMatch.address(),
                    singleStoreMatch.distanceKm(), singleStoreMatch.etaMinutes(),
                    items, pkgSubtotal));
        } else {
            // Split across stores using greedy selection (closest store first)
            Map<Long, Integer> remaining = new LinkedHashMap<>(wanted);
            for (NearestStoreDto s : openStores) {
                Map<Long, Integer> storeStock = stockByStore.getOrDefault(s.id(), Map.of());
                List<OrderItemPreview> storeItems = new ArrayList<>();
                BigDecimal pkgSubtotal = BigDecimal.ZERO;

                for (var entry : new ArrayList<>(remaining.entrySet())) {
                    Long pId = entry.getKey();
                    int needed = entry.getValue();
                    int avail = storeStock.getOrDefault(pId, 0);
                    if (avail > 0) {
                        int take = Math.min(avail, needed);
                        Product p = productMap.get(pId);
                        BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(take));
                        storeItems.add(new OrderItemPreview(p.getId(), p.getName(), p.getUnit(), take, p.getPrice(), lineTotal));
                        pkgSubtotal = pkgSubtotal.add(lineTotal);
                        totalItemsCount += take;

                        if (take == needed) {
                            remaining.remove(pId);
                        } else {
                            remaining.put(pId, needed - take);
                        }
                    }
                }

                if (!storeItems.isEmpty()) {
                    packages.add(new FulfillmentPlanPackage(
                            s.id(), s.name(), s.address(), s.distanceKm(), s.etaMinutes(),
                            storeItems, pkgSubtotal));
                    totalSubtotal = totalSubtotal.add(pkgSubtotal);
                }

                if (remaining.isEmpty()) {
                    break;
                }
            }

            if (!remaining.isEmpty()) {
                String missingDesc = remaining.entrySet().stream()
                        .map(e -> e.getValue() + " x " + productMap.get(e.getKey()).getName())
                        .collect(Collectors.joining(", "));
                throw ApiException.conflict("Insufficient stock across all nearby stores for: " + missingDesc);
            }
        }

        boolean isSplit = packages.size() > 1;
        return new FulfillmentPlanDto(packages, totalSubtotal, totalItemsCount, isSplit);
    }

    /**
     * Places one or multiple orders across dark stores according to the fulfillment plan.
     * All inventory changes, orders, timers, and partner dispatches are atomic.
     */
    @Transactional
    public List<OrderDto> placeMulti(Long customerId, MultiStorePlaceOrderRequest req) {
        FulfillmentPlanDto plan = buildPlan(customerId, new PlanOrderRequest(req.addressId(), req.items()));
        if (plan.packages().isEmpty()) {
            throw ApiException.badRequest("No packages to fulfill");
        }

        LocalDateTime now = LocalDateTime.now().withNano(0);
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> ApiException.notFound("Customer not found"));
        Address deliverTo = customerService.requireOwnAddress(customerId, req.addressId()).getAddress();

        Coupon coupon = null;
        BigDecimal remainingDiscount = BigDecimal.ZERO;
        if (req.couponCode() != null && !req.couponCode().isBlank()) {
            coupon = coupons.findByCodeIgnoreCase(req.couponCode().trim())
                    .orElseThrow(() -> ApiException.badRequest("Coupon code not recognised"));
            CustomerCoupon owned = customerCoupons.findById(new CustomerCoupon.Key(customerId, coupon.getId()))
                    .orElseThrow(() -> ApiException.badRequest("This coupon isn't available on your account"));
            if (owned.getRedeemedAt() != null) {
                throw ApiException.badRequest("You have already used this coupon");
            }
            if (!coupon.isValidAt(now)) {
                throw ApiException.badRequest("This coupon has expired or isn't active yet");
            }
            remainingDiscount = coupon.getDiscountType() == DiscountType.PERCENTAGE
                    ? plan.subtotal().multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : coupon.getValue();
            remainingDiscount = remainingDiscount.min(plan.subtotal());
            owned.setRedeemedAt(now);
        }

        List<OrderDto> resultOrders = new ArrayList<>();

        for (FulfillmentPlanPackage pkg : plan.packages()) {
            DarkStore store = stores.findById(pkg.storeId())
                    .orElseThrow(() -> ApiException.notFound("Store not found: " + pkg.storeId()));

            double km = DeliveryRules.distanceKm(store.getAddress(), deliverTo);
            if (!rules.isOpen(store.getId(), now)) {
                throw ApiException.conflict(store.getName() + " is closed right now");
            }

            // 1. decrement stock
            for (OrderItemPreview item : pkg.items()) {
                if (inventory.decrement(store.getId(), item.productId(), item.quantity()) == 0) {
                    int left = inventory.findById(new Inventory.Key(store.getId(), item.productId()))
                            .map(Inventory::getQuantity).orElse(0);
                    throw ApiException.conflict("Only " + left + " x " + item.name() + " left at " + store.getName());
                }
            }

            // 2. apply coupon discount to packages
            BigDecimal pkgDiscount = BigDecimal.ZERO;
            Coupon pkgCoupon = null;
            if (remainingDiscount.compareTo(BigDecimal.ZERO) > 0) {
                pkgDiscount = remainingDiscount.min(pkg.subtotal());
                remainingDiscount = remainingDiscount.subtract(pkgDiscount);
                pkgCoupon = coupon;
            }

            // 3. create order + line items
            Order order = new Order();
            order.setCustomer(customer);
            order.setDarkStore(store);
            order.setDeliveryAddress(deliverTo);
            order.setCoupon(pkgCoupon);
            order.setAmount(pkg.subtotal().subtract(pkgDiscount).setScale(2, RoundingMode.HALF_UP));
            order.setStatus(OrderStatus.PLACED);
            orders.save(order);

            for (OrderItemPreview item : pkg.items()) {
                Product p = products.findById(item.productId()).orElseThrow();
                OrderProduct op = new OrderProduct();
                op.setId(new OrderProduct.Key(order.getId(), p.getId()));
                op.setOrder(order);
                op.setProduct(p);
                op.setQuantity(item.quantity());
                op.setPriceAtOrder(p.getPrice());
                order.getItems().add(orderProducts.save(op));
            }

            // 4. order timer
            OrderTimer timer = new OrderTimer();
            timer.setOrder(order);
            timer.setPlacedAt(now);
            timer.setExpectedDeliveryAt(now.plusMinutes(DeliveryRules.etaMinutes(km)));
            timers.save(timer);
            order.setTimer(timer);

            // 5. payment record
            PaymentRecord pay = new PaymentRecord();
            pay.setOrder(order);
            pay.setAmount(order.getAmount());
            pay.setMode(req.paymentMode());
            pay.setStatus(req.paymentMode() == PaymentMode.CASH ? PaymentStatus.PENDING : PaymentStatus.SUCCESS);
            pay.setPaidAt(now);
            payments.save(pay);
            order.setPayment(pay);

            // 6. dispatch partner assignment
            dispatch.assign(order);

            resultOrders.add(mapper.toDto(order));
        }

        return resultOrders;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> myOrders(Long customerId) {
        return orders.findByCustomerIdOrderByIdDesc(customerId).stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto myOrder(Long customerId, Long orderId) {
        return mapper.toDto(requireOwnOrder(customerId, orderId));
    }

    @Transactional(readOnly = true)
    public ReceiptDto receipt(Long customerId, Long orderId) {
        return mapper.toReceipt(requireOwnOrder(customerId, orderId));
    }

    @Transactional
    public OrderDto cancel(Long customerId, Long orderId) {
        Order order = requireOwnOrder(customerId, orderId);
        if (!CANCELLABLE.contains(order.getStatus())) {
            throw ApiException.badRequest("Order is " + order.getStatus() + " and can no longer be cancelled");
        }
        LocalDateTime now = LocalDateTime.now().withNano(0);
        order.setStatus(OrderStatus.CANCELLED);
        order.getTimer().setCancelledAt(now);

        // put the stock back on the shelf
        for (OrderProduct op : order.getItems()) {
            inventory.increment(order.getDarkStore().getId(), op.getProduct().getId(), op.getQuantity());
        }
        // give the coupon back
        if (order.getCoupon() != null) {
            customerCoupons.findById(new CustomerCoupon.Key(customerId, order.getCoupon().getId()))
                    .ifPresent(cc -> cc.setRedeemedAt(null));
        }
        PaymentRecord pay = order.getPayment();
        if (pay != null) {
            pay.setStatus(pay.getStatus() == PaymentStatus.SUCCESS ? PaymentStatus.REFUNDED : PaymentStatus.FAILED);
        }
        if (order.getDeliveryPartner() != null) {
            dispatch.release(order.getDeliveryPartner());
        }
        return mapper.toDto(order);
    }

    private Order requireOwnOrder(Long customerId, Long orderId) {
        return orders.findById(orderId)
                .filter(o -> o.getCustomer().getId().equals(customerId))
                .orElseThrow(() -> ApiException.notFound("Order not found"));
    }
}
