package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.client.YandexGPTRequestBody;
import backend.academy.linktracker.ai.client.YandexGPTRestClient;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class SummarizeService {

    @Value("${app.yandexgpt.folder-id}")
    private String folderId;

    private final YandexGPTRestClient yandexGPTRestClient;
    private final ObjectMapper objectMapper;

    public String summarize(String description) {
        YandexGPTRequestBody yandexGPTRequestBody = YandexGPTRequestBody.builder()
                .modelUri("gpt://" + folderId + "/yandexgpt/latest")
                .completionOptions(Map.of("stream", false, "temperature", 0.7, "maxTokens", "500"))
                .messages(List.of(Map.of(
                        "role", "user", "text", "Summarize the following update in 2–3 sentences: " + description)))
                .build();
        String response = yandexGPTRestClient.gptRequest(yandexGPTRequestBody);
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode textNode = rootNode.path("result")
                    .path("alternatives")
                    .get(0)
                    .path("message")
                    .path("text");

            if (textNode.isMissingNode()) {
                throw new RuntimeException("Failed to extract text from YandexGPT response");
            }

            return textNode.asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse YandexGPT response: " + response, e);
        }
    }
}
