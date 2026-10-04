package com.quickcommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "customer_address")
@Getter
@Setter
public class CustomerAddress {

    @EmbeddedId
    private Key id;

    @ManyToOne(optional = false)
    @MapsId("addressId")
    @JoinColumn(name = "address_id")
    private Address address;

    private String label;

    @Column(name = "is_default")
    private boolean defaultAddress;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private Long customerId;
        private Long addressId;
    }
}
