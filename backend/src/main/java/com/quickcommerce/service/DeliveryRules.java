package com.quickcommerce.service;

import com.quickcommerce.entity.Address;
import com.quickcommerce.entity.OperatingHours;
import com.quickcommerce.repository.OperatingHoursRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

/** Store opening hours, delivery radius and expected-delivery-time rules. */
@Component
public class DeliveryRules {

    /** Stores farther than this from the delivery address can't take the order. */
    public static final double DELIVERY_RADIUS_KM = 10.0;

    private static final int BASE_MINUTES = 10;       // picking + packing
    private static final double MINUTES_PER_KM = 2.0; // riding

    private final OperatingHoursRepository hours;

    public DeliveryRules(OperatingHoursRepository hours) {
        this.hours = hours;
    }

    /** Value written to order_timer.expected_delivery_at, relative to placed_at. */
    public static int etaMinutes(double distanceKm) {
        return BASE_MINUTES + (int) Math.ceil(MINUTES_PER_KM * distanceKm);
    }

    /** operating_hours.day_of_week code, e.g. MON. */
    public static String dayCode(LocalDateTime t) {
        return t.getDayOfWeek().name().substring(0, 3);
    }

    public boolean isOpen(Long storeId, LocalDateTime t) {
        LocalTime time = t.toLocalTime();
        return hours.findById(new OperatingHours.Key(storeId, dayCode(t)))
                .map(h -> !time.isBefore(h.getOpensAt()) && !time.isAfter(h.getClosesAt()))
                .orElse(false);
    }

    /** Great-circle (Haversine) distance, same formula as DarkStoreRepository.findNearest. */
    public static double distanceKm(Address a, Address b) {
        double lat1 = Math.toRadians(a.getLatitude().doubleValue());
        double lat2 = Math.toRadians(b.getLatitude().doubleValue());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(b.getLongitude().doubleValue() - a.getLongitude().doubleValue());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLng / 2), 2);
        return 6371 * 2 * Math.asin(Math.sqrt(h));
    }
}
