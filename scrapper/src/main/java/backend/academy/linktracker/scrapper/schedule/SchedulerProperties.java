package backend.academy.linktracker.scrapper.schedule;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.scheduler", ignoreUnknownFields = false)
public record SchedulerProperties(boolean enable, Duration interval, int batchSize, int threadCount) {}
