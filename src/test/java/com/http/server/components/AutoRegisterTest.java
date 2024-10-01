package com.http.server.components;

import com.http.server.utils.AppProperties;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import redis.clients.jedis.JedisCluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutoRegisterTest {

    @Mock
    private AppProperties appProperties;

    @Mock
    private JedisCluster mockJedisCluster;

    @InjectMocks
    private AutoRegister autoRegister;

    @Test
    @Order(1)
    void createInstance() {
        mockAppProperties("null");
        String result = autoRegister.createInstance("null");
        String expectedInstance = createExpectedInstance("null");
        assertEquals(expectedInstance, result);
    }

    @Test
    @Order(2)
    void register() {
        mockAppProperties("null");
        Long expectedResponse = 1L;
        when(mockJedisCluster.hset(anyString(), anyString(), anyString())).thenReturn(expectedResponse);
        autoRegister.register();
        String expectedInstance = createExpectedInstance("null");
        verify(mockJedisCluster).hset("null", "null", expectedInstance);
    }

    @Test
    @Order(3)
    void createInstance_shouldFail() {
        mockAppProperties("initialStatus");
        String result = autoRegister.createInstance("testState");
        String expectedInstance = createExpectedInstance("initialStatus");
        assertNotEquals(expectedInstance, result);
    }

    @Test
    @Order(4)
    void testUnregister() {
        String hashName = "configurations";
        String instanceName = "http_server";
        when(appProperties.getConfigurationHash()).thenReturn(hashName);
        when(appProperties.getInstanceName()).thenReturn(instanceName);

        autoRegister.unregister();
        verify(mockJedisCluster).hdel(hashName, instanceName);
    }

    private void mockAppProperties(String instanceState) {
        when(appProperties.getConfigurationHash()).thenReturn("null");
        when(appProperties.getInstanceName()).thenReturn("null");
        when(appProperties.getInstanceIp()).thenReturn("null");
        when(appProperties.getInstancePort()).thenReturn("null");
        when(appProperties.getInstanceProtocol()).thenReturn("null");
        when(appProperties.getInstanceScheme()).thenReturn("null");
        when(appProperties.getInstanceApiKey()).thenReturn("null");
        when(appProperties.getInstanceInitialStatus()).thenReturn(instanceState);
    }

    private String createExpectedInstance(String state) {
        return String.format("{\"name\":\"%s\",\"ip\":\"%s\",\"port\":\"%s\",\"protocol\":\"%s\",\"scheme\":\"%s\",\"apiKey\":\"%s\",\"state\":\"%s\"}",
                appProperties.getInstanceName(),
                appProperties.getInstanceIp(),
                appProperties.getInstancePort(),
                appProperties.getInstanceProtocol(),
                appProperties.getInstanceScheme(),
                appProperties.getInstanceApiKey(),
                state);
    }
}
