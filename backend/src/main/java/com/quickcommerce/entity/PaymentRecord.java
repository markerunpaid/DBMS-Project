package com.quickcommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_record")
@Getter
@Setter
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMode mode;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "timestamp")
    private LocalDateTime paidAt;
}
