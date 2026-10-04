package com.quickcommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Getter
@Setter
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;

    private String code;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private BigDecimal value;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    public boolean isValidAt(LocalDateTime t) {
        return !t.isBefore(validFrom) && t.isBefore(validTo);
    }
}
