package backend.academy.linktracker.scrapper.config.metrics;
import backend.academy.linktracker.scrapper.repository.outbox.OutBoxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
@Slf4j
public class DebeziumMetrics {

    private final MeterRegistry registry;
    private final OutBoxRepository outboxRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${KAFKA_CONNECT_URL:http://connect:8083}")
    private String connectUrl;

    @Value("${app.debezium.connector-name:outbox-connector}")
    private String connectorName;

    private final AtomicInteger connectorStatus = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        Gauge.builder("outbox.events.pending",
                outboxRepository,
                repo -> repo.countByStatus("new"))
            .description("Pending outbox events")
            .register(registry);

        Gauge.builder("debezium.connector.status",
                connectorStatus,
                AtomicInteger::get)
            .description("1=RUNNING, 0=FAILED")
            .register(registry);
    }

    @Scheduled(fixedDelay = 10000)
    @SchedulerLock(name = "DebeziumMetrics_refreshConnectorStatus",
        lockAtMostFor = "${app.schedulerLock.lockAtMostFor.DebeziumMetrics_refreshConnectorStatus:10s}",
        lockAtLeastFor = "${app.schedulerLock.lockAtLeastFor.DebeziumMetrics_refreshConnectorStatus:5s}")
    public void refreshConnectorStatus() {
        try {
            String url = connectUrl + "/connectors/" + connectorName + "/status";
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response != null) {
                String state = response.path("connector").path("state").asText();
                connectorStatus.set("RUNNING".equals(state) ? 1 : 0);
            } else {
                connectorStatus.set(0);
            }
        } catch (Exception e) {
            connectorStatus.set(0);
            log.warn("Cannot get Debezium status", e);
        }
    }

    public void incrementOutboxCreated() {
        registry.counter("outbox.events.created.total").increment();
    }

    public boolean isConnectorRunning() {
        return connectorStatus.get() == 1;
    }
}
