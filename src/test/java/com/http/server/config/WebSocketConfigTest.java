package com.http.server.config;

import com.http.server.utils.AppProperties;
import com.paicbd.smsc.ws.SocketSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {
    @Mock
    AppProperties appProperties;
    @Mock
    SocketSession socketSession;
    @Mock
    CustomFrameHandler customFrameHandler;

    @InjectMocks
    WebSocketConfig webSocketConfig;

    @Test
    void socketClient() {
        assertNotNull(webSocketConfig.socketClient());
    }
}