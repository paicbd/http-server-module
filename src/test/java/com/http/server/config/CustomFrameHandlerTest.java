package com.http.server.config;

import com.http.server.http.HttpServerManager;
import com.http.server.utils.AppProperties;
import com.paicbd.smsc.dto.ServiceProvider;
import com.paicbd.smsc.ws.SocketSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.util.concurrent.ConcurrentMap;

import static com.http.server.utils.Constants.GENERAL_SETTINGS_SMPP_HTTP_ENDPOINT;
import static com.http.server.utils.Constants.SERVICE_PROVIDER_DELETED_ENDPOINT;
import static com.http.server.utils.Constants.STOP_INSTANCE_ENDPOINT;
import static com.http.server.utils.Constants.UPDATE_SERVER_HANDLER_ENDPOINT;
import static com.http.server.utils.Constants.UPDATE_SERVICE_PROVIDER_ENDPOINT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomFrameHandlerTest {

    @Mock
    private SocketSession socketSession;

    @Mock
    private HttpServerManager httpServerManager;

    @Mock
    private AppProperties appProperties;

    @Mock
    private ConcurrentMap<Integer, String> systemIdByNetworkIdCache;

    @Mock
    private ConcurrentMap<String, ServiceProvider> serviceProviderBySystemIdCache;

    @Mock
    private ConcurrentMap<Integer, ServiceProvider> serviceProviderByNetworkIdCache;

    @Mock
    private StompHeaders stompHeaders;

    @InjectMocks
    private CustomFrameHandler customFrameHandler;

    @Test
    void testHandleFrameLogic_UpdateServiceProvider() {
        when(stompHeaders.getDestination()).thenReturn(UPDATE_SERVICE_PROVIDER_ENDPOINT);
        String payload = "123";

        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(stompHeaders, payload));
    }

    @Test
    void testHandleFrameLogic_UpdateServer() {
        when(stompHeaders.getDestination()).thenReturn(UPDATE_SERVER_HANDLER_ENDPOINT);
        String payload = "testInstance";

        when(appProperties.getInstanceName()).thenReturn("testInstance");
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(stompHeaders, payload));

        String notEqualPayload = "notEqualPayload";
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(stompHeaders, notEqualPayload));
    }

    @Test
    void testHandleFrameLogic_ServiceProviderDeleted() {
        when(stompHeaders.getDestination()).thenReturn(SERVICE_PROVIDER_DELETED_ENDPOINT);
        String payload = "123";

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setSystemId("testSystemId");
        serviceProvider.setNetworkId(123);

        serviceProviderByNetworkIdCache.putIfAbsent(123, serviceProvider);
        customFrameHandler = new CustomFrameHandler(socketSession, httpServerManager, appProperties, systemIdByNetworkIdCache, serviceProviderBySystemIdCache, serviceProviderByNetworkIdCache);
        when(serviceProviderByNetworkIdCache.remove(anyInt())).thenReturn(serviceProvider);
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(stompHeaders, payload));
    }

    @Test
    void testHandleFrameLogic_Default() {
        when(stompHeaders.getDestination()).thenReturn("/unknown");
        String payload = "testInstance";

        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(stompHeaders, payload));
    }

    @Test
    void testHandleFrameLogic_StopInstance() {
        when(stompHeaders.getDestination()).thenReturn(STOP_INSTANCE_ENDPOINT);
        String payload = "testInstance";

        when(appProperties.getInstanceName()).thenReturn("testInstance");
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(stompHeaders, payload));

        String notEqualPayload = "notEqualPayload";
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(stompHeaders, notEqualPayload));
    }

    @Test
    void testHandleFrameLogic_GeneralSettingsSmppHttp() {
        when(stompHeaders.getDestination()).thenReturn(GENERAL_SETTINGS_SMPP_HTTP_ENDPOINT);
        String payload = "payload";

        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(stompHeaders, payload));
    }

    @Test
    void testHandleFrameLogic_UnknownDestination() {
        when(stompHeaders.getDestination()).thenReturn("/unknown");

        String payload = "payload";
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(stompHeaders, payload));
    }

    @Test
    void testHandleFrameLogic_NullPayload() {
        String payload = null;
        StompHeaders headers = new StompHeaders();

        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            customFrameHandler.handleFrameLogic(headers, payload);
        });

        assertInstanceOf(NullPointerException.class, exception);
    }


    @Test
    void testHandleFrameLogic_NullDestination() {
        String payload = "payload";
        StompHeaders headers = new StompHeaders();

        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            customFrameHandler.handleFrameLogic(headers, payload);
        });

        assertInstanceOf(NullPointerException.class, exception);
    }
}