package com.http.server.http;

import com.http.server.dto.GlobalRecords;
import com.http.server.utils.AppProperties;
import com.paicbd.smsc.dto.ServiceProvider;
import com.paicbd.smsc.ws.SocketSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompSession;
import redis.clients.jedis.JedisCluster;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpServerManagerTest {
    private static final String HTTP_SERVICE_PROVIDER_JSON = "{\"name\":\"httpSp01\",\"password\":\"1234\",\"tps\":1,\"validity\":0,\"network_id\":5,\"system_id\":\"httpSp01\",\"system_type\":\"\",\"interface_version\":\"IF_50\",\"address_ton\":0,\"address_npi\":0,\"address_range\":\"^[0-9a-zA-Z]*$\",\"enabled\":1,\"enquire_link_period\":3000,\"pdu_timeout\":5000,\"request_dlr\":false,\"status\":\"STOPPED\",\"bind_type\":\"TRANSMITTER\",\"max_binds\":1,\"current_binds_count\":0,\"credit\":0,\"binds\":[],\"credit_used\":0,\"is_prepaid\":true,\"has_available_credit\":false,\"protocol\":\"HTTP\",\"contact_name\":\"Obedis\",\"email\":\"mail@mali.com\",\"phone_number\":\"223232\",\"callback_url\":\"http://18.224.164.85:3000/api/callback\",\"authentication_types\":\"Undefined\",\"header_security_name\":\"\",\"token\":\"Undefined \",\"callback_headers_http\":[]}";
    private static final String HTTP_SERVICE_PROVIDER_JSON_ENABLED_0 = "{\"name\":\"httpSp04\",\"password\":\"1234\",\"tps\":1,\"validity\":0,\"network_id\":3,\"system_id\":\"httpSp04\",\"system_type\":\"\",\"interface_version\":\"IF_50\",\"address_ton\":0,\"address_npi\":0,\"address_range\":\"^[0-9a-zA-Z]*$\",\"enabled\":0,\"enquire_link_period\":3000,\"pdu_timeout\":5000,\"request_dlr\":false,\"status\":\"STOPPED\",\"bind_type\":\"TRANSMITTER\",\"max_binds\":1,\"current_binds_count\":0,\"credit\":0,\"binds\":[],\"credit_used\":0,\"is_prepaid\":true,\"has_available_credit\":false,\"protocol\":\"HTTP\",\"contact_name\":\"Obedis\",\"email\":\"mail@mali.com\",\"phone_number\":\"223232\",\"callback_url\":\"http://18.224.164.85:3000/api/callback\",\"authentication_types\":\"Undefined\",\"header_security_name\":\"\",\"token\":\"Undefined \",\"callback_headers_http\":[]}";
    private static final String SMPP_SERVICE_PROVIDER_JSON = "{\"name\":\"smppSp03\",\"password\":\"1234\",\"tps\":1,\"validity\":0,\"network_id\":2,\"system_id\":\"smppSp03\",\"system_type\":\"\",\"interface_version\":\"IF_50\",\"address_ton\":0,\"address_npi\":0,\"address_range\":\"^[0-9a-zA-Z]*$\",\"enabled\":1,\"enquire_link_period\":3000,\"pdu_timeout\":5000,\"request_dlr\":false,\"status\":\"STOPPED\",\"bind_type\":\"TRANSMITTER\",\"max_binds\":1,\"current_binds_count\":0,\"credit\":0,\"binds\":[],\"credit_used\":0,\"is_prepaid\":true,\"has_available_credit\":false,\"protocol\":\"SMPP\",\"contact_name\":\"Obedis\",\"email\":\"mail@mali.com\",\"phone_number\":\"223232\",\"callback_url\":\"http://18.224.164.85:3000/api/callback\",\"authentication_types\":\"Undefined\",\"header_security_name\":\"\",\"token\":\"Undefined \",\"callback_headers_http\":[]}";
    private static final String GENERAL_SETTINGS_JSON = "{\"id\":1,\"validity_period\":60,\"max_validity_period\":240,\"source_addr_ton\":1,\"source_addr_npi\":1,\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"encoding_iso88591\":3,\"encoding_gsm7\":0,\"encoding_ucs2\":2}";

    @Mock
    private JedisCluster jedisCluster;

    @Mock
    private AppProperties appProperties;

    @Mock
    private SocketSession socketSession;

    @Mock
    private ConcurrentMap<Integer, String> systemIdByNetworkIdCache;

    @Mock
    private ConcurrentMap<String, ServiceProvider> serviceProviderBySystemIdCache;

    @Mock
    private ConcurrentMap<Integer, ServiceProvider> serviceProviderByNetworkIdCache;

    @Mock
    private GlobalRecords.ServerHandler serverHandler;

    @InjectMocks
    private HttpServerManager httpServerManager;

    @Test
    void testInitializeCaches() {
        assertDoesNotThrow(() -> httpServerManager.initializeCaches());
    }

    @Test
    void testLoadServiceProviderGettingEmptyCache() {
        when(appProperties.getServiceProvidersHashTable()).thenReturn("service_providers");
        when(jedisCluster.hgetAll("service_providers")).thenReturn(Map.of());

        assertDoesNotThrow(() -> httpServerManager.loadServiceProviderCache());
    }

    @Test
    void testLoadServiceProviderCache() {
        when(appProperties.getServiceProvidersHashTable()).thenReturn("service_providers");
        Map<String, String> serviceProvidersData = new HashMap<>();
        serviceProvidersData.put("5", HTTP_SERVICE_PROVIDER_JSON);
        serviceProvidersData.put("2", SMPP_SERVICE_PROVIDER_JSON);
        when(jedisCluster.hgetAll("service_providers")).thenReturn(serviceProvidersData);

        serviceProviderByNetworkIdCache = new ConcurrentHashMap<>();
        serviceProviderBySystemIdCache = new ConcurrentHashMap<>();
        systemIdByNetworkIdCache = new ConcurrentHashMap<>();

        httpServerManager = new HttpServerManager(jedisCluster, appProperties, socketSession, systemIdByNetworkIdCache, serviceProviderBySystemIdCache, serviceProviderByNetworkIdCache);

        assertDoesNotThrow(() -> httpServerManager.loadServiceProviderCache());
        assertEquals(1, serviceProviderByNetworkIdCache.size());
        assertEquals(1, serviceProviderBySystemIdCache.size());
        assertEquals(1, systemIdByNetworkIdCache.size());
    }

    @Test
    void testLoadServiceProviderCacheWithInvalidJson() {
        when(appProperties.getServiceProvidersHashTable()).thenReturn("service_providers");
        Map<String, String> serviceProvidersData = new HashMap<>();
        serviceProvidersData.put("5", "{\"network_id\":1,\"name\":\"spHttp\",\"system_id\":\"spHttp\",\"password\":\"1234\",\"ip\":\"192.168.100.20\",\"port\":7001");
        when(jedisCluster.hgetAll("service_providers")).thenReturn(serviceProvidersData);

        serviceProviderByNetworkIdCache = new ConcurrentHashMap<>();
        serviceProviderBySystemIdCache = new ConcurrentHashMap<>();
        systemIdByNetworkIdCache = new ConcurrentHashMap<>();

        httpServerManager = new HttpServerManager(jedisCluster, appProperties, socketSession, systemIdByNetworkIdCache, serviceProviderBySystemIdCache, serviceProviderByNetworkIdCache);

        assertDoesNotThrow(() -> httpServerManager.loadServiceProviderCache());
        assertEquals(0, serviceProviderByNetworkIdCache.size());
        assertEquals(0, serviceProviderBySystemIdCache.size());
        assertEquals(0, systemIdByNetworkIdCache.size());
    }

    @Test
    void testLoadServiceProviderCacheThrowExceptionInForeach() {
        when(appProperties.getServiceProvidersHashTable()).thenReturn("service_providers");
        Map<String, String> serviceProvidersData = new HashMap<>();
        serviceProvidersData.put("5", HTTP_SERVICE_PROVIDER_JSON);
        serviceProvidersData.put("2", SMPP_SERVICE_PROVIDER_JSON);
        when(jedisCluster.hgetAll("service_providers")).thenReturn(serviceProvidersData);

        httpServerManager = new HttpServerManager(jedisCluster, appProperties, socketSession, systemIdByNetworkIdCache, serviceProviderBySystemIdCache, serviceProviderByNetworkIdCache);
        when(serviceProviderBySystemIdCache.put(anyString(), any())).thenThrow(new RuntimeException("Simulated exception"));

        assertDoesNotThrow(() -> httpServerManager.loadServiceProviderCache());
    }

    @Test
    void loadOrUpdateGeneralSettingsCache() {
        when(appProperties.getHttpGeneralSettingsHash()).thenReturn("general_settings");
        when(appProperties.getHttpGeneralSettingsKey()).thenReturn("smpp_http");
        when(jedisCluster.hget("general_settings", "smpp_http")).thenReturn(GENERAL_SETTINGS_JSON);

        httpServerManager.loadOrUpdateGeneralSettingsCache();
        assertEquals(GENERAL_SETTINGS_JSON, httpServerManager.getGeneralSettings().toString());
    }

    @Test
    void testManageServerHandler() {
        when(appProperties.getConfigurationHash()).thenReturn("configurations");
        when(jedisCluster.hget(appProperties.getConfigurationHash(), appProperties.getServerName())).thenReturn("{\"state\":\"STARTED\"}");
        assertDoesNotThrow(() -> httpServerManager.manageServerHandler());
        assertTrue(httpServerManager.isServerActive());
    }

    @Test
    void testManageServerHandlerGettingNull() {
        when(appProperties.getConfigurationHash()).thenReturn("configurations");
        when(jedisCluster.hget(appProperties.getConfigurationHash(), appProperties.getServerName())).thenReturn(null);
        assertDoesNotThrow(() -> httpServerManager.manageServerHandler());
        assertFalse(httpServerManager.isServerActive());
    }

    @Test
    void testUpdateServiceProviderGettingNull() {
        when(appProperties.getServiceProvidersHashTable()).thenReturn("service_providers");
        when(jedisCluster.hget("service_providers", "5")).thenReturn(null);
        when(jedisCluster.hget("service_providers", "2")).thenReturn(null);

        serviceProviderByNetworkIdCache = new ConcurrentHashMap<>();
        serviceProviderBySystemIdCache = new ConcurrentHashMap<>();
        systemIdByNetworkIdCache = new ConcurrentHashMap<>();

        httpServerManager = new HttpServerManager(jedisCluster, appProperties, socketSession, systemIdByNetworkIdCache, serviceProviderBySystemIdCache, serviceProviderByNetworkIdCache);

        httpServerManager.updateServiceProvider("5");
        assertEquals(0, serviceProviderByNetworkIdCache.size());
        assertEquals(0, serviceProviderBySystemIdCache.size());
        assertEquals(0, systemIdByNetworkIdCache.size());

        httpServerManager.updateServiceProvider("2");
        assertEquals(0, serviceProviderByNetworkIdCache.size());
        assertEquals(0, serviceProviderBySystemIdCache.size());
        assertEquals(0, systemIdByNetworkIdCache.size());
    }

    @Test
    void testUpdateServiceProviderGettingInvalidJson() {
        when(appProperties.getServiceProvidersHashTable()).thenReturn("service_providers");
        when(jedisCluster.hget("service_providers", "5")).thenReturn("{\"network_id\":1,\"name\":\"spHttp\",\"system_id\":\"spHttp\",\"password\":\"1234\",\"ip\":\"192.168.100.20\",\"port\":7001");
        when(jedisCluster.hget("service_providers", "2")).thenReturn(SMPP_SERVICE_PROVIDER_JSON);

        serviceProviderByNetworkIdCache = new ConcurrentHashMap<>();
        serviceProviderBySystemIdCache = new ConcurrentHashMap<>();
        systemIdByNetworkIdCache = new ConcurrentHashMap<>();

        httpServerManager = new HttpServerManager(jedisCluster, appProperties, socketSession, systemIdByNetworkIdCache, serviceProviderBySystemIdCache, serviceProviderByNetworkIdCache);

        httpServerManager.updateServiceProvider("5");
        assertEquals(0, serviceProviderByNetworkIdCache.size());
        assertEquals(0, serviceProviderBySystemIdCache.size());
        assertEquals(0, systemIdByNetworkIdCache.size());

        httpServerManager.updateServiceProvider("2");
        assertEquals(0, serviceProviderByNetworkIdCache.size());
        assertEquals(0, serviceProviderBySystemIdCache.size());
        assertEquals(0, systemIdByNetworkIdCache.size());
    }

    @Test
    void testUpdateServiceProvider() {
        when(appProperties.getServiceProvidersHashTable()).thenReturn("service_providers");
        when(jedisCluster.hget("service_providers", "5")).thenReturn(HTTP_SERVICE_PROVIDER_JSON);
        when(jedisCluster.hget("service_providers", "2")).thenReturn(SMPP_SERVICE_PROVIDER_JSON);
        when(jedisCluster.hget("service_providers", "4")).thenReturn(HTTP_SERVICE_PROVIDER_JSON_ENABLED_0);

        var mockStompSession = mock(StompSession.class);
        when(socketSession.getStompSession()).thenReturn(mockStompSession);
        serviceProviderByNetworkIdCache = new ConcurrentHashMap<>();
        serviceProviderBySystemIdCache = new ConcurrentHashMap<>();
        systemIdByNetworkIdCache = new ConcurrentHashMap<>();

        httpServerManager = new HttpServerManager(jedisCluster, appProperties, socketSession, systemIdByNetworkIdCache, serviceProviderBySystemIdCache, serviceProviderByNetworkIdCache);

        httpServerManager.updateServiceProvider("5");
        assertEquals(1, serviceProviderByNetworkIdCache.size());
        assertEquals(1, serviceProviderBySystemIdCache.size());
        assertEquals(1, systemIdByNetworkIdCache.size());

        httpServerManager.updateServiceProvider("2");
        assertEquals(1, serviceProviderByNetworkIdCache.size());
        assertEquals(1, serviceProviderBySystemIdCache.size());
        assertEquals(1, systemIdByNetworkIdCache.size());

        httpServerManager.updateServiceProvider("4");
        assertEquals(2, serviceProviderByNetworkIdCache.size());
        assertEquals(2, serviceProviderBySystemIdCache.size());
        assertEquals(2, systemIdByNetworkIdCache.size());
    }
}