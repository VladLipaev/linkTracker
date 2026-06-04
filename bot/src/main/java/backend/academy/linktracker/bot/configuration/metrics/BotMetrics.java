package backend.academy.linktracker.bot.configuration.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BotMetrics {
    private final MeterRegistry registry;

    public BotMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void incrementCommand(String command) {
        registry.counter("command_requests_total", "command", command).increment();
    }

    public void recordCommandDuration(long durationMs, String scope, String scopeType) {
        DistributionSummary.builder("command_duration_ms")
                .tags("scope", scope, "scope_type", scopeType)
                .publishPercentiles(0.5, 0.95, 0.99)
                .sla(10, 50, 100, 200, 500, 1000, 2000, 5000)
                .register(registry)
                .record(durationMs);
    }

    public void incrementSentNotification() {
        registry.counter("sent_notification_total").increment();
    }
}
