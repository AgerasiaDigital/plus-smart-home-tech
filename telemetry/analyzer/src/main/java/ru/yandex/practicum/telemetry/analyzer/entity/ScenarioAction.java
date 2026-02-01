package ru.yandex.practicum.telemetry.analyzer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "scenario_actions")
@IdClass(ScenarioAction.ScenarioActionId.class)
@Getter
@Setter
public class ScenarioAction {
    @Id
    @Column(name = "scenario_id")
    private Long scenarioId;

    @Id
    @Column(name = "sensor_id")
    private String sensorId;

    @Id
    @Column(name = "action_id")
    private Long actionId;

    @ManyToOne
    @JoinColumn(name = "scenario_id", insertable = false, updatable = false)
    private Scenario scenario;

    @ManyToOne
    @JoinColumn(name = "sensor_id", insertable = false, updatable = false)
    private Sensor sensor;

    @ManyToOne
    @JoinColumn(name = "action_id", insertable = false, updatable = false)
    private Action action;

    @Getter
    @Setter
    public static class ScenarioActionId implements Serializable {
        private Long scenarioId;
        private String sensorId;
        private Long actionId;
    }
}