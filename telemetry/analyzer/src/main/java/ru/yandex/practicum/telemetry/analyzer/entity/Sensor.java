package ru.yandex.practicum.telemetry.analyzer.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sensors")
@Data
public class Sensor {
    @Id
    private String id;

    @Column(name = "hub_id", nullable = false)
    private String hubId;
}