package com.http.server.components;

import com.http.server.http.HttpServerManager;
import com.paicbd.smsc.dto.ServiceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreditHandlerTest {

    @Mock
    private HttpServerManager httpServerManager;

    @InjectMocks
    private CreditHandler creditHandler;

    private ConcurrentMap<String, ServiceProvider> serviceProviderCache;

    @BeforeEach
    void setUp() {
        serviceProviderCache = new ConcurrentHashMap<>();
        ServiceProvider sp1 = new ServiceProvider();
        sp1.setHasAvailableCredit(true);
        ServiceProvider sp2 = new ServiceProvider();
        sp2.setHasAvailableCredit(false);

        serviceProviderCache.put("systemId1", sp1);
        serviceProviderCache.put("systemId2", sp2);

        when(httpServerManager.getServiceProviderCache()).thenReturn(serviceProviderCache);

        when(httpServerManager.getServiceProvider(anyString())).thenAnswer(invocation -> {
            String systemId = invocation.getArgument(0);
            return serviceProviderCache.get(systemId);
        });
    }

    @Test
    void init_hasAvailableCreditInitialized() {
        creditHandler.init();

        assertTrue(creditHandler.hasCredit("systemId1"));
        assertFalse(creditHandler.hasCredit("systemId2"));
    }

    @Test
    void hasCredit_systemIdExists() {
        assertTrue(creditHandler.hasCredit("systemId1"));
    }

    @Test
    void hasCredit_systemIdDoesNotExist() {
        assertFalse(creditHandler.hasCredit("nonExistentSystemId"));
    }

    @Test
    void removeFromRedisAndCache_systemIdExists() {
        String systemIdToRemove = "systemId1";
        assertTrue(creditHandler.hasCredit(systemIdToRemove));
        creditHandler.removeFromRedisAndCache(systemIdToRemove);
        assertTrue(creditHandler.hasCredit(systemIdToRemove));
    }

    @Test
    void hasCredit_throwsException() {
        String systemId = "systemId1";

        CreditHandler spyCreditHandler = spy(creditHandler);
        doThrow(new RuntimeException("Forced exception")).when(spyCreditHandler).getCreditConfigured(systemId);

        Exception exception = assertThrows(Exception.class, () -> {
            spyCreditHandler.hasCredit(systemId);
        });

        assertTrue(exception.getMessage().contains("Forced exception"));
    }
}
