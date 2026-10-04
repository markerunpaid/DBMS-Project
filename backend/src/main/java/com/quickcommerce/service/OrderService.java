package com.quickcommerce.service;

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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
