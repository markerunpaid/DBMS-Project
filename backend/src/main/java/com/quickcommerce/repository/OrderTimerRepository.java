package com.quickcommerce.repository;

import com.quickcommerce.entity.OrderTimer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderTimerRepository extends JpaRepository<OrderTimer, Long> {
}
