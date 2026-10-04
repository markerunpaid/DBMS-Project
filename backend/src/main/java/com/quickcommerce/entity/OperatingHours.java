package com.quickcommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalTime;

@Entity
@Table(name = "operating_hours")
@Getter
@Setter
public class OperatingHours {

    @EmbeddedId
    private Key id;

    private LocalTime opensAt;
    private LocalTime closesAt;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private Long darkStoreId;
        private String dayOfWeek;   // 'MON' .. 'SUN'
    }
}
