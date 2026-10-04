package com.quickcommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pincode")
@Getter
@Setter
public class Pincode {

    @Id
    @Column(name = "pin_code", length = 6)
    private String pinCode;

    private String city;
    private String state;
}
