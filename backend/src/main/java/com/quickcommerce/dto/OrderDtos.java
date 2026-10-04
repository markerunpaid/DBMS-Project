package com.quickcommerce.dto;

import com.quickcommerce.entity.OrderStatus;
import com.quickcommerce.entity.PaymentMode;
import com.quickcommerce.entity.PaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderLine(@NotNull Long productId, @Min(1) @Max(50) int quantity) {
    }

    public record PlaceOrderRequest(
            @NotNull Long storeId,
            @NotNull Long addressId,
            @NotEmpty List<@Valid OrderLine> items,
            String couponCode,
            @NotNull PaymentMode paymentMode) {
    }

    /** Row of the order_timer table. */
    public record TimerDto(
            LocalDateTime placedAt,
            LocalDateTime expectedDeliveryAt,
            LocalDateTime outForDeliveryAt,
            LocalDateTime receivedAt,
            LocalDateTime cancelledAt) {
    }

    public record OrderItemDto(
            Long productId, String name, String unit,
            int quantity, BigDecimal priceAtOrder, BigDecimal lineTotal) {
    }

    /** One shape for every role; each role only ever receives orders it is allowed to see. */
    public record OrderDto(
            Long id,
            OrderStatus status,
            BigDecimal amount,
            Long storeId,
            String storeName,
            String storeAddress,
            String customerName,
            String customerPhone,
            String deliveryAddress,
            Long partnerId,
            String partnerName,
            String partnerPhone,
            String couponCode,
            PaymentMode paymentMode,
            PaymentStatus paymentStatus,
            TimerDto timer,
            List<OrderItemDto> items) {
    }

    public record ReceiptDto(
            Long orderId,
            OrderStatus status,
            LocalDateTime placedAt,
            String storeName,
            String storeAddress,
            String customerName,
            String customerEmail,
            String customerPhone,
            String deliveryAddress,
            String partnerName,
            String partnerPhone,
            TimerDto timer,
            List<OrderItemDto> items,
            BigDecimal subtotal,
            String couponCode,
            BigDecimal discount,
            BigDecimal total,
            PaymentMode paymentMode,
            PaymentStatus paymentStatus,
            LocalDateTime paymentTime) {
    }
}
