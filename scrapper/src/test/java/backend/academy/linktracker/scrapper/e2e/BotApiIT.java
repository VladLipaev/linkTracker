package backend.academy.linktracker.scrapper.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Disabled
public class BotApiIT extends IntegrationEnvironment {

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    public void sendUpdateToBot_validUpdate_shouldReturn200() {
        String validBody = """
                {
                  "id": 1,
                  "url": "https://github.com/user/repo",
                  "description": "Новый коммит",
                  "tgChatIds": [123, 456]
                }
                """;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> httpEntity = new HttpEntity<>(validBody, httpHeaders);
        ResponseEntity<Void> response = restTemplate.postForEntity(getBotUrl() + "/updates", httpEntity, Void.class);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    public void sendUpdateToBot_invalidUpdate_shouldReturn400() {
        String invalidBody = """
                {
                  "description": "Здесь нет обязательных полей"
                }
                """;

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpClientErrorException ex = assertThrows(
                HttpClientErrorException.class,
                () -> restTemplate.postForEntity(
                        getBotUrl() + "/updates", new HttpEntity<>(invalidBody, httpHeaders), String.class));

        assertEquals(400, ex.getStatusCode().value());
    }
}
