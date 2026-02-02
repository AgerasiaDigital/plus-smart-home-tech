package ru.yandex.practicum.telemetry.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.telemetry.analyzer.processor.HubEventProcessor;
import ru.yandex.practicum.telemetry.analyzer.processor.SnapshotProcessor;

@SpringBootApplication
@ConfigurationPropertiesScan
@Slf4j
public class AnalyzerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AnalyzerApplication.class, args);

        final HubEventProcessor hubEventProcessor = context.getBean(HubEventProcessor.class);
        SnapshotProcessor snapshotProcessor = context.getBean(SnapshotProcessor.class);

        // Регистрируем хук для корректного завершения приложения
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down analyzer application...");
            context.close();
        }));

        // Запускаем в отдельном потоке обработчик событий от пользовательских хабов
        Thread hubEventsThread = new Thread(hubEventProcessor);
        hubEventsThread.setName("HubEventHandlerThread");
        hubEventsThread.start();

        // В текущем потоке начинаем обработку снимков состояния датчиков
        snapshotProcessor.start();
    }
}