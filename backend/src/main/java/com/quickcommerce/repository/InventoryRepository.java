package com.quickcommerce.repository;

import com.quickcommerce.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Inventory.Key> {

    List<Inventory> findByIdDarkStoreIdOrderByProductCategoryNameAscProductNameAsc(Long darkStoreId);

    List<Inventory> findByIdDarkStoreIdAndQuantityGreaterThanOrderByProductCategoryNameAscProductNameAsc(
            Long darkStoreId, int quantity);

    /**
     * Takes stock only if enough is left, in a single statement, so two concurrent
     * orders can never oversell. Returns 0 when stock is insufficient (or not stocked).
     */
    @Modifying
    @Query(value = """
            UPDATE inventory
               SET quantity = quantity - :qty
             WHERE dark_store_id = :storeId
               AND product_id    = :productId
               AND quantity     >= :qty
            """, nativeQuery = true)
    int decrement(@Param("storeId") Long storeId, @Param("productId") Long productId, @Param("qty") int qty);

    @Modifying
    @Query(value = """
            UPDATE inventory
               SET quantity = quantity + :qty
             WHERE dark_store_id = :storeId
               AND product_id    = :productId
            """, nativeQuery = true)
    int increment(@Param("storeId") Long storeId, @Param("productId") Long productId, @Param("qty") int qty);
}
