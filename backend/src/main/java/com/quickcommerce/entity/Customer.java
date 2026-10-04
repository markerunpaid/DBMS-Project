package com.quickcommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customer")
@Getter
@Setter
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long id;

    private String firstName;
    private String middleName;
    private String lastName;
    private String phone;
    private String email;
    private String passwordHash;

    public String fullName() {
        return Names.full(firstName, middleName, lastName);
    }
}
