package com.http.server.components;

import com.fasterxml.jackson.core.type.TypeReference;
import com.http.server.http.HttpServerManager;
import com.http.server.utils.AppProperties;
import com.paicbd.smsc.dto.ServiceProvider;
import com.paicbd.smsc.dto.GeneralSettings;
import com.paicbd.smsc.dto.UtilsRecords;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import redis.clients.jedis.JedisCluster;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.util.AssertionErrors.assertEquals;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeliverSmQueueConsumerTest {

    @Mock
    private JedisCluster jedisCluster;

    @Mock
    private AppProperties appProperties;

    @Mock
    private SocketSession socketSession;

    @Mock
    private AutoRegister autoRegister;

    @Mock
    private HttpServerManager httpServerManager;

    @InjectMocks
    private DeliverSmQueueConsumer deliverSmQueueConsumer;

    @BeforeEach
    void setUp() {
        Map<String, ServiceProvider> serviceProvidersData = new HashMap<>();
        Map<Integer, ServiceProvider> serviceProviderByNetworkIdCache = new HashMap<>();
        Map<String, GeneralSettings> generalSettingsData = new HashMap<>();
        when(appProperties.getServiceProvidersHashTable()).thenReturn("service_providers");
        when(appProperties.getHttpGeneralSettingsHash()).thenReturn("general_settings");
        when(appProperties.getConfigurationHash()).thenReturn("configurations");
        when(appProperties.getServerName()).thenReturn("http-server-instance-01");

        String serviceProviderJson = "{\"network_id\":1,\"name\":\"spHttp\",\"system_id\":\"spHttp\",\"password\":\"1234\",\"ip\":\"192.168.100.20\",\"port\":7001,\"bind_type\":\"TRANSCEIVER\",\"system_type\":\"\",\"interface_version\":\"IF_50\",\"sessions_number\":10,\"address_ton\":0,\"address_npi\":0,\"address_range\":\"500\",\"tps\":10,\"status\":\"STOPPED\",\"enabled\":0,\"enquire_link_period\":30000,\"enquire_link_timeout\":0,\"request_dlr\":true,\"no_retry_error_code\":\"\",\"retry_alternate_destination_error_code\":\"\",\"bind_timeout\":5000,\"bind_retry_period\":10000,\"pdu_timeout\":5000,\"pdu_degree\":1,\"thread_pool_size\":100,\"mno_id\":1,\"tlv_message_receipt_id\":false,\"message_id_decimal_format\":false,\"active_sessions_numbers\":0,\"protocol\":\"SMPP\",\"auto_retry_error_code\":\"\",\"encoding_iso88591\":3,\"encoding_gsm7\":0,\"encoding_ucs2\":2,\"split_message\":false,\"split_smpp_type\":\"TLV\", \"callback_url\":\"http://18.224.164.85:3000/api/callback\", \"authentication_types\":\"Undefined\", \"user_name\":\"admin\", \"password\":\"admin\"}";
        serviceProvidersData.put("spHttp", Converter.stringToObject(serviceProviderJson, new TypeReference<>() {}));
        when(jedisCluster.hget("service_providers", "spHttp")).thenReturn(serviceProviderJson);

        String generalSettingJson = "{\"id\":1,\"validity_period\":60,\"max_validity_period\":240,\"source_addr_ton\":1,\"source_addr_npi\":1,\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"encoding_iso88591\":3,\"encoding_gsm7\":0,\"encoding_ucs2\":2}\n";
        generalSettingsData.put("1", Converter.stringToObject(generalSettingJson, new TypeReference<>() {}));
        when(jedisCluster.hget("general_settings", "1")).thenReturn(generalSettingJson);

        String generalSettingManager = "{\"state\":\"STARTED\"}";
        when(jedisCluster.hget("configurations", "http-server-instance-01")).thenReturn(generalSettingManager);

        serviceProviderByNetworkIdCache.put(1, Converter.stringToObject(serviceProviderJson, new TypeReference<>() {}));

        httpServerManager = new HttpServerManager(jedisCluster, appProperties, socketSession, autoRegister);

        httpServerManager.getServiceProviderCache().put("spHttp", Converter.stringToObject(serviceProviderJson, new TypeReference<>() {}));
    }

    @Test
    void startScheduler() {
        when(appProperties.getDeliverSmQueue()).thenReturn("deliverSmQueue");
        when(jedisCluster.llen("deliverSmQueue")).thenReturn(10L);
        when(appProperties.getWorkers()).thenReturn(5);

        deliverSmQueueConsumer.startScheduler();

        verify(jedisCluster, times(1)).llen("deliverSmQueue");
    }

    @Test
    void processingBatch_shouldProcessDeliverSmItems() {
        String queueName = "deliverSmQueue";
        List<String> deliverSmItems = Arrays.asList(createMessageEventJson("1"), createMessageEventJson("2"));

        when(appProperties.getDeliverSmQueue()).thenReturn(queueName);
        when(appProperties.getBatchSizePerWorker()).thenReturn(10);
        when(jedisCluster.lpop(queueName, 10)).thenReturn(deliverSmItems);

        Runnable task = deliverSmQueueConsumer.processingBatch();
        task.run();

        verify(jedisCluster, times(1)).lpop(queueName, 10);
    }

    @Test
    void processingBatch_shouldHandleException() {
        String queueName = "deliverSmQueue";

        when(appProperties.getDeliverSmQueue()).thenReturn(queueName);
        when(appProperties.getBatchSizePerWorker()).thenReturn(10);
        when(jedisCluster.lpop(queueName, 10)).thenThrow(new RuntimeException("Simulated exception"));

        Runnable task = deliverSmQueueConsumer.processingBatch();
        task.run();

        verify(jedisCluster, times(1)).lpop(queueName, 10);
        verifyNoMoreInteractions(jedisCluster);
    }

    @Test
    void processDeliverSm_shouldProcessMessageEventSuccessfully() {
        String deliverSmRaw = "{\"id\":\"1\",\"message_id\":\"messageId\",\"system_id\":\"spHttp\",\"deliver_sm_id\":\"deliverSmId\",\"deliver_sm_server_id\":\"deliverSmServerId\",\"command_status\":0,\"sequence_number\":1,\"source_addr_ton\":1,\"source_addr_npi\":1,\"source_addr\":\"sourceAddr\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"destinationAddr\",\"esm_class\":1,\"validity_period\":\"validityPeriod\",\"registered_delivery\":1,\"data_coding\":1,\"sm_default_msg_id\":1,\"short_message\":\"shortMessage\",\"delivery_receipt\":\"delReceipt\",\"status\":\"status\",\"error_code\":\"errorCode\",\"check_submit_sm_response\":true,\"optional_parameters\":[],\"origin_network_type\":\"originNetworkType\",\"origin_protocol\":\"originProtocol\",\"origin_network_id\":1,\"dest_network_type\":\"destNetworkType\",\"dest_protocol\":\"destProtocol\",\"dest_network_id\":1,\"routing_id\":1,\"msisdn\":\"msisdn\",\"address_nature_msisdn\":1,\"numbering_plan_msisdn\":1,\"remote_dialog_id\":1,\"local_dialog_id\":1,\"sccp_called_party_address_pc\":1,\"sccp_called_party_address_ssn\":1,\"sccp_called_party_address\":\"sccpCalledPartyAddress\",\"sccp_calling_party_address_pc\":1,\"sccp_calling_party_address_ssn\":1,\"sccp_calling_party_address\":\"sccpCallingPartyAddress\",\"global_title\":\"globalTitle\",\"global_title_indicator\":\"globalTitleIndicator\",\"translation_type\":1,\"smsc_ssn\":1,\"hlr_ssn\":1,\"msc_ssn\":1,\"map_version\":1,\"is_retry\":false,\"retry_dest_network_id\":\"retryDestNetworkId\",\"retry_number\":1,\"is_last_retry\":false,\"is_network_notify_error\":false,\"due_delay\":1,\"accumulated_time\":1,\"drop_map_sri\":false,\"network_id_to_map_sri\":1,\"network_id_to_permanent_failure\":1,\"drop_temp_failure\":false,\"network_id_temp_failure\":1,\"imsi\":\"imsi\",\"network_node_number\":\"networkNodeNumber\",\"network_node_number_nature_of_address\":1,\"network_node_number_numbering_plan\":1,\"mo_message\":false,\"is_sri_response\":false,\"check_sri_response\":false,\"msg_reference_number\":\"msgReferenceNumber\",\"total_segment\":1,\"segment_sequence\":1,\"originator_sccp_address\":\"originatorSccpAddress\",\"udhi\":\"udhi\",\"udh_json\":\"udhJson\",\"parent_id\":\"parentId\",\"is_dlr\":false,\"message_parts\":[],\"process\":true}";
        ServiceProvider sp = new ServiceProvider();
        sp.setCallbackUrl("http://callback.url");
        sp.setAuthenticationTypes("Undefined");
        assertDoesNotThrow(() -> deliverSmQueueConsumer.processDeliverSm(deliverSmRaw));
     }

     @Test
     void processDeliverSm_SystemIdNull() {
        String deliverSmRaw = "{\"id\":\"1\",\"message_id\":\"messageId\",\"system_id\":null,\"deliver_sm_id\":\"deliverSmId\",\"deliver_sm_server_id\":\"deliverSmServerId\",\"command_status\":0,\"sequence_number\":1,\"source_addr_ton\":1,\"source_addr_npi\":1,\"source_addr\":\"sourceAddr\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"destinationAddr\",\"esm_class\":1,\"validity_period\":\"validityPeriod\",\"registered_delivery\":1,\"data_coding\":1,\"sm_default_msg_id\":1,\"short_message\":\"shortMessage\",\"delivery_receipt\":\"delReceipt\",\"status\":\"status\",\"error_code\":\"errorCode\",\"check_submit_sm_response\":true,\"optional_parameters\":[],\"origin_network_type\":\"originNetworkType\",\"origin_protocol\":\"originProtocol\",\"origin_network_id\":1,\"dest_network_type\":\"destNetworkType\",\"dest_protocol\":\"destProtocol\",\"dest_network_id\":1,\"routing_id\":1,\"msisdn\":\"msisdn\",\"address_nature_msisdn\":1,\"numbering_plan_msisdn\":1,\"remote_dialog_id\":1,\"local_dialog_id\":1,\"sccp_called_party_address_pc\":1,\"sccp_called_party_address_ssn\":1,\"sccp_called_party_address\":\"sccpCalledPartyAddress\",\"sccp_calling_party_address_pc\":1,\"sccp_calling_party_address_ssn\":1,\"sccp_calling_party_address\":\"sccpCallingPartyAddress\",\"global_title\":\"globalTitle\",\"global_title_indicator\":\"globalTitleIndicator\",\"translation_type\":1,\"smsc_ssn\":1,\"hlr_ssn\":1,\"msc_ssn\":1,\"map_version\":1,\"is_retry\":false,\"retry_dest_network_id\":\"retryDestNetworkId\",\"retry_number\":1,\"is_last_retry\":false,\"is_network_notify_error\":false,\"due_delay\":1,\"accumulated_time\":1,\"drop_map_sri\":false,\"network_id_to_map_sri\":1,\"network_id_to_permanent_failure\":1,\"drop_temp_failure\":false,\"network_id_temp_failure\":1,\"imsi\":\"imsi\",\"network_node_number\":\"networkNodeNumber\",\"network_node_number_nature_of_address\":1,\"network_node_number_numbering_plan\":1,\"mo_message\":false,\"is_sri_response\":false,\"check_sri_response\":false,\"msg_reference_number\":\"msgReferenceNumber\",\"total_segment\":1,\"segment_sequence\":1,\"originator_sccp_address\":\"originatorSccpAddress\",\"udhi\":\"udhi\",\"udh_json\":\"udhJson\",\"parent_id\":\"parentId\",\"is_dlr\":false,\"message_parts\":[],\"process\":true}";
        ServiceProvider sp = new ServiceProvider();
        sp.setCallbackUrl("http://callback.url");
        sp.setAuthenticationTypes("Undefined");
        assertDoesNotThrow(() -> deliverSmQueueConsumer.processDeliverSm(deliverSmRaw));
    }

    @Test
    void processDeliverSmNull() {
        assertDoesNotThrow(() -> deliverSmQueueConsumer.processDeliverSm(null));
    }

    @Test
    void getHttpHeaders_shouldReturnHeadersWithAuthentication() {
        ServiceProvider sp = mock(ServiceProvider.class);
        when(sp.getAuthenticationTypes()).thenReturn("Basic");
        when(sp.getHeaderSecurityName()).thenReturn("Authorization");
        when(sp.getToken()).thenReturn("Basic some-token");

        UtilsRecords.CallbackHeaderHttp headerHttp = new UtilsRecords.CallbackHeaderHttp("Custom-Header", "HeaderValue");
        when(sp.getCallbackHeadersHttp()).thenReturn(List.of(headerHttp));

        HttpHeaders headers = DeliverSmQueueConsumer.getHttpHeaders(sp);

        assertEquals("Content type should be application/json",
                MediaType.APPLICATION_JSON.toString(),
                headers.getContentType().toString());

        assertEquals("Authorization header should match",
                "Basic some-token",
                headers.getFirst("Authorization"));

        assertEquals("Custom header should match",
                "HeaderValue",
                headers.getFirst("Custom-Header"));
    }

    private String createMessageEventJson(String id) {
        return "{\"id\":\"" + id + "\",\"message_id\":\"messageId\",\"system_id\":\"spHttp\",\"deliver_sm_id\":\"deliverSmId\",\"deliver_sm_server_id\":\"deliverSmServerId\",\"command_status\":0,\"sequence_number\":1,\"source_addr_ton\":1,\"source_addr_npi\":1,\"source_addr\":\"sourceAddr\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"destinationAddr\",\"esm_class\":1,\"validity_period\":\"validityPeriod\",\"registered_delivery\":1,\"data_coding\":1,\"sm_default_msg_id\":1,\"short_message\":\"shortMessage\",\"delivery_receipt\":\"delReceipt\",\"status\":\"status\",\"error_code\":\"errorCode\",\"check_submit_sm_response\":true,\"optional_parameters\":[],\"origin_network_type\":\"originNetworkType\",\"origin_protocol\":\"originProtocol\",\"origin_network_id\":1,\"dest_network_type\":\"destNetworkType\",\"dest_protocol\":\"destProtocol\",\"dest_network_id\":1,\"routing_id\":1,\"msisdn\":\"msisdn\",\"address_nature_msisdn\":1,\"numbering_plan_msisdn\":1,\"remote_dialog_id\":1,\"local_dialog_id\":1,\"sccp_called_party_address_pc\":1,\"sccp_called_party_address_ssn\":1,\"sccp_called_party_address\":\"sccpCalledPartyAddress\",\"sccp_calling_party_address_pc\":1,\"sccp_calling_party_address_ssn\":1,\"sccp_calling_party_address\":\"sccpCallingPartyAddress\",\"global_title\":\"globalTitle\",\"global_title_indicator\":\"globalTitleIndicator\",\"translation_type\":1,\"smsc_ssn\":1,\"hlr_ssn\":1,\"msc_ssn\":1,\"map_version\":1,\"is_retry\":false,\"retry_dest_network_id\":\"retryDestNetworkId\",\"retry_number\":1,\"is_last_retry\":false,\"is_network_notify_error\":false,\"due_delay\":1,\"accumulated_time\":1,\"drop_map_sri\":false,\"network_id_to_map_sri\":1,\"network_id_to_permanent_failure\":1,\"drop_temp_failure\":false,\"network_id_temp_failure\":1,\"imsi\":\"imsi\",\"network_node_number\":\"networkNodeNumber\",\"network_node_number_nature_of_address\":1,\"network_node_number_numbering_plan\":1,\"mo_message\":false,\"is_sri_response\":false,\"check_sri_response\":false,\"msg_reference_number\":\"msgReferenceNumber\",\"total_segment\":1,\"segment_sequence\":1,\"originator_sccp_address\":\"originatorSccpAddress\",\"udhi\":\"udhi\",\"udh_json\":\"udhJson\",\"parent_id\":\"parentId\",\"is_dlr\":false,\"message_parts\":[],\"process\":true}";
    }
}
