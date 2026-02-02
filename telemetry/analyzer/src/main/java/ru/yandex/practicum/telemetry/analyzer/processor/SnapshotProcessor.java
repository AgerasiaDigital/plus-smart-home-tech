package ru.yandex.practicum.telemetry.analyzer.processor;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc.HubRouterControllerBlockingStub;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.analyzer.service.ScenarioAnalysisService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotProcessor {

    private final ScenarioAnalysisService scenarioAnalysisService;
    
    @GrpcClient("hub-router")
    private HubRouterControllerBlockingStub hubRouterClient;

    @Value("${app.kafka.consumer.snapshot.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.consumer.snapshot.group-id}")
    private String groupId;

    @Value("${app.kafka.consumer.snapshot.topic}")
    private String topic;

    @Value("${app.kafka.consumer.snapshot.key-deserializer}")
    private String keyDeserializer;

    @Value("${app.kafka.consumer.snapshot.value-deserializer}")
    private String valueDeserializer;

    @Value("${app.kafka.consumer.snapshot.auto-offset-reset}")
    private String autoOffsetReset;

    @Value("${app.kafka.consumer.snapshot.enable-auto-commit}")
    private boolean enableAutoCommit;

    @Value("${app.kafka.consumer.snapshot.auto-commit-interval-ms}")
    private int autoCommitIntervalMs;

    public void start() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", groupId);
        props.put("key.deserializer", keyDeserializer);
        props.put("value.deserializer", valueDeserializer);
        props.put("auto.offset.reset", autoOffsetReset);
        props.put("enable.auto.commit", enableAutoCommit);
        props.put("auto.commit.interval.ms", autoCommitIntervalMs);

        try (KafkaConsumer<String, SensorsSnapshotAvro> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            log.info("Started snapshot processor, subscribed to topic: {}", topic);

            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(1000));
                
                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    try {
                        processSnapshot(record.value());
                    } catch (Exception e) {
                        log.error("Error processing snapshot: {}", e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in snapshot processor: {}", e.getMessage(), e);
        }
    }

    private void processSnapshot(SensorsSnapshotAvro snapshot) {
        log.info("Processing snapshot for hub: {}", snapshot.getHubId());
        
        List<DeviceActionProto> actions = scenarioAnalysisService.analyzeSnapshot(snapshot);
        log.info("Found {} actions for hub: {}", actions.size(), snapshot.getHubId());
        
        for (DeviceActionProto action : actions) {
            try {
                log.info("Sending action {} for sensor {} in hub {}", action.getType(), action.getSensorId(), snapshot.getHubId());
                sendDeviceAction(snapshot.getHubId(), action);
            } catch (Exception e) {
                log.error("Error sending device action: {}", e.getMessage(), e);
            }
        }
        
        log.info("Processed {} actions for hub: {}", actions.size(), snapshot.getHubId());
    }

    private void sendDeviceAction(String hubId, DeviceActionProto action) {
        log.info("🚀 SENDING gRPC COMMAND: hub={}, sensor={}, action={}", 
                hubId, action.getSensorId(), action.getType());
                
        DeviceActionRequest request = 
            DeviceActionRequest.newBuilder()
                .setHubId(hubId)
                .setScenarioName("analyzer-scenario")
                .setAction(action)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano())
                        .build())
                .build();

        try {
            log.info("📡 Calling hubRouterClient.handleDeviceAction() to localhost:59091...");
            hubRouterClient.handleDeviceAction(request);
            log.info("✅ SUCCESS: gRPC command sent for sensor {} in hub {}", action.getSensorId(), hubId);
        } catch (Exception e) {
            log.error("❌ FAILED to send gRPC request: {}", e.getMessage(), e);
            throw e;
        }
    }
}