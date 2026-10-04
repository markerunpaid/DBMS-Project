package com.quickcommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dark_store")
@Getter
@Setter
public class DarkStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dark_store_id")
    private Long id;

    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "address_id")
    private Address address;

    @Column(name = "manager_employee_id")
    private Long managerEmployeeId;
}
