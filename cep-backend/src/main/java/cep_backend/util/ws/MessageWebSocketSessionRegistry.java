package cep_backend.util.ws;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageWebSocketSessionRegistry {
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        if (userId == null || userId <= 0 || session == null) {
            return;
        }
        userSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(Long userId, WebSocketSession session) {
        if (userId == null || userId <= 0 || session == null) {
            return;
        }
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            userSessions.remove(userId);
        }
    }

    public Set<WebSocketSession> sessionsOf(Long userId) {
        if (userId == null || userId <= 0) {
            return Set.of();
        }
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(sessions);
    }
}
