package com.quickcommerce.service;

import com.quickcommerce.dto.OrderDtos.OrderDto;
import com.quickcommerce.dto.PartnerDtos.PartnerProfileDto;
import com.quickcommerce.entity.DeliveryPartner;
import com.quickcommerce.entity.Order;
import com.quickcommerce.entity.OrderStatus;
import com.quickcommerce.entity.PartnerStatus;
import com.quickcommerce.entity.PaymentMode;
import com.quickcommerce.entity.PaymentRecord;
import com.quickcommerce.entity.PaymentStatus;
import com.quickcommerce.exception.ApiException;
import com.quickcommerce.repository.DarkStoreRepository;
import com.quickcommerce.repository.DeliveryPartnerRepository;
import com.quickcommerce.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Delivery partner: profile, availability and the pickup -> deliver steps of an order. */
@Service
public class PartnerService {

    private final DeliveryPartnerRepository partners;
    private final OrderRepository orders;
    private final DarkStoreRepository stores;
    private final OrderMapper mapper;
    private final DispatchService dispatch;

    public PartnerService(DeliveryPartnerRepository partners, OrderRepository orders,
                          DarkStoreRepository stores, OrderMapper mapper, DispatchService dispatch) {
        this.partners = partners;
        this.orders = orders;
        this.stores = stores;
        this.mapper = mapper;
        this.dispatch = dispatch;
    }

    @Transactional(readOnly = true)
    public PartnerProfileDto profile(Long partnerId) {
        DeliveryPartner p = requirePartner(partnerId);
        var store = p.getDarkStoreId() == null ? null : stores.findById(p.getDarkStoreId()).orElse(null);
        return new PartnerProfileDto(
                p.getId(), p.fullName(), p.getEmail(), p.getPhone(), p.getVehicleNumber(), p.getStatus(),
                store == null ? null : store.getId(),
                store == null ? null : store.getName(),
                store == null ? null : store.getAddress().format(),
                orders.countByDeliveryPartnerIdAndStatusIn(partnerId, DispatchService.ACTIVE),
                orders.countByDeliveryPartnerIdAndStatus(partnerId, OrderStatus.DELIVERED));
    }

    /** Partners switch themselves between AVAILABLE and OFFLINE; BUSY is set by the system. */
    @Transactional
    public PartnerProfileDto setStatus(Long partnerId, PartnerStatus status) {
        if (status == PartnerStatus.BUSY) {
            throw ApiException.badRequest("BUSY is set automatically when an order is assigned to you");
        }
        DeliveryPartner p = requirePartner(partnerId);
        if (orders.countByDeliveryPartnerIdAndStatusIn(partnerId, DispatchService.ACTIVE) > 0) {
            throw ApiException.conflict("Finish your active deliveries first");
        }
        p.setStatus(status);
        dispatch.offerWaitingOrder(p);   // coming online picks up an order that was waiting for a partner
        return profile(partnerId);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> deliveries(Long partnerId) {
        return orders.findByDeliveryPartnerIdOrderByIdDesc(partnerId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public OrderDto pickUp(Long partnerId, Long orderId) {
        Order o = requireAssigned(partnerId, orderId);
        if (o.getStatus() == OrderStatus.PLACED) {
            throw ApiException.badRequest("The store is still packing this order -- wait until it is PACKED");
        }
        if (o.getStatus() != OrderStatus.PACKED) {
            throw ApiException.badRequest("Only a PACKED order can be picked up (this one is " + o.getStatus() + ")");
        }
        o.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        o.getTimer().setOutForDeliveryAt(LocalDateTime.now().withNano(0));
        return mapper.toDto(o);
    }

    @Transactional
    public OrderDto deliver(Long partnerId, Long orderId) {
        Order o = requireAssigned(partnerId, orderId);
        if (o.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw ApiException.badRequest("Pick the order up before marking it delivered");
        }
        LocalDateTime now = LocalDateTime.now().withNano(0);
        o.setStatus(OrderStatus.DELIVERED);
        o.getTimer().setReceivedAt(now);
        PaymentRecord pay = o.getPayment();
        if (pay != null && pay.getMode() == PaymentMode.CASH && pay.getStatus() == PaymentStatus.PENDING) {
            pay.setStatus(PaymentStatus.SUCCESS);   // cash collected at the door
            pay.setPaidAt(now);
        }
        dispatch.release(o.getDeliveryPartner());   // free again -> may take the next waiting order
        return mapper.toDto(o);
    }

    private DeliveryPartner requirePartner(Long partnerId) {
        return partners.findById(partnerId).orElseThrow(() -> ApiException.notFound("Partner not found"));
    }

    private Order requireAssigned(Long partnerId, Long orderId) {
        Order o = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (o.getDeliveryPartner() == null || !o.getDeliveryPartner().getId().equals(partnerId)) {
            throw ApiException.forbidden("This order is not assigned to you");
        }
        return o;
    }
}
