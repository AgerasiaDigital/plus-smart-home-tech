package ru.yandex.practicum.telemetry.analyzer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "scenario_conditions")
@IdClass(ScenarioCondition.ScenarioConditionId.class)
@Getter
@Setter
public class ScenarioCondition {
    @Id
    @Column(name = "scenario_id")
    private Long scenarioId;

    @Id
    @Column(name = "sensor_id")
    private String sensorId;

    @Id
    @Column(name = "condition_id")
    private Long conditionId;

    @ManyToOne
    @JoinColumn(name = "scenario_id", insertable = false, updatable = false)
    private Scenario scenario;

    @ManyToOne
    @JoinColumn(name = "sensor_id", insertable = false, updatable = false)
    private Sensor sensor;

    @ManyToOne
    @JoinColumn(name = "condition_id", insertable = false, updatable = false)
    private Condition condition;

    @Getter
    @Setter
    public static class ScenarioConditionId implements Serializable {
        private Long scenarioId;
        private String sensorId;
        private Long conditionId;
    }
}