package backend.academy.linktracker.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.scrapper.ScrapperClient;
import backend.academy.linktracker.bot.client.telegram.TelegramClientFacade;
import backend.academy.linktracker.bot.configuration.CommandRegistry;
import backend.academy.linktracker.bot.controller.cache.CacheDialogUtil;
import backend.academy.linktracker.bot.controller.kafka.repository.IdempotencyRepository;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import backend.academy.linktracker.bot.handler.TelegramUpdateDispatcher;
import backend.academy.linktracker.bot.handler.dialog.BotLinkValidator;
import backend.academy.linktracker.bot.handler.dialog.DialogScrapperStepProcessor;
import backend.academy.linktracker.bot.handler.dialog.UserSession;
import backend.academy.linktracker.bot.handler.dialog.UserState;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.List;

public class CacheDialogIT extends AbstractIntegrationTest {

    @MockitoBean
    private TelegramClientFacade telegramClientFacade;

    @MockitoBean
    private ScrapperClient scrapperClient;

    @Autowired
    private CacheDialogUtil cacheDialogUtil;

    @Autowired
    private DialogScrapperStepProcessor dialogScrapperStepProcessor;

    @Autowired
    private TelegramUpdateDispatcher telegramUpdateDispatcher;

    @MockitoBean
    private IdempotencyRepository idempotencyRepository;

    private BotLinkValidator validator = new BotLinkValidator();

    @Autowired
    private RedisTemplate<String, UserSession> redisTemplate;

    @MockitoBean
    private CommandRegistry commandRegistry;

    private final long chatId = 123L;

    @Value("${app.redis.cache-key-prefix.dialog}")
    private String prefix;
    @BeforeEach
    void setUp(){
        cacheDialogUtil.invalidateChatCache(chatId);
    }

    @Test
    void requestGetLinks_UserStateWaitForTag_shouldSaveToRedis() {
        // Arrange
        UserSession session = UserSession.base();
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(123L);
        when(message.text()).thenReturn("/list");
        when(message.chat()).thenReturn(chat);
        when(update.message()).thenReturn(message);
        // Act
        UserSession userSessionBefore = redisTemplate.opsForValue().get(prefix + ":" +  chatId);
        assertNull(userSessionBefore);
        telegramUpdateDispatcher.dispatch(update);
        UserSession userSessionAfter = redisTemplate.opsForValue().get(prefix + ":" +  chatId);
         // Assert
        assertNotNull(userSessionAfter);
        assertEquals(
                UserState.WAITING_FOR_LIST_TAG,
                userSessionAfter.state());
    }

    @Test
    void requestGetLinksWithAnotherRequestTag_getUserSession_shouldBeUserStateWaitingForListTag() {
        // Arrange
        UserSession session = UserSession.base();
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(123L);
        when(message.text()).thenReturn("/list");
        when(message.chat()).thenReturn(chat);
        when(update.message()).thenReturn(message);

        UserSession session2 = UserSession.base();
        Update update2 = mock(Update.class);
        Message message2 = mock(Message.class);
        Chat chat2 = mock(Chat.class);
        when(chat2.id()).thenReturn(123L);
        when(message2.text()).thenReturn("skip");
        when(message2.chat()).thenReturn(chat2);
        when(update2.message()).thenReturn(message2);

        assertNull(cacheDialogUtil.getUserSession(chatId).orElse(null));

        telegramUpdateDispatcher.dispatch(update);
        UserSession userSession = cacheDialogUtil.getUserSession(chatId).orElse(null);
        assertNotNull(userSession);
        assertEquals(UserState.WAITING_FOR_LIST_TAG, userSession.state());

        UserSession userSession1 = redisTemplate.opsForValue().get(prefix + ":" + chatId);
        assertEquals(userSession, userSession1);

    }

    @Test
    void requestGetLinksWithAnotherRequestTagDone_invalidateCacheDialog_ValkeyShouldBeCleared() {
        // Arrange
        UserSession session = UserSession.base();
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(123L);
        when(message.text()).thenReturn("/list");
        when(message.chat()).thenReturn(chat);
        when(update.message()).thenReturn(message);

        UserSession session2 = UserSession.base();
        Update update2 = mock(Update.class);
        Message message2 = mock(Message.class);

        Chat chat2 = mock(Chat.class);
        when(chat2.id()).thenReturn(123L);
        when(message2.text()).thenReturn("skip");
        when(message2.chat()).thenReturn(chat2);
        when(update2.message()).thenReturn(message2);

        ListLinksResponse listLinksResponse = mock(ListLinksResponse.class);
        when(listLinksResponse.links()).thenReturn(List.of());
        when(scrapperClient.getLinks(chatId, null)).thenReturn(listLinksResponse);
        // Act
        telegramUpdateDispatcher.dispatch(update);
        UserSession userSessionBefore = redisTemplate.opsForValue().get(prefix + ":" +  chatId);
        telegramUpdateDispatcher.dispatch(update2);
        UserSession userSessionAfter = redisTemplate.opsForValue().get(prefix + ":" +  chatId);
        //проверяем что не сохранилось в локальном кеше
        UserSession userSessionFullyInvalidated = cacheDialogUtil.getUserSession(chatId).orElse(null);
        // Assert
        assertNotNull(userSessionBefore);
        assertEquals(UserState.WAITING_FOR_LIST_TAG, userSessionBefore.state());
        assertNull(userSessionAfter);
        assertNull(userSessionFullyInvalidated);
    }





}
