package backend.academy.linktracker.scrapper.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.config.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.dto.ErrorCode;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.handler.LinkValidator;
import backend.academy.linktracker.scrapper.repository.orm.JpaLinksRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaSubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaTgChatRepository;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


class LinksRestControllerIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JpaTgChatRepository tgChatRepository;

    @Autowired
    private JpaLinksRepository linksRepository;

    @Autowired
    private JpaSubscriptionRepository subscriptionRepository;

    @MockitoBean
    private LinkValidator linkValidator;

    @MockitoBean
    private ScrapperMetrics metrics;

    private final Long chatId = 777L;
    private final String validUrl = "https://github.com/user/repo";

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        linksRepository.deleteAll();
        tgChatRepository.deleteAll();

        // По умолчанию считаем, что валидатор возвращает true
        Mockito.when(linkValidator.isValid(anyString())).thenReturn(true);
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Tg-Chat-Id", String.valueOf(chatId));
        return headers;
    }

    @Test
    @DisplayName("POST /links — Успешное добавление ссылки с тегами")
    void addLink_Success() {
        // Создаем чат
        restTemplate.postForEntity("/tg-chat/{id}", null, Void.class, chatId);

        AddLinkRequest request = AddLinkRequest.builder().build();
        request.setLink(URI.create(validUrl));
        request.setTags(List.of("java", "github"));

        HttpEntity<AddLinkRequest> entity = new HttpEntity<>(request, createHeaders());

        ResponseEntity<LinkResponse> response = restTemplate.postForEntity("/links", entity, LinkResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUrl()).isEqualTo(URI.create(validUrl));
        assertThat(response.getBody().getTags()).containsExactlyInAnyOrder("java", "github");
    }

    @Test
    @DisplayName("POST /links — 404 NOT FOUND если чат не зарегистрирован")
    void addLink_ChatNotFound() {
        AddLinkRequest request = AddLinkRequest.builder().build();
        request.setLink(URI.create(validUrl));

        HttpEntity<AddLinkRequest> entity = new HttpEntity<>(request, createHeaders());

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity("/links", entity, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo(ErrorCode.CHAT_NOT_FOUND);
    }

    @Test
    @DisplayName("POST /links — 400 BAD REQUEST для неподдерживаемой ссылки")
    void addLink_UnsupportedLink() {
        restTemplate.postForEntity("/tg-chat/{id}", null, Void.class, chatId);

        Mockito.when(linkValidator.isValid(anyString())).thenReturn(false);

        AddLinkRequest request = AddLinkRequest.builder().build();
        request.setLink(URI.create("https://unsupported-domain.com"));

        HttpEntity<AddLinkRequest> entity = new HttpEntity<>(request, createHeaders());

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity("/links", entity, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("GET /links — Получение всех ссылок пользователя")
    void getAllLinks_Success() {
        restTemplate.postForEntity("/tg-chat/{id}", null, Void.class, chatId);

        AddLinkRequest addRequest = AddLinkRequest.builder().build();
        addRequest.setLink(URI.create(validUrl));
        addRequest.setTags(List.of("tag1"));
        restTemplate.postForEntity("/links", new HttpEntity<>(addRequest, createHeaders()), LinkResponse.class);

        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<ListLinksResponse> response = restTemplate.exchange(
            "/links", HttpMethod.GET, entity, ListLinksResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSize()).isEqualTo(1);
        assertThat(response.getBody().getLinks().get(0).getUrl()).isEqualTo(URI.create(validUrl));
    }

    @Test
    @DisplayName("DELETE /links — Успешное удаление ссылки")
    void removeLink_Success() {
        restTemplate.postForEntity("/tg-chat/{id}", null, Void.class, chatId);

        AddLinkRequest addRequest = AddLinkRequest.builder().build();
        addRequest.setLink(URI.create(validUrl));
        addRequest.setTags(List.of("tag1"));
        restTemplate.postForEntity("/links", new HttpEntity<>(addRequest, createHeaders()), LinkResponse.class);

        RemoveLinkRequest removeRequest = RemoveLinkRequest.builder().build();
        removeRequest.setLink(URI.create(validUrl));

        HttpEntity<RemoveLinkRequest> entity = new HttpEntity<>(removeRequest, createHeaders());

        ResponseEntity<LinkResponse> response = restTemplate.exchange(
            "/links", HttpMethod.DELETE, entity, LinkResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUrl()).isEqualTo(URI.create(validUrl));
    }
}
