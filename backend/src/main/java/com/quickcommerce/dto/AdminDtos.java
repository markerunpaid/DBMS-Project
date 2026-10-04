package com.quickcommerce.dto;

import com.quickcommerce.entity.PartnerStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record HoursDto(String day, LocalTime opensAt, LocalTime closesAt) {
    }

    public record AdminStoreDto(
            Long id, String name, String address, boolean openNow,
            List<HoursDto> hours,
            int productCount, int outOfStockCount, long activeOrders) {
    }

    public record InventoryItemDto(
            Long productId, String name, String unit, String categoryName,
            BigDecimal price, int quantity, LocalDateTime updatedAt) {
    }

    public record UpdateStockRequest(@Min(0) @Max(100000) int quantity) {
    }

    public record PartnerSummaryDto(Long id, String name, String phone, String vehicleNumber, PartnerStatus status) {
    }

    /** partnerId is optional: omit it to keep the auto-assigned partner. */
    public record PackRequest(Long partnerId) {
    }

    public record CategoryDto(Long id, String name) {
    }

    public record ProductDto(Long id, String name, String unit, BigDecimal price, LocalDate expiry,
                             Long categoryId, String categoryName) {
    }

    /** Put an existing catalog product on this store's shelf. */
    public record AddStockRequest(@NotNull Long productId, @Min(0) @Max(100000) int quantity) {
    }

    /**
     * Create a brand-new catalog product and stock it at this store. Give either an
     * existing categoryId or a new categoryName.
     */
    public record NewProductRequest(
            @NotBlank @Size(max = 150) String name,
            @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal price,
            @NotBlank @Size(max = 30) String unit,
            LocalDate expiry,
            Long categoryId,
            @Size(max = 80) String categoryName,
            @Min(0) @Max(100000) int quantity) {
    }
}
