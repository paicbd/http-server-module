package com.http.server.components;

import com.fasterxml.jackson.core.type.TypeReference;
import com.http.server.dto.GlobalRecords;
import com.http.server.utils.AppProperties;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.ServiceProvider;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.utils.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.JedisCluster;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliverSmConsumerTest {
    private static final String HTTP_SERVICE_PROVIDER_JSON = "{\"name\":\"httpSp01\",\"password\":\"1234\",\"tps\":1,\"validity\":0,\"network_id\":5,\"system_id\":\"httpSp01\",\"system_type\":\"\",\"interface_version\":\"IF_50\",\"address_ton\":0,\"address_npi\":0,\"address_range\":\"^[0-9a-zA-Z]*$\",\"enabled\":1,\"enquire_link_period\":3000,\"pdu_timeout\":5000,\"request_dlr\":false,\"status\":\"STOPPED\",\"bind_type\":\"TRANSMITTER\",\"max_binds\":1,\"current_binds_count\":0,\"credit\":0,\"binds\":[],\"credit_used\":0,\"is_prepaid\":true,\"has_available_credit\":false,\"protocol\":\"HTTP\",\"contact_name\":\"Obedis\",\"email\":\"mail@mali.com\",\"phone_number\":\"223232\",\"callback_url\":\"http://18.224.164.85:3000/api/callback\",\"authentication_types\":\"Undefined\",\"header_security_name\":\"\",\"token\":\"Undefined \",\"callback_headers_http\":[]}";

    @Mock
    JedisCluster jedisCluster;
    @Mock
    AppProperties appProperties;
    @Mock
    CdrProcessor cdrProcessor;
    @Mock
    ConcurrentMap<Integer, ServiceProvider> serviceProviderByNetworkIdCache;
    @Mock
    ConcurrentMap<String, ServiceProvider> serviceProviderBySystemIdCache;

    @InjectMocks
    DeliverSmConsumer deliverSmConsumer;

    @Mock
    RestTemplate restTemplate;

    @Test
    void init() {
        assertDoesNotThrow(() -> deliverSmConsumer.init());
    }

    @Test
    void startScheduler_DeliverSmQueueIsEmpty() {
        when(appProperties.getDeliverSmQueue()).thenReturn("deliverSmQueue");
        when(jedisCluster.llen("deliverSmQueue")).thenReturn(0L);
        assertDoesNotThrow(() -> deliverSmConsumer.startScheduler());
    }

    @Test
    void processingBatch() {
        when(jedisCluster.llen(appProperties.getDeliverSmQueue())).thenReturn(10L);
        when(appProperties.getWorkers()).thenReturn(10);
        assertDoesNotThrow(() -> deliverSmConsumer.startScheduler());
    }

    @Test
    void testProcessingBatch_throwsException() throws Exception {
        when(appProperties.getDeliverSmQueue()).thenReturn("queueName");
        when(appProperties.getBatchSizePerWorker()).thenReturn(10);

        when(jedisCluster.lpop("queueName", 10)).thenThrow(new RuntimeException("Simulated exception"));

        Method method = DeliverSmConsumer.class.getDeclaredMethod("processingBatch");
        method.setAccessible(true);
        Runnable runnable = (Runnable) method.invoke(deliverSmConsumer);

        runnable.run();

        verify(appProperties).getDeliverSmQueue();
        verify(appProperties).getBatchSizePerWorker();
        verify(jedisCluster).lpop("queueName", 10);
    }

    @Test
    void testProcessingBatchListNull() throws Exception {
        when(appProperties.getDeliverSmQueue()).thenReturn("queueName");
        when(appProperties.getBatchSizePerWorker()).thenReturn(10);

        when(jedisCluster.lpop("queueName", 10)).thenReturn(null);

        Method method = DeliverSmConsumer.class.getDeclaredMethod("processingBatch");
        method.setAccessible(true);
        Runnable runnable = (Runnable) method.invoke(deliverSmConsumer);

        runnable.run();

        verify(appProperties).getDeliverSmQueue();
        verify(appProperties).getBatchSizePerWorker();
        verify(jedisCluster).lpop("queueName", 10);
    }

    @Test
    void testProcessingBatch() throws Exception {
        when(appProperties.getDeliverSmQueue()).thenReturn("queueName");
        when(appProperties.getBatchSizePerWorker()).thenReturn(10);

        serviceProviderBySystemIdCache = new ConcurrentHashMap<>();
        serviceProviderBySystemIdCache.put("httpSp01", Converter.stringToObject(HTTP_SERVICE_PROVIDER_JSON, new TypeReference<>() {
        }));

        serviceProviderByNetworkIdCache = new ConcurrentHashMap<>();
        ServiceProvider sp = Converter.stringToObject(HTTP_SERVICE_PROVIDER_JSON, new TypeReference<>() {
        });
        sp.setAuthenticationTypes("Bearer");
        sp.setCallbackHeadersHttp(List.of(new UtilsRecords.CallbackHeaderHttp("Authorization", "Bearer token")));
        serviceProviderByNetworkIdCache.put(5, sp);

        deliverSmConsumer = new DeliverSmConsumer(jedisCluster, appProperties, cdrProcessor, serviceProviderByNetworkIdCache, serviceProviderBySystemIdCache);

        List<String> deliverSmItems = getDeliver();
        when(jedisCluster.lpop("queueName", 10)).thenReturn(deliverSmItems);

        Method method = DeliverSmConsumer.class.getDeclaredMethod("processingBatch");
        method.setAccessible(true);
        Runnable runnable = (Runnable) method.invoke(deliverSmConsumer);

        runnable.run();

        verify(appProperties).getDeliverSmQueue();
        verify(appProperties).getBatchSizePerWorker();
        verify(jedisCluster).lpop("queueName", 10);
    }

    @Test
    void processDeliverSm_ThrowsException_dueToInvalidJson() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        when(appProperties.getDeliverSmQueue()).thenReturn("queueName");
        when(appProperties.getBatchSizePerWorker()).thenReturn(10);
        deliverSmConsumer = new DeliverSmConsumer(jedisCluster, appProperties, cdrProcessor, serviceProviderByNetworkIdCache, serviceProviderBySystemIdCache);

        when(serviceProviderBySystemIdCache.get(anyString())).thenThrow(new RuntimeException("Simulated exception"));
        List<String> deliverSmItems = getDeliver();
        when(jedisCluster.lpop("queueName", 10)).thenReturn(deliverSmItems);

        Method method = DeliverSmConsumer.class.getDeclaredMethod("processingBatch");
        method.setAccessible(true);
        Runnable runnable = (Runnable) method.invoke(deliverSmConsumer);

        assertDoesNotThrow(runnable::run);
    }

    @Test
    void testSendPostRequest_statusNot200() {
        String url = "https://mockurl.com";
        String body = "{\"message\":\"Error response\"}";
        HttpEntity<String> request = new HttpEntity<>(body, null);

        GlobalRecords.DeliverSmResponse response = deliverSmConsumer.sendPostRequest(url, request);
        assertFalse(response.status());
    }

    private List<String> getDeliver() {
        return List.of(
                "{\"msisdn\":null,\"id\":\"1722615370524-9949305861760\",\"message_id\":\"1722615338515-9917296608286\",\"system_id\":\"httpSp01\",\"deliver_sm_id\":\"b6b67189-867e-45bb-9548-8ef49a304d87\",\"deliver_sm_server_id\":\"1722615338515-9917296608286\",\"command_status\":0,\"sequence_number\":14,\"source_addr_ton\":1,\"source_addr_npi\":1,\"source_addr\":\"5555\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"6666\",\"esm_class\":4,\"validity_period\":\"60\",\"registered_delivery\":1,\"data_coding\":0,\"sm_default_msg_id\":0,\"short_message\":\"id:1722615338515-9917296608286 sub:001 dlvrd:001 submit date:2408021016 done date:2408021016 stat:DELIVRD err:000 text:\",\"delivery_receipt\":\"id:1722615338515-9917296608286 sub:001 dlvrd:001 submit date:2408021016 done date:2408021016 stat:DELIVRD err:000 text:\",\"status\":\"DELIVRD\",\"error_code\":\"000\",\"check_submit_sm_response\":true,\"optional_parameters\":[{\"tag\":30,\"value\":\"b6b67189-867e-45bb-9548-8ef49a304d87\"},{\"tag\":1063,\"value\":\"1\"}],\"origin_network_type\":\"GW\",\"origin_protocol\":\"SMPP\",\"origin_network_id\":6,\"dest_network_type\":\"SP\",\"dest_protocol\":\"HTTP\",\"dest_network_id\":5,\"routing_id\":0,\"address_nature_msisdn\":null,\"numbering_plan_msisdn\":null,\"remote_dialog_id\":null,\"local_dialog_id\":null,\"sccp_called_party_address_pc\":null,\"sccp_called_party_address_ssn\":null,\"sccp_called_party_address\":null,\"sccp_calling_party_address_pc\":null,\"sccp_calling_party_address_ssn\":null,\"sccp_calling_party_address\":null,\"global_title\":null,\"global_title_indicator\":null,\"translation_type\":null,\"smsc_ssn\":null,\"hlr_ssn\":null,\"msc_ssn\":null,\"map_version\":null,\"is_retry\":false,\"retry_dest_network_id\":null,\"retry_number\":null,\"is_last_retry\":false,\"is_network_notify_error\":false,\"due_delay\":0,\"accumulated_time\":0,\"drop_map_sri\":false,\"network_id_to_map_sri\":0,\"network_id_to_permanent_failure\":0,\"drop_temp_failure\":false,\"network_id_temp_failure\":0,\"imsi\":null,\"network_node_number\":null,\"network_node_number_nature_of_address\":null,\"network_node_number_numbering_plan\":null,\"mo_message\":false,\"is_sri_response\":false,\"check_sri_response\":false,\"msg_reference_number\":null,\"total_segment\":null,\"segment_sequence\":null,\"originator_sccp_address\":null,\"udhi\":null,\"udh_json\":null,\"parent_id\":null,\"is_dlr\":true,\"message_parts\":null}",
                "{\"msisdn\":null,\"id\":\"1722615370526-9949307382958\",\"message_id\":\"1722615339493-9918274592992\",\"system_id\":null,\"deliver_sm_id\":\"29bb38a6-8d0d-43a0-9586-7b97f8d2c479\",\"deliver_sm_server_id\":\"1722615339493-9918274592992\",\"command_status\":0,\"sequence_number\":12,\"source_addr_ton\":1,\"source_addr_npi\":1,\"source_addr\":\"5555\",\"dest_addr_ton\":1,\"dest_addr_npi\":1,\"destination_addr\":\"6666\",\"esm_class\":4,\"validity_period\":\"60\",\"registered_delivery\":1,\"data_coding\":0,\"sm_default_msg_id\":0,\"short_message\":\"id:1722615339493-9918274592992 sub:001 dlvrd:001 submit date:2408021016 done date:2408021016 stat:DELIVRD err:000 text:\",\"delivery_receipt\":\"id:1722615339493-9918274592992 sub:001 dlvrd:001 submit date:2408021016 done date:2408021016 stat:DELIVRD err:000 text:\",\"status\":\"DELIVRD\",\"error_code\":\"000\",\"check_submit_sm_response\":true,\"optional_parameters\":[{\"tag\":30,\"value\":\"29bb38a6-8d0d-43a0-9586-7b97f8d2c479\"},{\"tag\":1063,\"value\":\"1\"}],\"origin_network_type\":\"GW\",\"origin_protocol\":\"SMPP\",\"origin_network_id\":6,\"dest_network_type\":\"SP\",\"dest_protocol\":\"HTTP\",\"dest_network_id\":5,\"routing_id\":0,\"address_nature_msisdn\":null,\"numbering_plan_msisdn\":null,\"remote_dialog_id\":null,\"local_dialog_id\":null,\"sccp_called_party_address_pc\":null,\"sccp_called_party_address_ssn\":null,\"sccp_called_party_address\":null,\"sccp_calling_party_address_pc\":null,\"sccp_calling_party_address_ssn\":null,\"sccp_calling_party_address\":null,\"global_title\":null,\"global_title_indicator\":null,\"translation_type\":null,\"smsc_ssn\":null,\"hlr_ssn\":null,\"msc_ssn\":null,\"map_version\":null,\"is_retry\":false,\"retry_dest_network_id\":null,\"retry_number\":null,\"is_last_retry\":false,\"is_network_notify_error\":false,\"due_delay\":0,\"accumulated_time\":0,\"drop_map_sri\":false,\"network_id_to_map_sri\":0,\"network_id_to_permanent_failure\":0,\"drop_temp_failure\":false,\"network_id_temp_failure\":0,\"imsi\":null,\"network_node_number\":null,\"network_node_number_nature_of_address\":null,\"network_node_number_numbering_plan\":null,\"mo_message\":false,\"is_sri_response\":false,\"check_sri_response\":false,\"msg_reference_number\":null,\"total_segment\":null,\"segment_sequence\":null,\"originator_sccp_address\":null,\"udhi\":null,\"udh_json\":null,\"parent_id\":null,\"is_dlr\":true,\"message_parts\":null}"
        );
    }
}