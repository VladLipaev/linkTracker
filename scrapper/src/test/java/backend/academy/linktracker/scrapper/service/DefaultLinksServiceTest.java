package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.config.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.entity.Subscription;
import backend.academy.linktracker.scrapper.entity.SubscriptionId;
import backend.academy.linktracker.scrapper.handler.LinkValidator;
import backend.academy.linktracker.scrapper.repository.LinksRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

@ExtendWith(MockitoExtension.class)
class DefaultLinksServiceTest {

    @Mock
    private LinksRepository linksRepository;
    @Mock
    private TgChatRepository tgChatRepository;
    @Mock
    private LinkValidator linkValidator;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private ScrapperMetrics metrics;

    @InjectMocks
    private DefaultLinksService linksService;

    private final Long chatId = 100L;
    private final String url = "https://github.com/user/repo";

    @BeforeEach
    void setUp() {
        // Устанавливаем значение BATCH_SIZE, так как оно внедряется через @Value
        org.springframework.test.util.ReflectionTestUtils.setField(linksService, "BATCH_SIZE", 10);
    }

    // --- ADD LINK TESTS ---

    @Test
    @DisplayName("addLink — Успешное добавление новой ссылки")
    void addLink_Success() {
        Chat chat = new Chat(chatId);
        AddLinkRequest request = AddLinkRequest.builder().build();
        request.setLink(URI.create(url));
        request.setTags(List.of("java"));

        Link savedLink = new Link(url, OffsetDateTime.now());
        savedLink.setId(1L);

        when(tgChatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(linkValidator.isValid(url)).thenReturn(true);
        when(linksRepository.findByChatIdAndUrl(chatId, url)).thenReturn(Optional.empty());
        when(linksRepository.findByUrl(url)).thenReturn(Optional.empty());
        when(linksRepository.save(any(Link.class))).thenReturn(savedLink);

        LinkResponse response = linksService.addLink(chatId, request);

        assertThat(response).isNotNull();
        assertThat(response.getUrl()).isEqualTo(URI.create(url));
        assertThat(response.getTags()).containsExactly("java");

        verify(subscriptionRepository).save(any(Subscription.class));
        verify(metrics).incrementLinks("github.com");
    }

    @Test
    @DisplayName("addLink — Чат не найден")
    void addLink_ChatNotFound() {
        AddLinkRequest request = AddLinkRequest.builder().build();
        request.setLink(URI.create(url));

        when(tgChatRepository.findById(chatId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linksService.addLink(chatId, request))
            .isInstanceOf(ChatNotFoundException.class)
            .hasMessage("Чат не зарегистрирован");
    }

    @Test
    @DisplayName("addLink — Неподдерживаемая ссылка")
    void addLink_UnsupportedLink() {
        Chat chat = new Chat(chatId);
        AddLinkRequest request = AddLinkRequest.builder().build();
        request.setLink(URI.create(url));

        when(tgChatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(linkValidator.isValid(url)).thenReturn(false);

        assertThatThrownBy(() -> linksService.addLink(chatId, request))
            .isInstanceOf(UnsupportedLinkException.class)
            .hasMessage("Ссылка не поддерживается.");
    }

    @Test
    @DisplayName("addLink — Ссылка уже отслеживается этим чатом")
    void addLink_AlreadyExistsForChat() {
        Chat chat = new Chat(chatId);
        AddLinkRequest request = AddLinkRequest.builder().build();
        request.setLink(URI.create(url));

        when(tgChatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(linkValidator.isValid(url)).thenReturn(true);
        when(linksRepository.findByChatIdAndUrl(chatId, url)).thenReturn(Optional.of(new Link(url, OffsetDateTime.now())));

        assertThatThrownBy(() -> linksService.addLink(chatId, request))
            .isInstanceOf(LinkAlreadyExistsException.class);
    }

    // --- REMOVE LINK TESTS ---

    @Test
    @DisplayName("removeLink — Успешное удаление ссылки и отписки")
    void removeLink_Success() {
        RemoveLinkRequest request = RemoveLinkRequest.builder().build();
        request.setLink(URI.create(url));

        Link link = new Link(url, OffsetDateTime.now());
        link.setId(1L);

        Slice<String> tagsSlice = new PageImpl<>(List.of("tag1"));

        when(tgChatRepository.findById(chatId)).thenReturn(Optional.of(new Chat(chatId)));
        when(linkValidator.isValid(url)).thenReturn(true);
        when(linksRepository.findByChatIdAndUrl(chatId, url)).thenReturn(Optional.of(link));
        when(subscriptionRepository.findTagsByChatIdAndLinkId(eq(chatId), eq(1L), any(PageRequest.class)))
            .thenReturn(tagsSlice);
        when(subscriptionRepository.existsByLinkId(1L)).thenReturn(false); // Подписок больше нет

        LinkResponse response = linksService.removeLink(chatId, request);

        assertThat(response.getUrl()).isEqualTo(URI.create(url));
        assertThat(response.getTags()).containsExactly("tag1");

        verify(subscriptionRepository).deleteBySubscriptionId(new SubscriptionId(chatId, 1L));
        verify(linksRepository).deleteById(1L); // Ссылка должна удалиться из базы
        verify(metrics).decrementLinks("github.com");
    }

    @Test
    @DisplayName("removeLink — Ссылка не найдена у пользователя")
    void removeLink_NotFound() {
        RemoveLinkRequest request = RemoveLinkRequest.builder().build();
        request.setLink(URI.create(url));

        when(tgChatRepository.findById(chatId)).thenReturn(Optional.of(new Chat(chatId)));
        when(linkValidator.isValid(url)).thenReturn(true);
        when(linksRepository.findByChatIdAndUrl(chatId, url)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linksService.removeLink(chatId, request))
            .isInstanceOf(NoSuchElementException.class);
    }

    // --- GET ALL LINKS TESTS ---

    @Test
    @DisplayName("getAllLinks — Без тега возвращает список всех ссылок чата")
    void getAllLinks_NoTag_Success() {
        Chat chat = new Chat(chatId);
        Link link = new Link(url, OffsetDateTime.now());
        link.setId(1L);
        chat.addLink(link);

        when(tgChatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        ListLinksResponse response = linksService.getAllLinks(chatId, null);

        assertThat(response.getSize()).isEqualTo(1);
        assertThat(response.getLinks().get(0).getUrl()).isEqualTo(URI.create(url));
    }

    @Test
    @DisplayName("getAllLinks — С фильтром по тегу")
    void getAllLinks_WithTag_Success() {
        Chat chat = new Chat(chatId);
        Link link = new Link(url, OffsetDateTime.now());
        link.setId(1L);

        Subscription sub = new Subscription(chatId, 1L);
        sub.setLink(link);
        sub.setTags(List.of("java"));

        when(tgChatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(subscriptionRepository.findSubscriptionsByChatIdAndTag(chatId, "java")).thenReturn(List.of(sub));

        ListLinksResponse response = linksService.getAllLinks(chatId, "java");

        assertThat(response.getSize()).isEqualTo(1);
        assertThat(response.getLinks().get(0).getTags()).containsExactly("java");
    }
}
