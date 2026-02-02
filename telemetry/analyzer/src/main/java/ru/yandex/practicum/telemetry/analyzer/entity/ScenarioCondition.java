package ru.yandex.practicum.telemetry.analyzer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "scenario_conditions")
@Getter
@Setter
@IdClass(ScenarioCondition.ScenarioConditionId.class)
public class ScenarioCondition {
    @Id
    @ManyToOne
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Id
    @ManyToOne
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @Id
    @ManyToOne
    @JoinColumn(name = "condition_id", nullable = false)
    private Condition condition;

    @Getter
    @Setter
    public static class ScenarioConditionId implements Serializable {
        private Long scenario;
        private String sensor;
        private Long condition;
    }
}