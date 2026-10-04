package com.quickcommerce.repository;

import com.quickcommerce.entity.OperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OperatingHoursRepository extends JpaRepository<OperatingHours, OperatingHours.Key> {

    List<OperatingHours> findByIdDarkStoreId(Long darkStoreId);
}
