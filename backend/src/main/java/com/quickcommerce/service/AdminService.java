package com.quickcommerce.service;

import com.quickcommerce.dto.AdminDtos.AddStockRequest;
import com.quickcommerce.dto.AdminDtos.AdminStoreDto;
import com.quickcommerce.dto.AdminDtos.CategoryDto;
import com.quickcommerce.dto.AdminDtos.HoursDto;
import com.quickcommerce.dto.AdminDtos.InventoryItemDto;
import com.quickcommerce.dto.AdminDtos.NewProductRequest;
import com.quickcommerce.dto.AdminDtos.PartnerSummaryDto;
import com.quickcommerce.dto.AdminDtos.ProductDto;
import com.quickcommerce.dto.OrderDtos.OrderDto;
import com.quickcommerce.entity.*;
import com.quickcommerce.exception.ApiException;
import com.quickcommerce.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/** Store manager: sees and runs only the dark stores where dark_store.manager_employee_id = them. */
@Service
public class AdminService {

    private static final List<String> WEEK = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
    private static final EnumSet<OrderStatus> OPEN_ORDERS =
            EnumSet.of(OrderStatus.PLACED, OrderStatus.PACKED, OrderStatus.OUT_FOR_DELIVERY);

    private final DarkStoreRepository stores;
    private final InventoryRepository inventory;
    private final OperatingHoursRepository hours;
    private final OrderRepository orders;
    private final DeliveryPartnerRepository partners;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final DispatchService dispatch;
    private final DeliveryRules rules;
    private final OrderMapper mapper;
    private final EntityManager em;

    public AdminService(DarkStoreRepository stores, InventoryRepository inventory, OperatingHoursRepository hours,
                        OrderRepository orders, DeliveryPartnerRepository partners, ProductRepository products,
                        CategoryRepository categories, DispatchService dispatch, DeliveryRules rules, OrderMapper mapper,
                        EntityManager em) {
        this.stores = stores;
        this.inventory = inventory;
        this.hours = hours;
        this.orders = orders;
        this.partners = partners;
        this.products = products;
        this.categories = categories;
        this.dispatch = dispatch;
        this.rules = rules;
        this.mapper = mapper;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public List<AdminStoreDto> myStores(Long adminId) {
        LocalDateTime now = LocalDateTime.now();
        return stores.findByManagerEmployeeIdOrderByName(adminId).stream().map(s -> {
            List<Inventory> stock = inventory.findByIdDarkStoreIdOrderByProductCategoryNameAscProductNameAsc(s.getId());
            List<HoursDto> week = hours.findByIdDarkStoreId(s.getId()).stream()
                    .sorted(Comparator.comparingInt(h -> WEEK.indexOf(h.getId().getDayOfWeek())))
                    .map(h -> new HoursDto(h.getId().getDayOfWeek(), h.getOpensAt(), h.getClosesAt()))
                    .toList();
            return new AdminStoreDto(
                    s.getId(), s.getName(), s.getAddress().format(), rules.isOpen(s.getId(), now), week,
                    stock.size(),
                    (int) stock.stream().filter(i -> i.getQuantity() == 0).count(),
                    orders.countByDarkStoreIdAndStatusIn(s.getId(), OPEN_ORDERS));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryItemDto> storeInventory(Long adminId, Long storeId) {
        requireManagedStore(adminId, storeId);
        return inventory.findByIdDarkStoreIdOrderByProductCategoryNameAscProductNameAsc(storeId).stream()
                .map(AdminService::toDto)
                .toList();
    }

    @Transactional
    public InventoryItemDto updateStock(Long adminId, Long storeId, Long productId, int quantity) {
        requireManagedStore(adminId, storeId);
        Inventory inv = inventory.findById(new Inventory.Key(storeId, productId))
                .orElseThrow(() -> ApiException.notFound("This product isn't stocked at this store"));
        inv.setQuantity(quantity);
        em.flush();
        em.refresh(inv);   // pick up updated_at set by MySQL
        return toDto(inv);
    }

    // ---------- catalog: add / create / remove items ----------

    @Transactional(readOnly = true)
    public List<CategoryDto> categories() {
        return categories.findAllByOrderByName().stream().map(c -> new CategoryDto(c.getId(), c.getName())).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDto> catalog() {
        return products.findAllByOrderByCategoryNameAscNameAsc().stream()
                .map(p -> new ProductDto(p.getId(), p.getName(), p.getUnit(), p.getPrice(), p.getExpiry(),
                        p.getCategory().getId(), p.getCategory().getName()))
                .toList();
    }

    /** Put an existing catalog product into this store's inventory. */
    @Transactional
    public InventoryItemDto addToStore(Long adminId, Long storeId, AddStockRequest req) {
        requireManagedStore(adminId, storeId);
        Product p = products.findById(req.productId())
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        if (inventory.existsById(new Inventory.Key(storeId, p.getId()))) {
            throw ApiException.conflict(p.getName() + " is already in this store -- change its stock instead");
        }
        return stock(storeId, p, req.quantity());
    }

    /** Create a new catalog product (and category, if new) and stock it at this store. */
    @Transactional
    public InventoryItemDto createProduct(Long adminId, Long storeId, NewProductRequest req) {
        requireManagedStore(adminId, storeId);
        Category category = resolveCategory(req);
        if (products.existsByNameIgnoreCaseAndUnitIgnoreCase(req.name().trim(), req.unit().trim())) {
            throw ApiException.conflict(req.name().trim() + " (" + req.unit().trim()
                    + ") already exists -- use \"Add existing product\" instead");
        }
        Product p = new Product();
        p.setName(req.name().trim());
        p.setPrice(req.price());
        p.setUnit(req.unit().trim());
        p.setExpiry(req.expiry());
        p.setCategory(category);
        products.save(p);
        return stock(storeId, p, req.quantity());
    }

    /** Take a product off this store's shelf. Past orders keep their line items. */
    @Transactional
    public void removeFromStore(Long adminId, Long storeId, Long productId) {
        requireManagedStore(adminId, storeId);
        Inventory.Key key = new Inventory.Key(storeId, productId);
        if (!inventory.existsById(key)) {
            throw ApiException.notFound("This product isn't stocked at this store");
        }
        inventory.deleteById(key);
    }

    private InventoryItemDto stock(Long storeId, Product p, int quantity) {
        Inventory inv = new Inventory();
        inv.setId(new Inventory.Key(storeId, p.getId()));
        inv.setProduct(p);
        inv.setQuantity(quantity);
        em.persist(inv);
        stores.addCategory(storeId, p.getCategory().getId());   // keep dark_store_category in sync
        em.flush();
        em.refresh(inv);
        return toDto(inv);
    }

    private Category resolveCategory(NewProductRequest req) {
        if (req.categoryId() != null) {
            return categories.findById(req.categoryId())
                    .orElseThrow(() -> ApiException.notFound("Category not found"));
        }
        if (req.categoryName() == null || req.categoryName().isBlank()) {
            throw ApiException.badRequest("Choose a category or type a new one");
        }
        String name = req.categoryName().trim();
        return categories.findByNameIgnoreCase(name).orElseGet(() -> {
            Category c = new Category();
            c.setName(name);
            return categories.save(c);
        });
    }

    @Transactional(readOnly = true)
    public List<OrderDto> storeOrders(Long adminId, Long storeId) {
        requireManagedStore(adminId, storeId);
        return orders.findByDarkStoreIdOrderByIdDesc(storeId).stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PartnerSummaryDto> storePartners(Long adminId, Long storeId) {
        requireManagedStore(adminId, storeId);
        return partners.findByDarkStoreIdOrderByFirstName(storeId).stream()
                .map(p -> new PartnerSummaryDto(p.getId(), p.fullName(), p.getPhone(), p.getVehicleNumber(), p.getStatus()))
                .toList();
    }

    /**
     * PLACED -> PACKED. The partner was normally auto-assigned when the order was placed;
     * passing partnerId reassigns it to another AVAILABLE partner of the same store.
     */
    @Transactional
    public OrderDto pack(Long adminId, Long orderId, Long partnerId) {
        Order o = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        requireManagedStore(adminId, o.getDarkStore().getId());
        if (o.getStatus() != OrderStatus.PLACED) {
            throw ApiException.badRequest("Only a PLACED order can be packed (this one is " + o.getStatus() + ")");
        }
        DeliveryPartner current = o.getDeliveryPartner();
        if (partnerId != null && (current == null || !current.getId().equals(partnerId))) {
            DeliveryPartner p = partners.findById(partnerId)
                    .orElseThrow(() -> ApiException.notFound("Delivery partner not found"));
            if (!o.getDarkStore().getId().equals(p.getDarkStoreId())) {
                throw ApiException.badRequest(p.fullName() + " is not attached to this store");
            }
            if (p.getStatus() != PartnerStatus.AVAILABLE) {
                throw ApiException.conflict(p.fullName() + " is " + p.getStatus() + " right now");
            }
            dispatch.handOver(o, p);
            if (current != null) {
                dispatch.release(current);   // the previous partner is free again
            }
        } else if (current == null) {
            dispatch.assign(o);              // nobody was free at order time -- try again now
        }
        o.setStatus(OrderStatus.PACKED);
        return mapper.toDto(o);
    }

    private DarkStore requireManagedStore(Long adminId, Long storeId) {
        DarkStore s = stores.findById(storeId).orElseThrow(() -> ApiException.notFound("Store not found"));
        if (!adminId.equals(s.getManagerEmployeeId())) {
            throw ApiException.forbidden("You don't manage " + s.getName());
        }
        return s;
    }

    private static InventoryItemDto toDto(Inventory inv) {
        Product p = inv.getProduct();
        return new InventoryItemDto(p.getId(), p.getName(), p.getUnit(), p.getCategory().getName(),
                p.getPrice(), inv.getQuantity(), inv.getUpdatedAt());
    }
}
