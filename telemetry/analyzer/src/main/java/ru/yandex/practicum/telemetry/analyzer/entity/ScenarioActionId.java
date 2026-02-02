package ru.yandex.practicum.telemetry.analyzer.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class ScenarioActionId implements Serializable {
    private Long scenario;
    private String sensor;
    private Long action;
}