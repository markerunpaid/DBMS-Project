package com.quickcommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "product")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    private String name;
    private BigDecimal price;
    private LocalDate expiry;
    private String unit;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;
}
