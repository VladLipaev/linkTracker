package backend.academy.linktracker.ai.client;

import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record YandexGPTRequestBody(
        String modelUri, Map<String, Object> completionOptions, List<Map<String, Object>> messages) {}
