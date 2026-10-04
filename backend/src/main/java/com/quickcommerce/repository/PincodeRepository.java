package com.quickcommerce.repository;

import com.quickcommerce.entity.Pincode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PincodeRepository extends JpaRepository<Pincode, String> {
}
