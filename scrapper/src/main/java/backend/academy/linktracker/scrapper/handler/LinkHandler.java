package backend.academy.linktracker.scrapper.handler;

import java.time.OffsetDateTime;

public interface LinkHandler {

    boolean supports(String url);

    OffsetDateTime fetchUpdate(String url);
}
