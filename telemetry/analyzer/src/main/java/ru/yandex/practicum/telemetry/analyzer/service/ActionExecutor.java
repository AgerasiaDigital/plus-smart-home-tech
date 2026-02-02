package ru.yandex.practicum.telemetry.analyzer.service;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.hubrouter.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc.HubRouterControllerBlockingStub;
import ru.yandex.practicum.telemetry.analyzer.entity.Action;
import ru.yandex.practicum.telemetry.analyzer.entity.ScenarioAction;

import java.time.Instant;

@Service
@Slf4j
public class ActionExecutor {

    @GrpcClient("hub-router")
    private HubRouterControllerBlockingStub hubRouterClient;

    public void executeAction(String hubId, String scenarioName, ScenarioAction scenarioAction) {
        Action action = scenarioAction.getAction();
        String sensorId = scenarioAction.getSensor().getId();

        log.info("Preparing to execute action: hubId={}, scenario={}, sensor={}, actionType={}",
                hubId, scenarioName, sensorId, action.getType());

        try {
            DeviceActionProto.Builder actionBuilder = DeviceActionProto.newBuilder()
                    .setSensorId(sensorId)
                    .setType(ActionTypeProto.valueOf(action.getType().name()));

            if (action.getValue() != null) {
                actionBuilder.setValue(action.getValue());
                log.debug("Action has value: {}", action.getValue());
            }

            Instant now = Instant.now();
            Timestamp timestamp = Timestamp.newBuilder()
                    .setSeconds(now.getEpochSecond())
                    .setNanos(now.getNano())
                    .build();

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenarioName)
                    .setAction(actionBuilder.build())
                    .setTimestamp(timestamp)
                    .build();

            log.info("Sending gRPC request to Hub Router: {}", request);
            hubRouterClient.handleDeviceAction(request);
            log.info("Action executed successfully: hubId={}, scenario={}, sensor={}, action={}",
                    hubId, scenarioName, sensorId, action.getType());

        } catch (Exception e) {
            log.error("Error executing action for sensor {} in scenario {}: {}",
                    sensorId, scenarioName, e.getMessage(), e);
        }
    }
}