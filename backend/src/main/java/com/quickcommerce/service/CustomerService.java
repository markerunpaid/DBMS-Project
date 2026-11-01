package com.quickcommerce.service;

import com.quickcommerce.dto.CustomerDtos.AddressDto;
import com.quickcommerce.dto.CustomerDtos.CatalogProductDto;
import com.quickcommerce.dto.CustomerDtos.CouponDto;
import com.quickcommerce.dto.CustomerDtos.NearestStoreDto;
import com.quickcommerce.dto.CustomerDtos.NewAddressRequest;
import com.quickcommerce.dto.CustomerDtos.StoreItemDto;
import com.quickcommerce.dto.CustomerDtos.StoreStockDto;
import com.quickcommerce.entity.Address;
import com.quickcommerce.entity.CustomerAddress;
import com.quickcommerce.entity.DarkStore;
import com.quickcommerce.entity.Inventory;
import com.quickcommerce.entity.Pincode;
import com.quickcommerce.entity.Product;
import com.quickcommerce.exception.ApiException;
import com.quickcommerce.repository.AddressRepository;
import com.quickcommerce.repository.CustomerAddressRepository;
import com.quickcommerce.repository.CustomerCouponRepository;
import com.quickcommerce.repository.DarkStoreRepository;
import com.quickcommerce.repository.InventoryRepository;
import com.quickcommerce.repository.PincodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AddressRepository addresses;
    private final CustomerAddressRepository customerAddresses;
    private final PincodeRepository pincodes;
    private final DarkStoreRepository stores;
    private final InventoryRepository inventory;
    private final CustomerCouponRepository customerCoupons;

    public CustomerService(AddressRepository addresses, CustomerAddressRepository customerAddresses,
                           PincodeRepository pincodes, DarkStoreRepository stores,
                           InventoryRepository inventory, CustomerCouponRepository customerCoupons) {
        this.addresses = addresses;
        this.customerAddresses = customerAddresses;
        this.pincodes = pincodes;
        this.stores = stores;
        this.inventory = inventory;
        this.customerCoupons = customerCoupons;
    }

    // ---------- addresses ----------

    @Transactional(readOnly = true)
    public List<AddressDto> addresses(Long customerId) {
        return customerAddresses.findByIdCustomerId(customerId).stream()
                .sorted(Comparator.comparing(CustomerAddress::isDefaultAddress).reversed()
                        .thenComparing(ca -> ca.getId().getAddressId()))
                .map(CustomerService::toDto)
                .toList();
    }

    @Transactional
    public AddressDto addAddress(Long customerId, NewAddressRequest r) {
        Pincode pin = pincodes.findById(r.pinCode()).orElseGet(() -> {
            if (isBlank(r.city()) || isBlank(r.state())) {
                throw ApiException.badRequest("New pin code " + r.pinCode() + ": please enter its city and state");
            }
            Pincode p = new Pincode();
            p.setPinCode(r.pinCode());
            p.setCity(r.city().trim());
            p.setState(r.state().trim());
            return pincodes.save(p);
        });

        Address a = new Address();
        a.setHouseNo(trimToNull(r.houseNo()));
        a.setStreet(trimToNull(r.street()));
        a.setPincode(pin);
        a.setLatitude(r.latitude());
        a.setLongitude(r.longitude());
        addresses.save(a);

        List<CustomerAddress> existing = customerAddresses.findByIdCustomerId(customerId);
        boolean makeDefault = r.makeDefault() || existing.isEmpty();
        if (makeDefault) {
            existing.forEach(ca -> ca.setDefaultAddress(false));
        }
        CustomerAddress ca = new CustomerAddress();
        ca.setId(new CustomerAddress.Key(customerId, a.getId()));
        ca.setAddress(a);
        ca.setLabel(trimToNull(r.label()));
        ca.setDefaultAddress(makeDefault);
        customerAddresses.save(ca);
        return toDto(ca);
    }

    /** The customer's saved address, or 404 -- never someone else's. */
    public CustomerAddress requireOwnAddress(Long customerId, Long addressId) {
        return customerAddresses.findById(new CustomerAddress.Key(customerId, addressId))
                .orElseThrow(() -> ApiException.notFound("Address not found in your saved addresses"));
    }

    // ---------- stores & items ----------

    @Transactional(readOnly = true)
    public List<NearestStoreDto> nearestStores(Long customerId, Long addressId) {
        Address from = requireOwnAddress(customerId, addressId).getAddress();
        LocalDateTime now = LocalDateTime.now();
        var rows = stores.findNearest(from.getLatitude().doubleValue(), from.getLongitude().doubleValue(),
                DeliveryRules.dayCode(now), now.format(TIME));
        Map<Long, DarkStore> byId = stores.findAllById(rows.stream().map(DarkStoreRepository.NearestStoreRow::getId).toList())
                .stream().collect(Collectors.toMap(DarkStore::getId, Function.identity()));
        return rows.stream().map(row -> {
            DarkStore s = byId.get(row.getId());
            double km = Math.round(row.getDistanceKm().doubleValue() * 100) / 100.0;
            return new NearestStoreDto(
                    s.getId(), s.getName(), s.getAddress().format(), km,
                    row.getOpenNow().intValue() == 1,
                    km <= DeliveryRules.DELIVERY_RADIUS_KM,
                    DeliveryRules.etaMinutes(km));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<NearestStoreDto> getDeliverableStores(Long customerId, Long addressId) {
        return nearestStores(customerId, addressId).stream()
                .filter(s -> s.openNow() && s.deliverable())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoreItemDto> storeItems(Long storeId) {
        if (!stores.existsById(storeId)) {
            throw ApiException.notFound("Store not found");
        }
        return inventory.findByIdDarkStoreIdAndQuantityGreaterThanOrderByProductCategoryNameAscProductNameAsc(storeId, 0)
                .stream()
                .map(inv -> {
                    var p = inv.getProduct();
                    return new StoreItemDto(p.getId(), p.getName(), p.getUnit(), p.getPrice(), p.getExpiry(),
                            p.getCategory().getId(), p.getCategory().getName(), inv.getQuantity());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogProductDto> unifiedCatalog(Long customerId, Long addressId) {
        List<NearestStoreDto> openStores = getDeliverableStores(customerId, addressId);
        if (openStores.isEmpty()) {
            return List.of();
        }
        Map<Long, NearestStoreDto> storeMap = openStores.stream()
                .collect(Collectors.toMap(NearestStoreDto::id, Function.identity()));
        List<Long> storeIds = openStores.stream().map(NearestStoreDto::id).toList();

        List<Inventory> rows = inventory.findAvailableInStores(storeIds);

        Map<Product, List<Inventory>> byProduct = rows.stream()
                .collect(Collectors.groupingBy(Inventory::getProduct, LinkedHashMap::new, Collectors.toList()));

        List<CatalogProductDto> result = new ArrayList<>();
        for (var entry : byProduct.entrySet()) {
            Product p = entry.getKey();
            List<Inventory> invs = entry.getValue();

            int totalAvailable = 0;
            List<StoreStockDto> stockList = new ArrayList<>();
            for (Inventory inv : invs) {
                Long sId = inv.getId().getDarkStoreId();
                NearestStoreDto sInfo = storeMap.get(sId);
                if (sInfo != null && inv.getQuantity() > 0) {
                    totalAvailable += inv.getQuantity();
                    stockList.add(new StoreStockDto(
                            sId, sInfo.name(), inv.getQuantity(), sInfo.distanceKm(), sInfo.etaMinutes()));
                }
            }

            if (totalAvailable > 0) {
                result.add(new CatalogProductDto(
                        p.getId(), p.getName(), p.getUnit(), p.getPrice(), p.getExpiry(),
                        p.getCategory().getId(), p.getCategory().getName(), totalAvailable, stockList));
            }
        }
        return result;
    }

    // ---------- coupons ----------

    /** Coupons assigned to this customer that are unused and inside their validity window. */
    @Transactional(readOnly = true)
    public List<CouponDto> usableCoupons(Long customerId) {
        LocalDateTime now = LocalDateTime.now();
        return customerCoupons.findByIdCustomerId(customerId).stream()
                .filter(cc -> cc.getRedeemedAt() == null && cc.getCoupon().isValidAt(now))
                .map(cc -> {
                    var c = cc.getCoupon();
                    return new CouponDto(c.getCode(), c.getDiscountType(), c.getValue(), c.getValidTo());
                })
                .toList();
    }

    private static AddressDto toDto(CustomerAddress ca) {
        Address a = ca.getAddress();
        Pincode p = a.getPincode();
        return new AddressDto(a.getId(), ca.getLabel(), ca.isDefaultAddress(), a.getHouseNo(), a.getStreet(),
                p.getPinCode(), p.getCity(), p.getState(), a.getLatitude(), a.getLongitude(), a.format());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }
}
