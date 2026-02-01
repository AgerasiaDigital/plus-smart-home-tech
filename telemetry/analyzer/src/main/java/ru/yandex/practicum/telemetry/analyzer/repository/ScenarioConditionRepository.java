package ru.yandex.practicum.telemetry.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.telemetry.analyzer.entity.ScenarioCondition;

import java.util.List;

public interface ScenarioConditionRepository
        extends JpaRepository<ScenarioCondition, ScenarioCondition.ScenarioConditionId> {

    List<ScenarioCondition> findByScenarioId(Long scenarioId);

    @Query("DELETE FROM ScenarioCondition sc WHERE sc.scenarioId = :scenarioId")
    void deleteByScenarioId(Long scenarioId);
}