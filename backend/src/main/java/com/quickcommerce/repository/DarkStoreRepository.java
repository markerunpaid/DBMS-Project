package com.quickcommerce.repository;

import com.quickcommerce.entity.DarkStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DarkStoreRepository extends JpaRepository<DarkStore, Long> {

    List<DarkStore> findByManagerEmployeeIdOrderByName(Long managerEmployeeId);

    boolean existsByManagerEmployeeId(Long managerEmployeeId);

    /** Record that the store now stocks this category (dark_store_category M:N); no-op if it already does. */
    @Modifying
    @Query(value = "INSERT IGNORE INTO dark_store_category (dark_store_id, category_id) VALUES (:storeId, :categoryId)",
            nativeQuery = true)
    void addCategory(@Param("storeId") Long storeId, @Param("categoryId") Long categoryId);

    interface NearestStoreRow {
        Long getId();

        Number getDistanceKm();

        Number getOpenNow();
    }

    /**
     * Every dark store sorted by great-circle (Haversine) distance from the given point,
     * plus whether operating_hours says it is open on {@code day} at {@code time}.
     */
    @Query(value = """
            SELECT ds.dark_store_id AS id,
                   6371 * 2 * ASIN(SQRT(
                       POWER(SIN(RADIANS(a.latitude - :lat) / 2), 2)
                     + COS(RADIANS(:lat)) * COS(RADIANS(a.latitude))
                     * POWER(SIN(RADIANS(a.longitude - :lng) / 2), 2)
                   )) AS distanceKm,
                   EXISTS (SELECT 1
                             FROM operating_hours oh
                            WHERE oh.dark_store_id = ds.dark_store_id
                              AND oh.day_of_week   = :day
                              AND CAST(:time AS TIME) BETWEEN oh.opens_at AND oh.closes_at) AS openNow
              FROM dark_store ds
              JOIN address a ON a.address_id = ds.address_id
             ORDER BY distanceKm
            """, nativeQuery = true)
    List<NearestStoreRow> findNearest(@Param("lat") double lat,
                                      @Param("lng") double lng,
                                      @Param("day") String day,
                                      @Param("time") String time);
}
