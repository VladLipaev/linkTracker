package backend.academy.linktracker.scrapper.service.mapper;

import org.springframework.stereotype.Component;
import java.net.URI;

@Component
public class UriConverter {
    public URI toUri(String url) {
        return url != null ? URI.create(url) : null;
    }
}
