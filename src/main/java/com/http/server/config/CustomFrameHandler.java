package com.http.server.config;

import com.http.server.http.HttpServerManager;
import com.http.server.utils.AppProperties;
import com.paicbd.smsc.dto.ServiceProvider;
import com.paicbd.smsc.ws.FrameHandler;
import com.paicbd.smsc.ws.SocketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

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
    private final ConcurrentMap<Integer, String> systemIdByNetworkIdCache;
    private final ConcurrentMap<String, ServiceProvider> serviceProviderBySystemIdCache;
    private final ConcurrentMap<Integer, ServiceProvider> serviceProviderByNetworkIdCache;

    @Override
    public void handleFrameLogic(StompHeaders headers, Object payload) {
        String text = payload.toString();
        String destination = headers.getDestination();
        Objects.requireNonNull(text, "payload cannot be null");
        Objects.requireNonNull(destination, "Destination cannot be null");

        switch (destination) {
            case UPDATE_SERVICE_PROVIDER_ENDPOINT -> handleUpdateServiceProvider(text);
            case UPDATE_SERVER_HANDLER_ENDPOINT -> handleUpdateServer(text);
            case SERVICE_PROVIDER_DELETED_ENDPOINT -> handleDeleteServiceProvider(text);
            case STOP_INSTANCE_ENDPOINT -> handleStopInstance(text);
            case GENERAL_SETTINGS_SMPP_HTTP_ENDPOINT -> httpServerManager.loadOrUpdateGeneralSettingsCache();
            default -> log.warn("Unknown destination: {}", destination);
        }
    }

    private void handleUpdateServiceProvider(String stringNetworkId) {
        log.info("Updating service provider with networkId {}", stringNetworkId);
        httpServerManager.updateServiceProvider(stringNetworkId);
    }

    private void handleUpdateServer(String payload) {
        if (Objects.equals(payload, appProperties.getInstanceName())) {
            log.warn("Received Notification for updateServerHandler");
            httpServerManager.manageServerHandler();
        }
    }

    private void handleDeleteServiceProvider(String stringNetworkId) {
        log.warn("Received Notification for serviceProviderDeleted");
        ServiceProvider toDelete = serviceProviderByNetworkIdCache.remove(Integer.parseInt(stringNetworkId));
        systemIdByNetworkIdCache.remove(Integer.parseInt(stringNetworkId));
        serviceProviderBySystemIdCache.remove(toDelete.getSystemId());
    }

    private void handleStopInstance(String payload) {
        if (payload.equals(appProperties.getInstanceName())) {
            log.warn("Stopping this instance: {}", appProperties.getInstanceName());
        }
    }
}
