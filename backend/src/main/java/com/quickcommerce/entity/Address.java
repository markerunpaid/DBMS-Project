package com.quickcommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "address")
@Getter
@Setter
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long id;

    private String houseNo;
    private String street;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pin_code")
    private Pincode pincode;

    private BigDecimal latitude;
    private BigDecimal longitude;

    /** e.g. "Room 214, IIT BHU Hostel Road, Varanasi, Uttar Pradesh - 221005" */
    public String format() {
        StringBuilder sb = new StringBuilder();
        if (houseNo != null && !houseNo.isBlank()) sb.append(houseNo).append(", ");
        if (street != null && !street.isBlank()) sb.append(street).append(", ");
        sb.append(pincode.getCity()).append(", ").append(pincode.getState())
          .append(" - ").append(pincode.getPinCode());
        return sb.toString();
    }
}
