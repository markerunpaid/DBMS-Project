package com.quickcommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "delivery_partner")
@Getter
@Setter
public class DeliveryPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partner_id")
    private Long id;

    private String firstName;
    private String middleName;
    private String lastName;
    private String phone;
    private String email;
    private String passwordHash;
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    private PartnerStatus status;

    @Column(name = "dark_store_id")
    private Long darkStoreId;

    public String fullName() {
        return Names.full(firstName, middleName, lastName);
    }
}
