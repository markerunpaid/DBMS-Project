package com.quickcommerce.repository;

import com.quickcommerce.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, CustomerAddress.Key> {

    List<CustomerAddress> findByIdCustomerId(Long customerId);
}
