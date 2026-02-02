package ru.yandex.practicum.telemetry.analyzer.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "scenario_conditions")
@IdClass(ScenarioConditionId.class)
@Data
@EqualsAndHashCode(exclude = {"scenario", "sensor", "condition"})
@ToString(exclude = {"scenario", "sensor", "condition"})
public class ScenarioCondition {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "condition_id")
    private Condition condition;
}