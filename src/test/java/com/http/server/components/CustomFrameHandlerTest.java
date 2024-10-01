package com.http.server.components;

import com.http.server.utils.AppProperties;
import com.http.server.http.HttpServerManager;
import com.paicbd.smsc.ws.SocketSession;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.simp.stomp.StompSession;

import static com.http.server.utils.Constants.UPDATE_SERVICE_PROVIDER_ENDPOINT;
import static com.http.server.utils.Constants.UPDATE_SERVER_HANDLER_ENDPOINT;
import static com.http.server.utils.Constants.SERVICE_PROVIDER_DELETED_ENDPOINT;
import static com.http.server.utils.Constants.STOP_INSTANCE_ENDPOINT;
import static com.http.server.utils.Constants.RESPONSE_HTTP_SERVER_ENDPOINT;
import static com.http.server.utils.Constants.GENERAL_SETTINGS_SMPP_HTTP_ENDPOINT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomFrameHandlerTest {

    private static final String INSTANCE_NAME = "instance123";
    private static final String PAYLOAD_SYSTEM_ID = "systemId123";
    private static final String PAYLOAD_INSTANCE = "instance123";
    private static final String PAYLOAD_OTHER_INSTANCE = "otherInstance";

    @Mock(strictness = Mock.Strictness.LENIENT) // In handleFrameLogic_invalidDestination test case, IllegalArgumentException is thrown by the method
    private SocketSession mockSocketSession;

    @Mock
    private HttpServerManager mockHttpServerManager;

    @Mock
    private AppProperties mockAppProperties;

    @Mock
    private StompHeaders mockStompHeaders;

    @Mock
    private StompSession mockStompSession;

    @Mock
    private CreditHandler creditHandler;

    @InjectMocks
    private CustomFrameHandler customFrameHandler;

    @BeforeEach
    public void setUp() {
        when(mockSocketSession.getStompSession()).thenReturn(mockStompSession);
    }

    @Test
    void handleFrameLogic_updateServiceProvider() {
        when(mockStompHeaders.getDestination()).thenReturn(UPDATE_SERVICE_PROVIDER_ENDPOINT);
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(mockStompHeaders, PAYLOAD_SYSTEM_ID));
    }

    @Test
    void handleFrameLogic_updateServerHandler() {
        when(mockAppProperties.getInstanceName()).thenReturn(INSTANCE_NAME);
        when(mockStompHeaders.getDestination()).thenReturn(UPDATE_SERVER_HANDLER_ENDPOINT);
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(mockStompHeaders, INSTANCE_NAME));
        verify(mockHttpServerManager).manageServerHandler();
    }

    @Test
    void handleFrameLogic_updateServerHandler_failure() {
        when(mockAppProperties.getInstanceName()).thenReturn(PAYLOAD_INSTANCE);
        when(mockStompHeaders.getDestination()).thenReturn(UPDATE_SERVER_HANDLER_ENDPOINT);
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(mockStompHeaders, PAYLOAD_OTHER_INSTANCE));
        verifyNoInteractions(mockHttpServerManager);
    }

    @Test
    void handleFrameLogic_deleteServiceProvider() {
        when(mockStompHeaders.getDestination()).thenReturn(SERVICE_PROVIDER_DELETED_ENDPOINT);
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(mockStompHeaders, PAYLOAD_SYSTEM_ID));
        verify(mockHttpServerManager).removeServiceProvider(PAYLOAD_SYSTEM_ID);
    }

    @Test
    void handleFrameLogic_deleteServiceProvider_failure() {
        when(mockStompHeaders.getDestination()).thenReturn(SERVICE_PROVIDER_DELETED_ENDPOINT);
        doThrow(new RuntimeException("Failed to remove service provider")).when(mockHttpServerManager).removeServiceProvider(PAYLOAD_SYSTEM_ID);
        assertThrows(RuntimeException.class, () -> customFrameHandler.handleFrameLogic(mockStompHeaders, PAYLOAD_SYSTEM_ID));
        verify(mockHttpServerManager).removeServiceProvider(PAYLOAD_SYSTEM_ID);
    }

    @Test
    void handleFrameLogic_stopInstance_failure() {
        when(mockAppProperties.getInstanceName()).thenReturn(INSTANCE_NAME);
        when(mockStompHeaders.getDestination()).thenReturn(STOP_INSTANCE_ENDPOINT);
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(mockStompHeaders, PAYLOAD_SYSTEM_ID));
        verifyNoInteractions(mockHttpServerManager);
    }

    @Test
    void handleFrameLogic_updateGeneralSettingsSmppHttp() {
        when(mockStompHeaders.getDestination()).thenReturn(GENERAL_SETTINGS_SMPP_HTTP_ENDPOINT);
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(mockStompHeaders, PAYLOAD_SYSTEM_ID));
        verify(mockHttpServerManager).loadOrUpdateGeneralSettingsCache();
    }

    @Test
    void handleFrameLogic_unknownDestination() {
        when(mockStompHeaders.getDestination()).thenReturn("unknown_destination");
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(mockStompHeaders, PAYLOAD_SYSTEM_ID));
        verifyNoInteractions(mockHttpServerManager);
    }

    @Test
    void handleFrameLogic_stopInstance() {
        when(mockAppProperties.getInstanceName()).thenReturn(INSTANCE_NAME);
        when(mockStompHeaders.getDestination()).thenReturn(STOP_INSTANCE_ENDPOINT);

        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(mockStompHeaders, INSTANCE_NAME));

        verify(mockStompSession).send(RESPONSE_HTTP_SERVER_ENDPOINT, "OK");
    }
}