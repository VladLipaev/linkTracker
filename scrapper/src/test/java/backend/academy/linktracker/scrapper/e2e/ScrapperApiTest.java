package backend.academy.linktracker.scrapper.e2e;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class ScrapperApiTest extends IntegrationEnvironment {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void registerChatAddLinkGetLink_validRequest_shouldAddAndGetLink() throws Exception {

        long chatId = 1L;
        String linkUrl = "https://github.com/test/test";

        // 1. Регистрируем чат
        ResponseEntity<Void> registerResponse =
                restTemplate.postForEntity(getScrapperUrl() + "/tg-chat/" + chatId, null, Void.class);

        assertEquals(200, registerResponse.getStatusCode().value());

        // 2. Добавляем ссылку
        String body = String.format("{\"link\":\"%s\"}", linkUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Tg-Chat-Id", String.valueOf(chatId));

        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> addLinkResponse =
                restTemplate.postForEntity(getScrapperUrl() + "/links", requestEntity, String.class);

        assertEquals(200, addLinkResponse.getStatusCode().value());

        // 3. Получаем ссылки
        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.set("Tg-Chat-Id", String.valueOf(chatId));

        HttpEntity<Void> getRequest = new HttpEntity<>(getHeaders);

        ResponseEntity<String> getLinksResponse =
                restTemplate.exchange(getScrapperUrl() + "/links", HttpMethod.GET, getRequest, String.class);

        assertEquals(200, getLinksResponse.getStatusCode().value());

        // Проверяем JSON
        JsonNode jsonNode = objectMapper.readTree(getLinksResponse.getBody());

        boolean linkFound = false;

        for (JsonNode linkNode : jsonNode.get("links")) {
            if (linkNode.get("url").asText().equals(linkUrl)) {
                linkFound = true;
                break;
            }
        }

        assertTrue(linkFound, "Добавленная ссылка должна присутствовать в ответе");
    }

    @Test
    public void registerChatAddLinkDeleteLinkGetLink_validRequest_shouldAddAndDeleteLink() throws Exception {
        long chatId = 20L;
        String linkUrl = "https://github.com/test/to-delete";

        // 1. Регистрируем чат
        restTemplate.postForEntity(getScrapperUrl() + "/tg-chat/" + chatId, null, Void.class);

        // 2. Добавляем ссылку
        String body = String.format("{\"link\":\"%s\"}", linkUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Tg-Chat-Id", String.valueOf(chatId));

        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(getScrapperUrl() + "/links", requestEntity, String.class);

        // 3. Удаляем ссылку (RestTemplate.exchange нужен, так как DELETE с телом)
        ResponseEntity<String> deleteResponse =
                restTemplate.exchange(getScrapperUrl() + "/links", HttpMethod.DELETE, requestEntity, String.class);
        assertEquals(200, deleteResponse.getStatusCode().value());

        // 4. Получаем ссылки и проверяем, что удалённой там нет
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> getLinksResponse =
                restTemplate.exchange(getScrapperUrl() + "/links", HttpMethod.GET, getRequest, String.class);

        JsonNode jsonNode = objectMapper.readTree(getLinksResponse.getBody());
        boolean linkFound = false;

        if (jsonNode.has("links") && jsonNode.get("links").isArray()) {
            for (JsonNode linkNode : jsonNode.get("links")) {
                if (linkNode.get("url").asText().equals(linkUrl)) {
                    linkFound = true;
                    break;
                }
            }
        }

        assertFalse(linkFound, "Удалённая ссылка не должна присутствовать в ответе");
    }

    // 3.3 Попытка удаления ссылки из несуществующего чата
    @Test
    public void registerChatAddLinkDeleteLinkGetLink_invalidChat_shouldFailToDeleteLinkFromNonExistentChat()
            throws Exception {
        long validChatId = 30L;
        long invalidChatId = 999L; // Несуществующий
        String linkUrl = "https://github.com/test/invalid-delete";

        // 1. Регистрируем чат и добавляем ссылку
        restTemplate.postForEntity(getScrapperUrl() + "/tg-chat/" + validChatId, null, Void.class);

        HttpHeaders validHeaders = new HttpHeaders();
        validHeaders.setContentType(MediaType.APPLICATION_JSON);
        validHeaders.set("Tg-Chat-Id", String.valueOf(validChatId));
        String body = String.format("{\"link\":\"%s\"}", linkUrl);

        restTemplate.postForEntity(getScrapperUrl() + "/links", new HttpEntity<>(body, validHeaders), String.class);

        // 2. Пытаемся удалить ссылку с чужим chatId
        HttpHeaders invalidHeaders = new HttpHeaders();
        invalidHeaders.setContentType(MediaType.APPLICATION_JSON);
        invalidHeaders.set("Tg-Chat-Id", String.valueOf(invalidChatId));
        HttpEntity<String> invalidRequest = new HttpEntity<>(body, invalidHeaders);

        // Ожидаем ошибку 404
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            restTemplate.exchange(getScrapperUrl() + "/links", HttpMethod.DELETE, invalidRequest, String.class);
        });
        assertEquals(404, exception.getStatusCode().value());

        // 3. Проверяем, что ссылка всё ещё на месте у правильного чата
        ResponseEntity<String> getLinksResponse = restTemplate.exchange(
                getScrapperUrl() + "/links", HttpMethod.GET, new HttpEntity<>(validHeaders), String.class);

        JsonNode jsonNode = objectMapper.readTree(getLinksResponse.getBody());
        boolean linkFound = false;
        for (JsonNode linkNode : jsonNode.get("links")) {
            if (linkNode.get("url").asText().equals(linkUrl)) {
                linkFound = true;
                break;
            }
        }
        assertTrue(linkFound, "Ссылка должна остаться в системе");
    }

    // 3.4 Добавление ссылки в несуществующий чат
    @Test
    public void registerChatAddLink_invalidChat_shouldFailToAddLinkToNonExistentChat() {
        long validChatId = 40L;
        long invalidChatId = 4040L; // Чат, который не регистрировали
        String linkUrl = "https://github.com/test/invalid-add";

        // 1. Регистрируем нормальный чат
        restTemplate.postForEntity(getScrapperUrl() + "/tg-chat/" + validChatId, null, Void.class);

        // 2. Пытаемся добавить ссылку в несуществующий
        HttpHeaders invalidHeaders = new HttpHeaders();
        invalidHeaders.setContentType(MediaType.APPLICATION_JSON);
        invalidHeaders.set("Tg-Chat-Id", String.valueOf(invalidChatId));

        String body = String.format("{\"link\":\"%s\"}", linkUrl);
        HttpEntity<String> requestEntity = new HttpEntity<>(body, invalidHeaders);

        // Ожидаем ошибку 404
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            restTemplate.postForEntity(getScrapperUrl() + "/links", requestEntity, String.class);
        });

        assertEquals(404, exception.getStatusCode().value());
    }

    // 3.5 Работа с удалённым чатом
    @Test
    public void registerChatDeleteChatAddLink_invalidChat_shouldFailToAddLinkToDeletedChat() {
        long chatId = 50L;
        String linkUrl = "https://github.com/test/deleted-chat";

        // 1. Регистрируем чат
        restTemplate.postForEntity(getScrapperUrl() + "/tg-chat/" + chatId, null, Void.class);

        // 2. Удаляем чат
        ResponseEntity<Void> deleteChatResponse =
                restTemplate.exchange(getScrapperUrl() + "/tg-chat/" + chatId, HttpMethod.DELETE, null, Void.class);
        assertEquals(200, deleteChatResponse.getStatusCode().value());

        // 3. Пытаемся добавить в него ссылку
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Tg-Chat-Id", String.valueOf(chatId));

        String body = String.format("{\"link\":\"%s\"}", linkUrl);
        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

        // Ожидаем ошибку 404
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            restTemplate.postForEntity(getScrapperUrl() + "/links", requestEntity, String.class);
        });

        assertEquals(404, exception.getStatusCode().value());
    }

    // 3.6 Удаление несуществующего чата
    @Test
    public void deleteChat_invalidChat_shouldReturn404WhenDeletingNonExistentChat() {
        long invalidChatId = 6060L;

        // Ожидаем ошибку 404
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            restTemplate.exchange(getScrapperUrl() + "/tg-chat/" + invalidChatId, HttpMethod.DELETE, null, Void.class);
        });

        assertEquals(404, exception.getStatusCode().value());
    }
}
