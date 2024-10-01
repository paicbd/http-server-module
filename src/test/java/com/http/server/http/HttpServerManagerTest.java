package com.http.server.http;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.type.TypeReference;
import com.http.server.utils.AppProperties;
import com.http.server.components.AutoRegister;
import com.paicbd.smsc.dto.ServiceProvider;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.ws.SocketSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisCluster;
import ch.qos.logback.classic.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.http.server.utils.Constants.STARTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HttpServerManagerTest {

    @Mock
    private JedisCluster jedisCluster;

    @Mock
    private AppProperties appProperties;

    @Mock
    private SocketSession socketSession;

    @Mock
    private AutoRegister autoRegister;

    @InjectMocks
    private HttpServerManager httpServerManager;

    @BeforeEach
    void setUp()  {
        httpServerManager = new HttpServerManager(jedisCluster, appProperties, socketSession, autoRegister);

        when(appProperties.getServiceProvidersHashTable()).thenReturn("service_providers");
        when(appProperties.getHttpGeneralSettingsHash()).thenReturn("general_settings");
        when(appProperties.getConfigurationHash()).thenReturn("configurations");
        when(appProperties.getServerName()).thenReturn("http-server-instance-01");

        String serviceProviderJson = "{\"network_id\":1,\"name\":\"spHttp\",\"system_id\":\"spHttp\",\"password\":\"1234\",\"ip\":\"192.168.100.20\",\"port\":7001,\"bind_type\":\"TRANSCEIVER\",\"system_type\":\"\",\"interface_version\":\"IF_50\",\"sessions_number\":10,\"address_ton\":0,\"address_npi\":0,\"address_range\":\"500\",\"tps\":10,\"status\":\"STOPPED\",\"enabled\":0,\"enquire_link_period\":30000,\"enquire_link_timeout\":0,\"request_dlr\":true,\"no_retry_error_code\":\"\",\"retry_alternate_destination_error_code\":\"\",\"bind_timeout\":5000,\"bind_retry_period\":10000,\"pdu_timeout\":5000,\"pdu_degree\":1,\"thread_pool_size\":100,\"mno_id\":1,\"tlv_message_receipt_id\":false,\"message_id_decimal_format\":false,\"active_sessions_numbers\":0,\"protocol\":\"SMPP\",\"auto_retry_error_code\":\"\",\"encoding_iso88591\":3,\"encoding_gsm7\":0,\"encoding_ucs2\":2,\"split_message\":false,\"split_smpp_type\":\"TLV\"}";
        when(jedisCluster.hget("service_providers", "spHttp")).thenReturn(serviceProviderJson);
        String serviceProviderJson2 = "{\"network_id\":1,\"name\":\"spHttp2\",\"system_id\":\"spHtt2\",\"password\":\"1234\",\"ip\":\"192.168.100.20\",\"port\":7001,\"bind_type\":\"TRANSCEIVER\",\"system_type\":\"\",\"interface_version\":\"IF_50\",\"sessions_number\":10,\"address_ton\":0,\"address_npi\":0,\"address_range\":\"500\",\"tps\":10,\"status\":\"STOPPED\",\"enabled\":0,\"enquire_link_period\":30000,\"enquire_link_timeout\":0,\"request_dlr\":true,\"no_retry_error_code\":\"\",\"retry_alternate_destination_error_code\":\"\",\"bind_timeout\":5000,\"bind_retry_period\":10000,\"pdu_timeout\":5000,\"pdu_degree\":1,\"thread_pool_size\":100,\"mno_id\":1,\"tlv_message_receipt_id\":false,\"message_id_decimal_format\":false,\"active_sessions_numbers\":0,\"protocol\":\"SMPP\",\"auto_retry_error_code\":\"\",\"encoding_iso88591\":3,\"encoding_gsm7\":0,\"encoding_ucs2\":2,\"split_message\":false,\"split_smpp_type\":\"TLV\"}";
        when(jedisCluster.hget("service_providers", "spHttp")).thenReturn(serviceProviderJson2);

        httpServerManager.getServiceProviderCache().put("spHttp", Converter.stringToObject(serviceProviderJson, new TypeReference<>() {}));
        httpServerManager.getServiceProviderCache().put("spHttp2", Converter.stringToObject(serviceProviderJson2, new TypeReference<>() {}));

        String generalSettingJson = "{\"id\":1,\"validity_period\":60,\"max_validity_period\":240,\"source_addr_ton\":1,\"source_addr_npi\":1,\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"encoding_iso88591\":3,\"encoding_gsm7\":0,\"encoding_ucs2\":2}\n";
        httpServerManager.getGeneralSettingsCache().put("1", Converter.stringToObject(generalSettingJson, new TypeReference<>() {}));
        when(jedisCluster.hget("general_settings", "1")).thenReturn(generalSettingJson);

        String generalSettingManager = "{\"state\":\"STARTED\"}";
        when(jedisCluster.hget("configurations", "http-server-instance-01")).thenReturn(generalSettingManager);

        httpServerManager.getServiceProviderByNetworkIdCache().put(1, Converter.stringToObject(serviceProviderJson, new TypeReference<>() {}));
    }

    @Test
    void initializeCaches() {
        httpServerManager.initializeCaches();

        verify(autoRegister).register();


        assertFalse(httpServerManager.getServiceProviderCache().isEmpty(), "ServiceProviderCache should contain at least one entry");
        assertFalse(httpServerManager.getServiceProviderByNetworkIdCache().isEmpty(), "ServiceProviderByNetworkIdCache should contain at least one entry");
        assertFalse(httpServerManager.getGeneralSettingsCache().isEmpty(), "GeneralSettingsCache should contain at least one entry");
    }

    @Test
    void initializeCaches_withInvalidJson_shouldLogError() {
        Map<String, String> serviceProvidersData = new HashMap<>();
        serviceProvidersData.put("spHttp", "{\"network_id\":1,\"name\":\"spHttp\",\"system_id\":\"spHttp\",\"password\":\"1234\",\"ip\":\"192.168.100.20\",\"port\":7001");
        when(jedisCluster.hgetAll("service_providers")).thenReturn(serviceProvidersData);

        Map<String, String> generalSettingsData = new HashMap<>();
        generalSettingsData.put("1", "{\"id\":1,\"validity_period\":60,\"max_validity_period\":240,\"source_addr_ton\":1,\"source_addr_npi\":1,\"dest_addr_ton\":1,\"dest_addr_npi\":1}");
        when(jedisCluster.hgetAll("general_settings")).thenReturn(generalSettingsData);

        Logger logger = (Logger) LoggerFactory.getLogger(HttpServerManager.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        httpServerManager.initializeCaches();

        verify(autoRegister).register();

        List<ILoggingEvent> logsList = listAppender.list;
        boolean errorLogged = logsList.stream().anyMatch(event -> event.getLevel() == ch.qos.logback.classic.Level.ERROR && event.getFormattedMessage().contains("Error initializing cache for key spHttp"));
        assertTrue(errorLogged, "Expected error log was not found");

        logger.detachAppender(listAppender);
    }

    @Test
    void stateGetterSetter() {
        String expectedState = "RUNNING";
        httpServerManager.setState(expectedState);
        assertEquals(expectedState, httpServerManager.getState(), "El getter y setter de state deben funcionar correctamente");
    }

    @Test
    void loadServiceProviderCache() {
        Map<String, String> serviceProvidersData = new HashMap<>();
        String serviceProviderJson = "{\"network_id\":1,\"name\":\"spHttp\",\"system_id\":\"spHttp\",\"password\":\"1234\",\"ip\":\"192.168.100.20\",\"port\":7001,\"bind_type\":\"TRANSCEIVER\",\"system_type\":\"\",\"interface_version\":\"IF_50\",\"sessions_number\":10,\"address_ton\":0,\"address_npi\":0,\"address_range\":\"500\",\"tps\":10,\"status\":\"STOPPED\",\"enabled\":0,\"enquire_link_period\":30000,\"enquire_link_timeout\":0,\"request_dlr\":true,\"no_retry_error_code\":\"\",\"retry_alternate_destination_error_code\":\"\",\"bind_timeout\":5000,\"bind_retry_period\":10000,\"pdu_timeout\":5000,\"pdu_degree\":1,\"thread_pool_size\":100,\"mno_id\":1,\"tlv_message_receipt_id\":false,\"message_id_decimal_format\":false,\"active_sessions_numbers\":0,\"protocol\":\"SMPP\",\"auto_retry_error_code\":\"\",\"encoding_iso88591\":3,\"encoding_gsm7\":0,\"encoding_ucs2\":2,\"split_message\":false,\"split_smpp_type\":\"TLV\"}";
        serviceProvidersData.put("spHttp", serviceProviderJson);
        when(jedisCluster.hgetAll("service_providers")).thenReturn(serviceProvidersData);

        httpServerManager.loadServiceProviderCache();

        assertFalse(httpServerManager.getServiceProviderCache().isEmpty(), "ServiceProviderCache should contain at least one entry");
        assertEquals(1, httpServerManager.getServiceProviderByNetworkIdCache().size());

        ServiceProvider serviceProvider = httpServerManager.getServiceProviderCache().get("spHttp");
        assertNotNull(serviceProvider);
        assertEquals("spHttp", serviceProvider.getSystemId());
        assertEquals(1, httpServerManager.getServiceProviderByNetworkIdCache().get(1).getNetworkId());
    }

    @Test
    void loadOrUpdateGeneralSettingsCache() {
        String generalSettingsJson = "{\"source_addr_ton\":\"1\", \"validity_period\":\"10\"}";  // Simula un JSON válido que representa la configuración general.
        when(jedisCluster.hget(appProperties.getHttpGeneralSettingsHash(), appProperties.getHttpGeneralSettingsKey()))
                .thenReturn(generalSettingsJson);

        httpServerManager.loadOrUpdateGeneralSettingsCache();

        assertNotNull(httpServerManager.getGeneralSettings(), "GeneralSettings should not be null");
    }

    @Test
    void updateServiceProvider_serviceProvider() {
        // serviceProvideInRaw is not FOUND
        when(jedisCluster.hget(appProperties.getServiceProvidersHashTable(), "spHttp3")).thenReturn(null);
        assertDoesNotThrow(() -> httpServerManager.updateServiceProvider("spHttp3"));

        // serviceProvideInRaw is existing
        assertDoesNotThrow(() -> httpServerManager.updateServiceProvider("spHttp"));

        // update service provider with enabled != 0
        when(jedisCluster.hget(appProperties.getServiceProvidersHashTable(), "spHttp2")).thenReturn("{\"network_id\":1,\"name\":\"spHttp2\",\"system_id\":\"spHtt2\",\"password\":\"1234\",\"ip\":\"192.168.100.20\",\"port\":8080,\"bind_type\":\"TRANSCEIVER\",\"system_type\":\"\",\"interface_version\":\"IF_50\",\"sessions_number\":10,\"address_ton\":0,\"address_npi\":0,\"address_range\":\"500\",\"tps\":10,\"status\":\"STOPPED\",\"enabled\":1,\"enquire_link_period\":30000,\"enquire_link_timeout\":0,\"request_dlr\":true,\"no_retry_error_code\":\"\",\"retry_alternate_destination_error_code\":\"\",\"bind_timeout\":5000,\"bind_retry_period\":10000,\"pdu_timeout\":5000,\"pdu_degree\":1,\"thread_pool_size\":100,\"mno_id\":1,\"tlv_message_receipt_id\":false,\"message_id_decimal_format\":false,\"active_sessions_numbers\":0,\"protocol\":\"SMPP\",\"auto_retry_error_code\":\"\",\"encoding_iso88591\":3,\"encoding_gsm7\":0,\"encoding_ucs2\":2,\"split_message\":false,\"split_smpp_type\":\"TLV\"}");
        assertDoesNotThrow(() -> httpServerManager.updateServiceProvider("spHttp2"));
    }

    @Test
    void removeServiceProvider_serviceProvider() {
        when(jedisCluster.hdel(appProperties.getServiceProvidersHashTable(), "spHttp2")).thenReturn(1L);
        assertDoesNotThrow(() -> httpServerManager.removeServiceProvider("doesNotExists"));
        assertDoesNotThrow(() -> this.httpServerManager.removeServiceProviderFromCache("spHttp2"));
    }

    @Test
    void isServerActive_serverStarted() {
        httpServerManager.setState(STARTED);
        assertTrue(httpServerManager.isServerActive());
    }

    @Test
    void isServerActive_serverNotStarted() {
        httpServerManager.setState("STOPPED");
        assertFalse(httpServerManager.isServerActive());
    }

    @Test
    void manageServerHandler_shouldSetStateAndLog() {
        String serverHandlerJson = "{\"state\":\"STARTED\"}";
        when(jedisCluster.hget(appProperties.getConfigurationHash(), appProperties.getServerName()))
                .thenReturn(serverHandlerJson);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        ((Logger) LoggerFactory.getLogger(HttpServerManager.class)).addAppender(listAppender);

        httpServerManager.manageServerHandler();

        assertEquals("STARTED", httpServerManager.getState(), "El estado del servidor debe ser 'STARTED'");

        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.stream().anyMatch(event -> event.getFormattedMessage().contains("STARTED")), "Debe contener el mensaje de log con el estado 'STARTED'");

        listAppender.stop();
    }

    @Test
    void manageServerHandler_shouldHandleNullServerHandlerJson() {
        when(jedisCluster.hget(appProperties.getConfigurationHash(), appProperties.getServerName()))
                .thenReturn(null);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        ((Logger) LoggerFactory.getLogger(HttpServerManager.class)).addAppender(listAppender);

        httpServerManager.manageServerHandler();

        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.stream().anyMatch(event -> event.getFormattedMessage().contains("ServerHandler not found in Redis")), "Debe contener el mensaje de error 'ServerHandler not found in Redis'");

        listAppender.stop();
    }

    @Test
    void manageServerHandler_shouldHandleException() {
        when(jedisCluster.hget(appProperties.getConfigurationHash(), appProperties.getServerName()))
                .thenThrow(new RuntimeException("Simulated Redis exception"));

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        ((Logger) LoggerFactory.getLogger(HttpServerManager.class)).addAppender(listAppender);

        assertDoesNotThrow(() -> httpServerManager.manageServerHandler());

        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.stream().anyMatch(event -> event.getFormattedMessage().contains("Error on getServerHandler: Simulated Redis exception")), "Debe contener el mensaje de error con la excepción simulada");

        listAppender.stop();
    }

    @Test
    void getServiceProviderByNetworkId_existingId() {
        ServiceProvider result = httpServerManager.getServiceProviderByNetworkId(1);
        assertNotNull(result);
    }

    @Test
    void getServiceProvider() {
        assertDoesNotThrow(() -> httpServerManager.getServiceProvider("spHttp"));
    }

    @Test
    void getGeneralSettings() {
        assertDoesNotThrow(() -> httpServerManager.getGeneralSettings());
    }
}
