package com.quickcommerce.repository;

import com.quickcommerce.entity.CustomerCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerCouponRepository extends JpaRepository<CustomerCoupon, CustomerCoupon.Key> {

    List<CustomerCoupon> findByIdCustomerId(Long customerId);
}
