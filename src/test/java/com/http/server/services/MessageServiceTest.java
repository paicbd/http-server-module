package com.http.server.services;

import com.http.server.components.CreditHandler;
import com.http.server.dto.GlobalRecords;
import com.http.server.http.HttpServerManager;
import com.http.server.utils.AppProperties;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.GeneralSettings;
import com.paicbd.smsc.dto.ServiceProvider;
import com.paicbd.smsc.utils.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.JedisCluster;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
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
    CreditHandler handlerCreditBalance;

    @Mock
    List<String> listOfDestinations = new ArrayList<>();

    @InjectMocks
    MessageService messageService;


    @Test
    void messageInvalidDataCoding() {
        var messageRequest = getMessageRequest("777777", 15);
        var response = messageService.message(messageRequest);
        assertNotNull(response);
        assertEquals("Invalid data coding", response.errorMessage());
    }

    @Test
    void messageServiceNotFound() {
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setEnabled(0);
        when(httpServerManager.getServiceProvider(anyString())).thenReturn(serviceProvider);
        var messageRequest = getMessageRequest("777777", 0);
        var response = messageService.message(messageRequest);
        assertNotNull(response);
        assertEquals("Service not found", response.errorMessage());
    }

    @Test
    void messageServiceNotCredit() {
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setEnabled(1);
        when(httpServerManager.getServiceProvider(anyString())).thenReturn(serviceProvider);
        when(handlerCreditBalance.hasCredit(anyString())).thenReturn(false);
        var messageRequest = getMessageRequest("777777", 0);
        var response = messageService.message(messageRequest);
        assertNotNull(response);
        assertEquals("Service provider don't have enough credits", response.errorMessage());
    }

    @Test
    void messageServiceErrorType() {
        when(handlerCreditBalance.hasCredit(anyString())).thenReturn(true);
        String spString = "{\"name\":\"httpsp\",\"password\":\"1234\",\"tps\":1,\"validity\":0,\"network_id\":4,\"system_id\":\"httpsp\",\"system_type\":\"\",\"interface_version\":\"IF_50\",\"address_ton\":0,\"address_npi\":0,\"address_range\":\"^[0-9a-zA-Z]*$\",\"enabled\":1,\"enquire_link_period\":3000,\"pdu_timeout\":5000,\"request_dlr\":false,\"status\":\"STOPPED\",\"max_binds\":1,\"current_binds_count\":0,\"credit\":0,\"binds\":[],\"credit_used\":0,\"is_prepaid\":true,\"has_available_credit\":false,\"protocol\":\"HTTP\",\"contact_name\":\"Obed Navarrete\",\"email\":\"administrator@company.com\",\"phone_number\":\"85585858\",\"callback_url\":\"http://192.168.100.20:300/call\",\"authentication_types\":\"Undefined\",\"header_security_name\":\"\",\"token\":\"Undefined \",\"callback_headers_http\":[]}";
        ServiceProvider serviceProvider = Converter.stringToObject(spString, ServiceProvider.class);
        var generalSettings = new GeneralSettings();
        generalSettings.setEncodingGsm7(0);
        when(httpServerManager.getGeneralSettings()).thenReturn(generalSettings);
        when(httpServerManager.getServiceProvider(anyString())).thenReturn(serviceProvider);
        serviceProvider.setEnabled(1);
        when(httpServerManager.getServiceProvider(anyString())).thenReturn(serviceProvider);
        var messageRequest = getMessageRequest(123456, null);
        var response = messageService.message(messageRequest);
        assertNotNull(response);
        assertEquals("Bad type format of destination", response.errorMessage());
    }

    @Test
    void messageServiceException() {
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setEnabled(1);
        when(httpServerManager.getServiceProvider(anyString())).thenReturn(serviceProvider);
        when(handlerCreditBalance.hasCredit(anyString())).thenReturn(true);
        when(httpServerManager.getGeneralSettings()).thenReturn(null);
        var messageRequest = getMessageRequest("777777", null);
        var response = messageService.message(messageRequest);
        assertNotNull(response);
    }

    @Test
    void messageServiceSingleDestination() {
        when(handlerCreditBalance.hasCredit(anyString())).thenReturn(true);
        String spString = "{\"name\":\"httpsp\",\"password\":\"1234\",\"tps\":1,\"validity\":0,\"network_id\":4,\"system_id\":\"httpsp\",\"system_type\":\"\",\"interface_version\":\"IF_50\",\"address_ton\":0,\"address_npi\":0,\"address_range\":\"^[0-9a-zA-Z]*$\",\"enabled\":0,\"enquire_link_period\":3000,\"pdu_timeout\":5000,\"request_dlr\":false,\"status\":\"STOPPED\",\"max_binds\":1,\"current_binds_count\":0,\"credit\":0,\"binds\":[],\"credit_used\":0,\"is_prepaid\":true,\"has_available_credit\":false,\"protocol\":\"HTTP\",\"contact_name\":\"Obed Navarrete\",\"email\":\"administrator@company.com\",\"phone_number\":\"85585858\",\"callback_url\":\"http://192.168.100.20:300/call\",\"authentication_types\":\"Undefined\",\"header_security_name\":\"\",\"token\":\"Undefined \",\"callback_headers_http\":[]}";
        ServiceProvider serviceProvider = Converter.stringToObject(spString, ServiceProvider.class);
        serviceProvider.setEnabled(1);
        when(httpServerManager.getServiceProvider(anyString())).thenReturn(serviceProvider);
        when(httpServerManager.getServiceProvider(anyString())).thenReturn(serviceProvider);
        var messageRequest = getMessageRequest("777777", 0);
        var response = messageService.message(messageRequest);
        assertNotNull(response);
    }

    @Test
    void messageServiceMultiDestinationMaxDestination() {
        when(handlerCreditBalance.hasCredit(anyString())).thenReturn(true);
        String spString = "{\"name\":\"httpsp\",\"password\":\"1234\",\"tps\":1,\"validity\":0,\"network_id\":4,\"system_id\":\"httpsp\",\"system_type\":\"\",\"interface_version\":\"IF_50\",\"address_ton\":0,\"address_npi\":0,\"address_range\":\"^[0-9a-zA-Z]*$\",\"enabled\":0,\"enquire_link_period\":3000,\"pdu_timeout\":5000,\"request_dlr\":false,\"status\":\"STOPPED\",\"max_binds\":1,\"current_binds_count\":0,\"credit\":0,\"binds\":[],\"credit_used\":0,\"is_prepaid\":true,\"has_available_credit\":false,\"protocol\":\"HTTP\",\"contact_name\":\"Obed Navarrete\",\"email\":\"administrator@company.com\",\"phone_number\":\"85585858\",\"callback_url\":\"http://192.168.100.20:300/call\",\"authentication_types\":\"Undefined\",\"header_security_name\":\"\",\"token\":\"Undefined \",\"callback_headers_http\":[]}";
        ServiceProvider serviceProvider = Converter.stringToObject(spString, ServiceProvider.class);
        var generalSettings = new GeneralSettings();
        generalSettings.setEncodingGsm7(0);
        when(httpServerManager.getGeneralSettings()).thenReturn(generalSettings);
        serviceProvider.setEnabled(1);
        when(httpServerManager.getServiceProvider(anyString())).thenReturn(serviceProvider);
        when(listOfDestinations.size()).thenReturn(11000);
        var messageRequest = getMessageRequest(listOfDestinations, null);
        var response = messageService.message(messageRequest);
        assertEquals("The maximum number of recipients has been exceeded", response.errorMessage());
        assertNotNull(response);
    }

    @Test
    void messageServiceMultiDestination() {
        when(handlerCreditBalance.hasCredit(anyString())).thenReturn(true);
        String spString = "{\"name\":\"httpsp\",\"password\":\"1234\",\"tps\":1,\"validity\":0,\"network_id\":4,\"system_id\":\"httpsp\",\"system_type\":\"\",\"interface_version\":\"IF_50\",\"address_ton\":0,\"address_npi\":0,\"address_range\":\"^[0-9a-zA-Z]*$\",\"enabled\":0,\"enquire_link_period\":3000,\"pdu_timeout\":5000,\"request_dlr\":false,\"status\":\"STOPPED\",\"max_binds\":1,\"current_binds_count\":0,\"credit\":0,\"binds\":[],\"credit_used\":0,\"is_prepaid\":true,\"has_available_credit\":false,\"protocol\":\"HTTP\",\"contact_name\":\"Obed Navarrete\",\"email\":\"administrator@company.com\",\"phone_number\":\"85585858\",\"callback_url\":\"http://192.168.100.20:300/call\",\"authentication_types\":\"Undefined\",\"header_security_name\":\"\",\"token\":\"Undefined \",\"callback_headers_http\":[]}";
        ServiceProvider serviceProvider = Converter.stringToObject(spString, ServiceProvider.class);
        var generalSettings = new GeneralSettings();
        generalSettings.setEncodingGsm7(0);
        serviceProvider.setEnabled(1);
        when(httpServerManager.getGeneralSettings()).thenReturn(generalSettings);
        when(httpServerManager.getServiceProvider(anyString())).thenReturn(serviceProvider);
        var messageRequest = getMessageRequest(List.of("777777","787878"), null);
        var response = messageService.message(messageRequest);
        assertNotNull(response);
    }

    private GlobalRecords.MessageRequest getMessageRequest(Object destinationAddress, Integer dataCoding) {
        return new GlobalRecords.MessageRequest(
            "httpSp", 1,4,"8888888",1,4,destinationAddress,
                3,"",1, dataCoding, 0,
                "",null
        );
    }
}
