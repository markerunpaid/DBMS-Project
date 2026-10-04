package com.quickcommerce.repository;

import com.quickcommerce.entity.Order;
import com.quickcommerce.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByIdDesc(Long customerId);

    List<Order> findByDarkStoreIdOrderByIdDesc(Long darkStoreId);

    List<Order> findByDeliveryPartnerIdOrderByIdDesc(Long partnerId);

    long countByDarkStoreIdAndStatusIn(Long darkStoreId, Collection<OrderStatus> statuses);

    long countByDeliveryPartnerIdAndStatusIn(Long partnerId, Collection<OrderStatus> statuses);

    long countByDeliveryPartnerIdAndStatus(Long partnerId, OrderStatus status);

    /** Oldest order of a store still waiting for a delivery partner. */
    Optional<Order> findFirstByDarkStoreIdAndDeliveryPartnerIsNullAndStatusInOrderByIdAsc(
            Long darkStoreId, Collection<OrderStatus> statuses);
}
