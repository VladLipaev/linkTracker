package backend.academy.linktracker.scrapper.handler;

import java.time.OffsetDateTime;

public record UpdateResult(boolean hasUpdate, OffsetDateTime newUpdatedAt, String description) {}
