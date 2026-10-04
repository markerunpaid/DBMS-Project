package com.quickcommerce.service;

import com.quickcommerce.entity.DeliveryPartner;
import com.quickcommerce.entity.Order;
import com.quickcommerce.entity.OrderStatus;
import com.quickcommerce.entity.PartnerStatus;
import com.quickcommerce.repository.DeliveryPartnerRepository;
import com.quickcommerce.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

/**
 * Automatic delivery-partner assignment. Every call runs inside the caller's transaction.
 *
 * - An order is handed to a free (AVAILABLE) partner of its store the moment it is placed.
 * - If every partner is busy, the order waits; the next partner who becomes free takes
 *   the oldest waiting order of their store.
 * - A partner holds one order at a time: AVAILABLE -> BUSY on assignment, back to AVAILABLE
 *   once delivered or cancelled.
 */
@Service
public class DispatchService {

    /** Orders a partner is still responsible for. */
    static final EnumSet<OrderStatus> ACTIVE =
            EnumSet.of(OrderStatus.PLACED, OrderStatus.PACKED, OrderStatus.OUT_FOR_DELIVERY);

    /** Orders that can still take a partner. */
    private static final EnumSet<OrderStatus> WAITING = EnumSet.of(OrderStatus.PLACED, OrderStatus.PACKED);

    private final DeliveryPartnerRepository partners;
    private final OrderRepository orders;

    public DispatchService(DeliveryPartnerRepository partners, OrderRepository orders) {
        this.partners = partners;
        this.orders = orders;
    }

    /** Give the order to the store's first free partner, if there is one. */
    public void assign(Order order) {
        if (order.getDeliveryPartner() != null) return;
        partners.findFirstByDarkStoreIdAndStatusOrderByIdAsc(order.getDarkStore().getId(), PartnerStatus.AVAILABLE)
                .ifPresent(p -> handOver(order, p));
    }

    /** Partner finished (or lost) an order: free them if nothing is left, then offer the next waiting order. */
    public void release(DeliveryPartner p) {
        if (p.getStatus() == PartnerStatus.BUSY && orders.countByDeliveryPartnerIdAndStatusIn(p.getId(), ACTIVE) == 0) {
            p.setStatus(PartnerStatus.AVAILABLE);
        }
        offerWaitingOrder(p);
    }

    /** A free partner takes the oldest order of their store that nobody is delivering yet. */
    public void offerWaitingOrder(DeliveryPartner p) {
        if (p.getStatus() != PartnerStatus.AVAILABLE || p.getDarkStoreId() == null) return;
        orders.findFirstByDarkStoreIdAndDeliveryPartnerIsNullAndStatusInOrderByIdAsc(p.getDarkStoreId(), WAITING)
                .ifPresent(o -> handOver(o, p));
    }

    void handOver(Order order, DeliveryPartner p) {
        order.setDeliveryPartner(p);
        p.setStatus(PartnerStatus.BUSY);
    }
}
