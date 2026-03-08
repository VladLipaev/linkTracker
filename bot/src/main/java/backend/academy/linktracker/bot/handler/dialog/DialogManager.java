package backend.academy.linktracker.bot.handler.dialog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
// используется как кэш память для хранения состояния машины состояний и ссылки
public class DialogManager {

    private final Map<Long, UserSession> activeSessions = new ConcurrentHashMap<>();

    public UserSession getSession(long chatId) {
        return activeSessions.getOrDefault(chatId, UserSession.base());
    }

    public void setSession(long chatId, UserSession session) {
        if (session.state() == UserState.BASE) {
            activeSessions.remove(chatId);
        } else {
            activeSessions.put(chatId, session);
        }
    }
}
