package backend.academy.linktracker.scrapper.controller;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.dto.ErrorCode;
import backend.academy.linktracker.scrapper.repository.orm.JpaTgChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TgChatRestControllerIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JpaTgChatRepository tgChatRepository;

    @BeforeEach
    void setUp() {
        tgChatRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /tg-chat/{id} — Успешная регистрация чата")
    void registerChat_Success() {
        Long chatId = 100L;

        ResponseEntity<Void> response = restTemplate.postForEntity("/tg-chat/{id}", null, Void.class, chatId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tgChatRepository.existsById(chatId)).isTrue();
    }

    @Test
    @DisplayName("POST /tg-chat/{id} — 409 CONFLICT при повторной регистрации")
    void registerChat_AlreadyExists() {
        Long chatId = 100L;
        restTemplate.postForEntity("/tg-chat/{id}", null, Void.class, chatId);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
            "/tg-chat/{id}", null, ApiErrorResponse.class, chatId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo(ErrorCode.CHAT_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("DELETE /tg-chat/{id} — Успешное удаление чата")
    void deleteChat_Success() {
        Long chatId = 100L;
        restTemplate.postForEntity("/tg-chat/{id}", null, Void.class, chatId);

        ResponseEntity<Void> response = restTemplate.exchange(
            "/tg-chat/{id}", HttpMethod.DELETE, null, Void.class, chatId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tgChatRepository.existsById(chatId)).isFalse();
    }

    @Test
    @DisplayName("DELETE /tg-chat/{id} — 404 NOT FOUND если чат не существует")
    void deleteChat_NotFound() {
        Long chatId = 999L;

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
            "/tg-chat/{id}", HttpMethod.DELETE, null, ApiErrorResponse.class, chatId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo(ErrorCode.CHAT_NOT_FOUND);
    }
}
