package com.quickcommerce.service;

import com.quickcommerce.dto.OrderDtos.OrderDto;
import com.quickcommerce.dto.OrderDtos.OrderItemDto;
import com.quickcommerce.dto.OrderDtos.ReceiptDto;
import com.quickcommerce.dto.OrderDtos.TimerDto;
import com.quickcommerce.entity.Order;
import com.quickcommerce.entity.OrderProduct;
import com.quickcommerce.entity.OrderTimer;
import com.quickcommerce.entity.PaymentRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Entity -> response shape. Call inside a transaction (touches lazy order items). */
@Component
public class OrderMapper {

    public OrderDto toDto(Order o) {
        var partner = o.getDeliveryPartner();
        PaymentRecord pay = o.getPayment();
        return new OrderDto(
                o.getId(),
                o.getStatus(),
                o.getAmount(),
                o.getDarkStore().getId(),
                o.getDarkStore().getName(),
                o.getDarkStore().getAddress().format(),
                o.getCustomer().fullName(),
                o.getCustomer().getPhone(),
                o.getDeliveryAddress().format(),
                partner == null ? null : partner.getId(),
                partner == null ? null : partner.fullName(),
                partner == null ? null : partner.getPhone(),
                o.getCoupon() == null ? null : o.getCoupon().getCode(),
                pay == null ? null : pay.getMode(),
                pay == null ? null : pay.getStatus(),
                timer(o.getTimer()),
                items(o));
    }

    public ReceiptDto toReceipt(Order o) {
        List<OrderItemDto> items = items(o);
        BigDecimal subtotal = items.stream().map(OrderItemDto::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        PaymentRecord pay = o.getPayment();
        var partner = o.getDeliveryPartner();
        return new ReceiptDto(
                o.getId(),
                o.getStatus(),
                o.getTimer() == null ? null : o.getTimer().getPlacedAt(),
                o.getDarkStore().getName(),
                o.getDarkStore().getAddress().format(),
                o.getCustomer().fullName(),
                o.getCustomer().getEmail(),
                o.getCustomer().getPhone(),
                o.getDeliveryAddress().format(),
                partner == null ? null : partner.fullName(),
                partner == null ? null : partner.getPhone(),
                timer(o.getTimer()),
                items,
                subtotal,
                o.getCoupon() == null ? null : o.getCoupon().getCode(),
                subtotal.subtract(o.getAmount()),
                o.getAmount(),
                pay == null ? null : pay.getMode(),
                pay == null ? null : pay.getStatus(),
                pay == null ? null : pay.getPaidAt());
    }

    private static List<OrderItemDto> items(Order o) {
        return o.getItems().stream()
                .sorted(Comparator.comparing((OrderProduct op) -> op.getProduct().getName()))
                .map(op -> new OrderItemDto(
                        op.getProduct().getId(),
                        op.getProduct().getName(),
                        op.getProduct().getUnit(),
                        op.getQuantity(),
                        op.getPriceAtOrder(),
                        op.getPriceAtOrder().multiply(BigDecimal.valueOf(op.getQuantity()))))
                .toList();
    }

    private static TimerDto timer(OrderTimer t) {
        if (t == null) return null;
        return new TimerDto(t.getPlacedAt(), t.getExpectedDeliveryAt(), t.getOutForDeliveryAt(),
                t.getReceivedAt(), t.getCancelledAt());
    }
}
