package com.http.server.components;

import com.http.server.http.HttpServerManager;
import com.http.server.utils.AppProperties;
import com.paicbd.smsc.ws.FrameHandler;
import com.paicbd.smsc.ws.SocketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.http.server.utils.Constants.UPDATE_SERVICE_PROVIDER_ENDPOINT;
import static com.http.server.utils.Constants.UPDATE_SERVER_HANDLER_ENDPOINT;
import static com.http.server.utils.Constants.SERVICE_PROVIDER_DELETED_ENDPOINT;
import static com.http.server.utils.Constants.STOP_INSTANCE_ENDPOINT;
import static com.http.server.utils.Constants.GENERAL_SETTINGS_SMPP_HTTP_ENDPOINT;
import static com.http.server.utils.Constants.RESPONSE_HTTP_SERVER_ENDPOINT;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomFrameHandler implements FrameHandler {
    private final SocketSession socketSession;
    private final HttpServerManager httpServerManager;
    private final AppProperties appProperties;
    private final CreditHandler creditHandler;

    @Override
    public void handleFrameLogic(StompHeaders headers, Object payload) {
        String systemId = payload.toString();
        String destination = headers.getDestination();
        Objects.requireNonNull(systemId, "System ID cannot be null");
        Objects.requireNonNull(destination, "Destination cannot be null");

        switch (destination) {
            case UPDATE_SERVICE_PROVIDER_ENDPOINT -> handleUpdate(systemId, this::handleUpdateServiceProvider);
            case UPDATE_SERVER_HANDLER_ENDPOINT -> handleUpdate(payload.toString(), this::handleUpdateServer);
            case SERVICE_PROVIDER_DELETED_ENDPOINT -> handleUpdate(systemId, this::handleDeleteServiceProvider);
            case STOP_INSTANCE_ENDPOINT -> handleUpdate(systemId, this::handleStopInstance);
            case GENERAL_SETTINGS_SMPP_HTTP_ENDPOINT -> httpServerManager.loadOrUpdateGeneralSettingsCache();
            default -> log.warn("Unknown destination: {}", destination);
        }
    }

    private void handleUpdate(String payload, java.util.function.Consumer<String> handler) {
        handler.accept(payload);
        socketSession.getStompSession().send(RESPONSE_HTTP_SERVER_ENDPOINT, "OK");
    }

    private void handleUpdateServiceProvider(String systemId) {
        log.info("Updating service provider {}", systemId);
        creditHandler.removeFromRedisAndCache(systemId);
        httpServerManager.updateServiceProvider(systemId);
    }

    private void handleUpdateServer(String payload) {
        if (Objects.equals(payload, appProperties.getInstanceName())) {
            log.warn("Received Notification for updateServerHandler");
            httpServerManager.manageServerHandler();
        }
    }

    private void handleDeleteServiceProvider(String systemId) {
        log.warn("Received Notification for serviceProviderDeleted");
        httpServerManager.removeServiceProvider(systemId);
    }

    private void handleStopInstance(String payload) {
        if (payload.equals(appProperties.getInstanceName())) {
            log.warn("Stopping this instance: {}", appProperties.getInstanceName());
        }
    }
}
