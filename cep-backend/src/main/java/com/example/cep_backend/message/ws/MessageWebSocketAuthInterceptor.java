package com.example.cep_backend.message.ws;

import com.example.cep_backend.auth.dto.AuthUserDto;
import com.example.cep_backend.auth.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Component
public class MessageWebSocketAuthInterceptor implements HandshakeInterceptor {
    public static final String ATTR_USER_ID = "userId";

    private final AuthService authService;

    public MessageWebSocketAuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String authorization = resolveAuthorization(request);
        if (authorization == null || authorization.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            AuthUserDto user = authService.currentUser(authorization);
            attributes.put(ATTR_USER_ID, user.userId());
            return true;
        } catch (RuntimeException ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // no-op
    }

    private String resolveAuthorization(ServerHttpRequest request) {
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders != null && !authHeaders.isEmpty()) {
            return authHeaders.getFirst();
        }

        String accessToken = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("accessToken");
        if (accessToken == null || accessToken.isBlank()) {
            return "";
        }
        return "Bearer " + accessToken.trim();
    }
}
