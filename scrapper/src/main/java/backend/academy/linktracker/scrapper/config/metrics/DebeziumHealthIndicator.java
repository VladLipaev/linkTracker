package backend.academy.linktracker.scrapper.config.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DebeziumHealthIndicator implements HealthIndicator {

    private final DebeziumMetrics debeziumMetrics;
    @Override
    public Health health() {
        return debeziumMetrics.isConnectorRunning()
            ? Health.up().build()
            : Health.down().build();
    }
}
