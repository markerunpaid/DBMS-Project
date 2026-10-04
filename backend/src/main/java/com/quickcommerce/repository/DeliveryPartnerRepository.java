package com.quickcommerce.repository;

import com.quickcommerce.entity.DeliveryPartner;
import com.quickcommerce.entity.PartnerStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, Long> {

    Optional<DeliveryPartner> findByEmailIgnoreCase(String email);

    List<DeliveryPartner> findByDarkStoreIdOrderByFirstName(Long darkStoreId);

    /**
     * First free partner of a store, row-locked (SELECT ... FOR UPDATE) so two orders
     * placed at the same moment can't both grab the same partner.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeliveryPartner> findFirstByDarkStoreIdAndStatusOrderByIdAsc(Long darkStoreId, PartnerStatus status);
}
