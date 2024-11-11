package com.http.server.components;

import com.http.server.utils.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.JedisCluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AutoRegisterTest {
    @Mock(strictness = Mock.Strictness.LENIENT)
    private AppProperties appProperties;
    @Mock
    private JedisCluster jedisCluster;

    @InjectMocks
    private AutoRegister autoRegister;

    @BeforeEach
    public void setUp() {
        when(appProperties.getConfigurationHash()).thenReturn("testConfigHash");
        when(appProperties.getInstanceName()).thenReturn("testInstanceName");
        when(appProperties.getInstanceIp()).thenReturn("127.0.0.1");
        when(appProperties.getInstancePort()).thenReturn("8080");
        when(appProperties.getInstanceProtocol()).thenReturn("HTTP");
        when(appProperties.getInstanceScheme()).thenReturn("http");
        when(appProperties.getInstanceApiKey()).thenReturn("testApiKey");
        when(appProperties.getInstanceInitialStatus()).thenReturn("STARTING");
    }

    @Test
    void testRegister() {
        autoRegister.register();

        String expectedInstance = "{\"name\":\"testInstanceName\",\"ip\":\"127.0.0.1\",\"port\":\"8080\",\"protocol\":\"HTTP\",\"scheme\":\"http\",\"apiKey\":\"testApiKey\",\"state\":\"STARTING\"}";
        verify(jedisCluster).hset("testConfigHash", "testInstanceName", expectedInstance);
    }

    @Test
    void testCreateInstanceWithEmptyState() {
        String result = autoRegister.createInstance("");

        String expected = "{\"name\":\"testInstanceName\",\"ip\":\"127.0.0.1\",\"port\":\"8080\",\"protocol\":\"HTTP\",\"scheme\":\"http\",\"apiKey\":\"testApiKey\",\"state\":\"STARTING\"}";
        assertEquals(expected, result);
    }

    @Test
    void testCreateInstanceWithNonEmptyState() {
        String result = autoRegister.createInstance("RUNNING");

        String expected = "{\"name\":\"testInstanceName\",\"ip\":\"127.0.0.1\",\"port\":\"8080\",\"protocol\":\"HTTP\",\"scheme\":\"http\",\"apiKey\":\"testApiKey\",\"state\":\"RUNNING\"}";
        assertEquals(expected, result);
    }

    @Test
    void testUnregister() {
        autoRegister.unregister();

        String expectedInstance = "{\"name\":\"testInstanceName\",\"ip\":\"127.0.0.1\",\"port\":\"8080\",\"protocol\":\"HTTP\",\"scheme\":\"http\",\"apiKey\":\"testApiKey\",\"state\":\"STOPPED\"}";
        verify(jedisCluster).hset("testConfigHash", "testInstanceName", expectedInstance);
        verify(jedisCluster).hdel("testConfigHash", "testInstanceName");
    }
}