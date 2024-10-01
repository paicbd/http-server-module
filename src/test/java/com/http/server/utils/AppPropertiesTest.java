package com.http.server.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@MockitoSettings(strictness = Strictness.LENIENT)
class AppPropertiesTest {

    @InjectMocks
    private AppProperties appProperties;

    @BeforeEach
    void setUp() throws Exception {
        injectField("redisNodes", Arrays.asList("192.168.100.1:6379", "192.168.100.2:6379", "192.168.100.3:6379"));
        injectField("redisMaxTotal", 20);
        injectField("redisMaxIdle", 20);
        injectField("redisMinIdle", 1);
        injectField("redisBlockWhenExhausted", true);
        injectField("deliverSmQueue", "http_dlr");
        injectField("workers", 5);
        injectField("batchSizePerWorker", 1);
        injectField("wsHost", "192.168.100.0");
        injectField("wsPort", 8080);
        injectField("wsPath", "/ws");
        injectField("wsEnabled", false);
        injectField("wsHeaderName", "Authorization");
        injectField("wsRetryInterval", 10);
        injectField("wsHeaderValue", "Bearer Token");
        injectField("serviceProvidersHashTable", "service_providers");
        injectField("serviceProvidersHashName", "service_providers");
        injectField("instanceName", "http_server");
        injectField("configurationHash", "configurations");
        injectField("serverName", "http-server-instance-01");
        injectField("instanceIp", "127.0.0.1");
        injectField("instancePort", "8080");
        injectField("instanceInitialStatus", "STOPPED");
        injectField("instanceProtocol", "HTTP");
        injectField("instanceScheme", "http");
        injectField("instanceApiKey", "api_key_123");
        injectField("httpGeneralSettingsHash", "general_settings");
        injectField("httpGeneralSettingsKey", "smpp_http");
        injectField("preMessageList", "preMessage");
        injectField("prefixQueueDeliver", "http_dlr");
    }

    @Test
    void testAppProperties_1() {
        assertEquals(Arrays.asList("192.168.100.1:6379", "192.168.100.2:6379", "192.168.100.3:6379"), appProperties.getRedisNodes());
        assertEquals(20, appProperties.getRedisMaxTotal());
        assertEquals(20, appProperties.getRedisMaxIdle());
        assertEquals(1, appProperties.getRedisMinIdle());
        assertTrue(appProperties.isRedisBlockWhenExhausted());
        assertEquals("http_dlr", appProperties.getDeliverSmQueue());
        assertEquals(5, appProperties.getWorkers());
        assertEquals(1, appProperties.getBatchSizePerWorker());
        assertEquals("192.168.100.0", appProperties.getWsHost());
        assertEquals(8080, appProperties.getWsPort());
        assertEquals("/ws", appProperties.getWsPath());
        assertFalse(appProperties.isWsEnabled());
        assertEquals("Authorization", appProperties.getWsHeaderName());
        assertEquals("Bearer Token", appProperties.getWsHeaderValue());
        assertEquals(10, appProperties.getWsRetryInterval());
    }

    @Test
    void testAppProperties_2() {
        assertEquals("service_providers", appProperties.getServiceProvidersHashTable());
        assertEquals("service_providers", appProperties.getServiceProvidersHashName());
        assertEquals("http_server", appProperties.getInstanceName());
        assertEquals("configurations", appProperties.getConfigurationHash());
        assertEquals("http-server-instance-01", appProperties.getServerName());
        assertEquals("127.0.0.1", appProperties.getInstanceIp());
        assertEquals("8080", appProperties.getInstancePort());
        assertEquals("STOPPED", appProperties.getInstanceInitialStatus());
        assertEquals("HTTP", appProperties.getInstanceProtocol());
        assertEquals("http", appProperties.getInstanceScheme());
        assertEquals("api_key_123", appProperties.getInstanceApiKey());
        assertEquals("general_settings", appProperties.getHttpGeneralSettingsHash());
        assertEquals("smpp_http", appProperties.getHttpGeneralSettingsKey());
        assertEquals("preMessage", appProperties.getPreMessageList());
        assertEquals("http_dlr", appProperties.getPrefixQueueDeliver());
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = AppProperties.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(appProperties, value);
    }
}
