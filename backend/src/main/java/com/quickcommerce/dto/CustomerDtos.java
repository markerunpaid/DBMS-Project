package com.quickcommerce.dto;

import com.quickcommerce.entity.DiscountType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
}
