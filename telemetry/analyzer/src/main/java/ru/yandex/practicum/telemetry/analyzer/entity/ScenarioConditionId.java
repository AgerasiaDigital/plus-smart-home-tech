package ru.yandex.practicum.telemetry.analyzer.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class ScenarioConditionId implements Serializable {
    private Long scenario;
    private String sensor;
    private Long condition;
}