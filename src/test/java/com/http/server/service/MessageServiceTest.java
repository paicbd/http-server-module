package com.http.server.service;

import com.http.server.dto.GlobalRecords;
import com.http.server.http.HttpServerManager;
import com.http.server.utils.AppProperties;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.GeneralSettings;
import com.paicbd.smsc.dto.ServiceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.JedisCluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
    @Mock
    JedisCluster jedisCluster;
    @Mock
    CdrProcessor cdrProcessor;
    @Mock
    AppProperties appProperties;
    @Mock
    HttpServerManager httpServerManager;
    @Mock
    ConcurrentMap<String, ServiceProvider> serviceProviderBySystemIdCache;

    @InjectMocks
    MessageService messageService;

    @Test
    void init() {
        assertDoesNotThrow(() -> messageService.init());
    }

    @Test
    void onMessageReceive_InvalidDataCoding() {
        GlobalRecords.MessageRequest messageRequest = buildMessageRequest(18, "11901910");
        GlobalRecords.SubmitResponse response = messageService.onMessageReceive(messageRequest);
        assertEquals("Invalid data coding", response.errorMessage());
        assertNull(response.messageId());
        assertNull(response.systemId());
    }

    @Test
    void onMessageReceive_SpNotStarted() {
        GlobalRecords.MessageRequest messageRequest = buildMessageRequest(0, "11901910");
        ConcurrentMap<String, ServiceProvider> spc = new ConcurrentHashMap<>();
        spc.computeIfAbsent("systemId", k -> new ServiceProvider()).setEnabled(0);
        messageService = new MessageService(jedisCluster, cdrProcessor, appProperties, httpServerManager, spc);
        GlobalRecords.SubmitResponse response = messageService.onMessageReceive(messageRequest);
        assertEquals("Service not found", response.errorMessage());
        assertNull(response.messageId());
        assertNull(response.systemId());
    }

    @Test
    void onMessageReceive_SpStarted_WithoutCredit() {
        GlobalRecords.MessageRequest messageRequest = buildMessageRequest(0, "11901910");
        ConcurrentMap<String, ServiceProvider> spc = new ConcurrentHashMap<>();
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setEnabled(1);
        serviceProvider.setCredit(0);
        serviceProvider.setHasAvailableCredit(false);
        spc.putIfAbsent("systemId", serviceProvider);
        messageService = new MessageService(jedisCluster, cdrProcessor, appProperties, httpServerManager, spc);
        GlobalRecords.SubmitResponse response = messageService.onMessageReceive(messageRequest);
        assertNotNull(response.errorMessage());
        assertNull(response.messageId());
        assertNull(response.systemId());
    }

    @Test
    void onMessageReceive_SpStarted_WithCredit_DataCodingNull() {
        GlobalRecords.MessageRequest messageRequest = buildMessageRequest(null, "11901910");
        ConcurrentMap<String, ServiceProvider> spc = new ConcurrentHashMap<>();
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setEnabled(1);
        serviceProvider.setCredit(1);
        serviceProvider.setHasAvailableCredit(true);
        spc.putIfAbsent("systemId", serviceProvider);
        messageService = new MessageService(jedisCluster, cdrProcessor, appProperties, httpServerManager, spc);
        when(httpServerManager.getGeneralSettings()).thenReturn(buildGeneralSettings());
        GlobalRecords.SubmitResponse response = messageService.onMessageReceive(messageRequest);
        assertNotNull(response.errorMessage());
        assertNotNull(response.messageId());
        assertNotNull(response.systemId());
    }

    @Test
    void onMessageReceive_SpStarted_WithCredit_DataCodingNonNull_DestinationList() {
        GlobalRecords.MessageRequest messageRequest = buildMessageRequest(null, List.of("11901910", "11901911"));
        ConcurrentMap<String, ServiceProvider> spc = new ConcurrentHashMap<>();
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setEnabled(1);
        serviceProvider.setCredit(1);
        serviceProvider.setHasAvailableCredit(true);
        spc.putIfAbsent("systemId", serviceProvider);
        messageService = new MessageService(jedisCluster, cdrProcessor, appProperties, httpServerManager, spc);
        when(httpServerManager.getGeneralSettings()).thenReturn(buildGeneralSettings());
        GlobalRecords.SubmitResponse response = messageService.onMessageReceive(messageRequest);
        assertNotNull(response.errorMessage());
        assertNotNull(response.messageId());
        assertNotNull(response.systemId());
    }

    @Test
    void onMessageReceive_SpStarted_WithCredit_DataCodingNonNull_InvalidDestinationType() {
        GlobalRecords.MessageRequest messageRequest = buildMessageRequest(null, new Object());
        ConcurrentMap<String, ServiceProvider> spc = new ConcurrentHashMap<>();
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setEnabled(1);
        serviceProvider.setCredit(1);
        serviceProvider.setHasAvailableCredit(true);
        spc.putIfAbsent("systemId", serviceProvider);
        messageService = new MessageService(jedisCluster, cdrProcessor, appProperties, httpServerManager, spc);
        when(httpServerManager.getGeneralSettings()).thenReturn(buildGeneralSettings());
        GlobalRecords.SubmitResponse response = messageService.onMessageReceive(messageRequest);
        assertNotNull(response.errorMessage());
        assertNull(response.messageId());
        assertNull(response.systemId());
    }

    @Test
    void onMessageReceive_SpStarted_WithCredit_DataCodingNonNull_DestinationListMax() {
        GlobalRecords.MessageRequest messageRequest = buildMessageRequest(0, buildDestinationList());
        ConcurrentMap<String, ServiceProvider> spc = new ConcurrentHashMap<>();
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setEnabled(1);
        serviceProvider.setCredit(1);
        serviceProvider.setHasAvailableCredit(true);
        spc.putIfAbsent("systemId", serviceProvider);
        messageService = new MessageService(jedisCluster, cdrProcessor, appProperties, httpServerManager, spc);
        when(httpServerManager.getGeneralSettings()).thenReturn(buildGeneralSettings());
        GlobalRecords.SubmitResponse response = messageService.onMessageReceive(messageRequest);
        assertNotNull(response.errorMessage());
        assertNull(response.messageId());
        assertNull(response.systemId());
    }

    @Test
    void onMessageReceive_ThrowException() {
        GlobalRecords.MessageRequest messageRequest = buildMessageRequest(0, "11901910");
        assertDoesNotThrow(() -> messageService.onMessageReceive(messageRequest));
    }

    static GlobalRecords.MessageRequest buildMessageRequest(Integer dataCoding, Object destinationAddress) {
        return new GlobalRecords.MessageRequest(
            "systemId",
            1,
            1,
            "12345678",
            1,
            1,
            destinationAddress,
            1,
            300,
            1,
            dataCoding,
            1,
            "shortMessage",
            null,
            null
        );
    }

    static GeneralSettings buildGeneralSettings() {
        GeneralSettings generalSettings = new GeneralSettings();
        generalSettings.setEncodingGsm7(0);
        generalSettings.setDestAddrNpi(1);
        generalSettings.setDestAddrTon(1);
        generalSettings.setSourceAddrNpi(1);
        generalSettings.setSourceAddrTon(1);
        generalSettings.setEncodingIso88591(3);
        generalSettings.setEncodingUcs2(2);
        generalSettings.setValidityPeriod(120);
        generalSettings.setMaxValidityPeriod(240);
        generalSettings.setId(1);
        return generalSettings;
    }

    static List<String> buildDestinationList() {
        Random random = new Random();
        int size = 10002;
        List<String> destinationList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            destinationList.add(String.valueOf(random.nextInt(10000)));
        }
        return destinationList;
    }
}