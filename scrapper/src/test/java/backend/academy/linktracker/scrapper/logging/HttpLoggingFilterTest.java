package backend.academy.linktracker.scrapper.logging;

import backend.academy.linktracker.scrapper.config.logging.HttpLoggingFilter;
import backend.academy.linktracker.scrapper.properties.HttpMaskingProperties;
import backend.academy.linktracker.scrapper.service.MaskingService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpLoggingFilterTest {

    @Mock
    private MaskingService maskingService;

    private HttpMaskingProperties httpMaskingProperties;
    private HttpLoggingFilter loggingFilter;

    @BeforeEach
    void setUp() {
        httpMaskingProperties = new HttpMaskingProperties();
        loggingFilter = new HttpLoggingFilter(maskingService, httpMaskingProperties);
    }

    @Test
    @DisplayName("Должен успешного пропустить запрос по цепочке и вызвать маскирование query-строки")
    void shouldProcessFilterAndMaskQueryParams() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/links");
        request.setQueryString("url=https://github.com&token=secret123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        MockFilterChain filterChain = new MockFilterChain();

        when(maskingService.maskQuery(eq(request.getQueryString()), eq(httpMaskingProperties)))
            .thenReturn("url=https://github.com&token=***");

        loggingFilter.doFilter(request, response, filterChain);

        // Проверяем, что маскирование вызвалось с нужными свойствами
        verify(maskingService).maskQuery("url=https://github.com&token=secret123", httpMaskingProperties);

        // Проверяем, что статус ответа корректно прочитан
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Должен корректно выполнить логирование в блоке finally даже при возникновении ошибки дальше в FilterChain")
    void shouldLogResponseEvenWhenExceptionOccurs() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/links");
        request.setQueryString("key=val");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain filterChain = org.mockito.Mockito.mock(MockFilterChain.class);
        doThrow(new RuntimeException("Database error"))
            .when(filterChain).doFilter(any(), any());

        when(maskingService.maskQuery("key=val", httpMaskingProperties))
            .thenReturn("key=val");

        // Проверяем, что исключение пробрасывается наверх
        assertThrows(RuntimeException.class, () -> loggingFilter.doFilter(request, response, filterChain));

        // Проверяем, что логирование запроса и ответа в тике finally всё равно отработало
        verify(maskingService).maskQuery("key=val", httpMaskingProperties);
    }
}
