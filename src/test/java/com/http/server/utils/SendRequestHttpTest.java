package com.http.server.utils;

import com.http.server.dto.GlobalRecords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SendRequestHttpTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SendRequestHttp sendRequestHttp;

    @BeforeEach
    void setUp() {
        sendRequestHttp = new SendRequestHttp();
    }

    @Test
    void testSendPostRequest_SuccessStatusCode() {
        String expectedUrl = "http://18.224.164.85:3000/api/callback";
        String deliverSm = "{\"message_id\":\"messageId\",\"source_addr_ton\":1,\"source_addr_npi\":1,\"source_addr\":\"sourceAddr\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"destinationAddr\",\"esm_class\":1,\"data_coding\":1,\"short_message\":\"shortMessage\",\"status\":\"status\",\"error_code\":\"errorCode\",\"optional_parameters\":[{\"tag\":1,\"value\":\"value\"}]}";
        HttpEntity<?> requestEntity = new HttpEntity<>(deliverSm);
        ResponseEntity<String> mockResponse = new ResponseEntity<>("Success", HttpStatus.OK);
        when(restTemplate.postForEntity(expectedUrl, requestEntity, String.class)).thenReturn(mockResponse);
        sendRequestHttp = new SendRequestHttp();
        GlobalRecords.DeliverSmResponse response = sendRequestHttp.sendPostRequest(expectedUrl, requestEntity);
        assertEquals(true, response.status());
    }

    @Test
    void testSendPostRequest_FailureStatusCode() {
        String expectedUrl = "https://example.com/api";
        String deliverSm = "{\"message_id\":\"messageId\",\"source_addr_ton\":1,\"source_addr_npi\":1,\"source_addr\":\"sourceAddr\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"destinationAddr\",\"esm_class\":1,\"data_coding\":1,\"short_message\":\"shortMessage\",\"status\":\"status\",\"error_code\":\"errorCode\",\"optional_parameters\":[{\"tag\":1,\"value\":\"value\"}]}";
        HttpEntity<?> requestEntity = new HttpEntity<>(deliverSm);
        ResponseEntity<String> mockResponse = new ResponseEntity<>("Failure", HttpStatus.BAD_REQUEST);
        when(restTemplate.postForEntity(expectedUrl, requestEntity, String.class)).thenReturn(mockResponse);
        GlobalRecords.DeliverSmResponse response = sendRequestHttp.sendPostRequest(expectedUrl, requestEntity);
        assertEquals(false, response.status());
    }

    @Test
    void testSendPostRequest_Exception() {
        String expectedUrl = "https://example.com/api";
        String deliverSm = "{\"message_id\":\"messageId\",\"source_addr_ton\":1,\"source_addr_npi\":1,\"source_addr\":\"sourceAddr\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"destinationAddr\",\"esm_class\":1,\"data_coding\":1,\"short_message\":\"shortMessage\",\"status\":\"status\",\"error_code\":\"errorCode\",\"optional_parameters\":[{\"tag\":1,\"value\":\"value\"}]}";
        HttpEntity<?> requestEntity = new HttpEntity<>(deliverSm);
        when(restTemplate.postForEntity(expectedUrl, requestEntity, String.class)).thenThrow(new RuntimeException("Exception"));
        GlobalRecords.DeliverSmResponse response = sendRequestHttp.sendPostRequest(expectedUrl, requestEntity);
        assertEquals(false, response.status());
    }
}
