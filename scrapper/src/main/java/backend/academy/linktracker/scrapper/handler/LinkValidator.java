package backend.academy.linktracker.scrapper.handler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkValidator {
    private final List<LinkHandler> handlers;

    public boolean isValid(String url) {
        return handlers.stream().anyMatch(h -> h.supports(url));
    }
}
