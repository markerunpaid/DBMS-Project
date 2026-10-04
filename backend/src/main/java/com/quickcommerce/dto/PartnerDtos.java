package com.quickcommerce.dto;

import com.quickcommerce.entity.PartnerStatus;
import jakarta.validation.constraints.NotNull;

public final class PartnerDtos {

    private PartnerDtos() {
    }

    public record PartnerProfileDto(
            Long id, String name, String email, String phone, String vehicleNumber,
            PartnerStatus status, Long storeId, String storeName, String storeAddress,
            long activeDeliveries, long completedDeliveries) {
    }

    public record StatusRequest(@NotNull PartnerStatus status) {
    }
}
