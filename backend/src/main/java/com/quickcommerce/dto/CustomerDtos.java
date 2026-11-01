package com.quickcommerce.dto;

import com.quickcommerce.dto.OrderDtos.OrderLine;
import com.quickcommerce.entity.DiscountType;
import com.quickcommerce.entity.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record AddressDto(
            Long addressId, String label, boolean isDefault,
            String houseNo, String street, String pinCode, String city, String state,
            BigDecimal latitude, BigDecimal longitude, String formatted) {
    }

    /** city/state are only needed when the pin code isn't in the pincode table yet. */
    public record NewAddressRequest(
            @Size(max = 30) String label,
            @Size(max = 30) String houseNo,
            @Size(max = 150) String street,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "must be 6 digits") String pinCode,
            @Size(max = 80) String city,
            @Size(max = 80) String state,
            @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            boolean makeDefault) {
    }

    public record NearestStoreDto(
            Long id, String name, String address,
            double distanceKm, boolean openNow, boolean deliverable, int etaMinutes) {
    }

    public record StoreItemDto(
            Long productId, String name, String unit, BigDecimal price, LocalDate expiry,
            Long categoryId, String categoryName, int available) {
    }

    public record CouponDto(String code, DiscountType discountType, BigDecimal value, LocalDateTime validTo) {
    }

    public record StoreStockDto(
            Long storeId, String storeName, int available, double distanceKm, int etaMinutes) {
    }

    public record CatalogProductDto(
            Long productId, String name, String unit, BigDecimal price, LocalDate expiry,
            Long categoryId, String categoryName, int totalAvailable, List<StoreStockDto> stores) {
    }

    public record OrderItemPreview(
            Long productId, String name, String unit, int quantity, BigDecimal price, BigDecimal lineTotal) {
    }

    public record FulfillmentPlanPackage(
            Long storeId, String storeName, String storeAddress, double distanceKm, int etaMinutes,
            List<OrderItemPreview> items, BigDecimal subtotal) {
    }

    public record FulfillmentPlanDto(
            List<FulfillmentPlanPackage> packages, BigDecimal subtotal, int totalItems, boolean isSplit) {
    }

    public record MultiStorePlaceOrderRequest(
            @NotNull Long addressId,
            @NotEmpty List<@Valid OrderLine> items,
            String couponCode,
            @NotNull PaymentMode paymentMode) {
    }

    public record PlanOrderRequest(
            @NotNull Long addressId,
            @NotEmpty List<@Valid OrderLine> items) {
    }
}
