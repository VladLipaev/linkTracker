package backend.academy.linktracker.scrapper.config.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class ScrapperMetrics {

    private final MeterRegistry registry;

    public ScrapperMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void incrementApiRequest(String source) {
        registry.counter("api_requests_total", "source", source).increment();
    }

    public void recordRequestDuration(long durationMs, String scope, String scopeType) {
        DistributionSummary.builder("request_duration_ms")
                .tags("scope", scope, "scope_type", scopeType)
                .publishPercentiles(0.5, 0.95, 0.99)
                .sla(10, 50, 100, 200, 500, 1000, 2000, 5000)
                .register(registry)
                .record(durationMs);
    }

    private final ConcurrentHashMap<String, AtomicInteger> linksCountBySource = new ConcurrentHashMap<>();

    public void initLinkCount(String source, int count) {
        AtomicInteger counter = linksCountBySource.computeIfAbsent(source, k -> new AtomicInteger(count));
        Gauge.builder("links_on_track_total", counter::get)
                .tags("tracked_source", source)
                .register(registry);
    }

    public void incrementLinks(String source) {
        AtomicInteger counter = linksCountBySource.get(source);
        if (counter != null) counter.incrementAndGet();
    }

    public void decrementLinks(String source) {
        AtomicInteger counter = linksCountBySource.get(source);
        if (counter != null) counter.decrementAndGet();
    }
}
