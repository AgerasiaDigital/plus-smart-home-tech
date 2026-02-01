package ru.yandex.practicum.telemetry.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.telemetry.analyzer.entity.ScenarioAction;

import java.util.List;

public interface ScenarioActionRepository
        extends JpaRepository<ScenarioAction, ScenarioAction.ScenarioActionId> {

    List<ScenarioAction> findByScenarioId(Long scenarioId);

    @Query("DELETE FROM ScenarioAction sa WHERE sa.scenarioId = :scenarioId")
    void deleteByScenarioId(Long scenarioId);
}
