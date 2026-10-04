package com.quickcommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_coupon")
@Getter
@Setter
public class CustomerCoupon {

    @EmbeddedId
    private Key id;

    @ManyToOne(optional = false)
    @MapsId("couponId")
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    private LocalDateTime redeemedAt;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private Long customerId;
        private Long couponId;
    }
}
