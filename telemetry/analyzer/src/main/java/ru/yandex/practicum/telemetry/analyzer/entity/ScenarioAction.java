package ru.yandex.practicum.telemetry.analyzer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "scenario_actions")
@Getter
@Setter
@IdClass(ScenarioAction.ScenarioActionId.class)
public class ScenarioAction {
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
    @JoinColumn(name = "action_id", nullable = false)
    private Action action;

    @Getter
    @Setter
    public static class ScenarioActionId implements Serializable {
        private Long scenario;
        private String sensor;
        private Long action;
    }
}