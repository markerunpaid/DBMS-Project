package com.quickcommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Weak entity: one timer per order, sharing the order's primary key. */
@Entity
@Table(name = "order_timer")
@Getter
@Setter
public class OrderTimer {

    @Id
    @Column(name = "order_id")
    private Long orderId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "order_id")
    private Order order;

    private LocalDateTime placedAt;
    private LocalDateTime expectedDeliveryAt;
    private LocalDateTime outForDeliveryAt;
    private LocalDateTime receivedAt;
    private LocalDateTime cancelledAt;
}
