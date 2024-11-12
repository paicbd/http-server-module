package com.http.server.config;

import com.http.server.utils.AppProperties;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.ws.SocketClient;
import com.paicbd.smsc.ws.SocketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static com.http.server.utils.Constants.UPDATE_SERVICE_PROVIDER_ENDPOINT;
import static com.http.server.utils.Constants.UPDATE_SERVER_HANDLER_ENDPOINT;
import static com.http.server.utils.Constants.SERVICE_PROVIDER_DELETED_ENDPOINT;
import static com.http.server.utils.Constants.STOP_INSTANCE_ENDPOINT;
import static com.http.server.utils.Constants.GENERAL_SETTINGS_SMPP_HTTP_ENDPOINT;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {
    private final AppProperties appProperties;
    private final SocketSession socketSession;
    private final CustomFrameHandler customFrameHandler;

    @Bean
    public SocketClient socketClient() {
        List<String> topicsToSubscribe = List.of(
                UPDATE_SERVICE_PROVIDER_ENDPOINT,
                UPDATE_SERVER_HANDLER_ENDPOINT,
                SERVICE_PROVIDER_DELETED_ENDPOINT,
                STOP_INSTANCE_ENDPOINT,
                GENERAL_SETTINGS_SMPP_HTTP_ENDPOINT
        );
        UtilsRecords.WebSocketConnectionParams wsp = new UtilsRecords.WebSocketConnectionParams(
                appProperties.isWsEnabled(),
                appProperties.getWsHost(),
                appProperties.getWsPort(),
                appProperties.getWsPath(),
                topicsToSubscribe,
                appProperties.getWsHeaderName(),
                appProperties.getWsHeaderValue(),
                appProperties.getWsRetryInterval(),
                "HTTP-SERVER"
        );
        return new SocketClient(customFrameHandler, wsp, socketSession);
    }
}
