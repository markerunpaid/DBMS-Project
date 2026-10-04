package com.quickcommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee")
@Getter
@Setter
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long id;

    private String firstName;
    private String middleName;
    private String lastName;
    private String phone;
    private String email;
    private String passwordHash;

    @Column(name = "dark_store_id")
    private Long darkStoreId;

    public String fullName() {
        return Names.full(firstName, middleName, lastName);
    }
}
