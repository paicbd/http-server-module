package com.http.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.utils.Generated;

import java.util.List;
import java.util.Map;

@Generated
public class GlobalRecords {
    @Generated
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DeliverSm(
            @JsonProperty("message_id")
            String messageId,
            @JsonProperty("source_addr_ton")
            Integer sourceAddrTon,
            @JsonProperty("source_addr_npi")
            Integer sourceAddrNpi,
            @JsonProperty("source_addr")
            String sourceAddr,
            @JsonProperty("dest_addr_ton")
            Integer destAddrTon,
            @JsonProperty("dest_addr_npi")
            Integer destAddrNpi,
            @JsonProperty("destination_addr")
            String destinationAddr,
            @JsonProperty("esm_class")
            Integer esmClass,
            @JsonProperty("data_coding")
            Integer dataCoding,
            @JsonProperty("short_message")
            String shortMessage,
            @JsonProperty("status")
            String status,
            @JsonProperty("error_code")
            String errorCode,
            @JsonProperty("optional_parameters")
            List<UtilsRecords.OptionalParameter> optionalParameters,
            @JsonProperty("msg_reference_number")
            String msgReferenceNumber,
            @JsonProperty("total_segment")
            Integer totalSegment,
            @JsonProperty("segment_sequence")
            Integer segmentSequence
    ) {
    }

    @Generated
    public record DeliverSmResponse(
            boolean status,
            String message
    ) {
    }

    @Generated
    public record ServerHandler(
            String name,
            String ip,
            String port,
            String protocol,
            String scheme,
            String apiKey,
            String state
    ) {
    }

    @Generated
    public record MessageRequest(
            @JsonProperty("system_id") String systemId,
            @JsonProperty("source_addr_ton") Integer sourceAddrTon,
            @JsonProperty("source_addr_npi") Integer sourceAddrNpi,
            @JsonProperty("source_addr") String sourceAddr,
            @JsonProperty("dest_addr_ton") Integer destAddrTon,
            @JsonProperty("dest_addr_npi") Integer destAddrNpi,
            @JsonProperty("destination_addr") Object destinationAddr,
            @JsonProperty("esm_class") Integer esmClass,
            @JsonProperty("validity_period") long validityPeriod,
            @JsonProperty("registered_delivery") Integer registeredDelivery,
            @JsonProperty("data_coding") Integer dataCoding,
            @JsonProperty("sm_default_msg_id") int smDefaultMsgId,
            @JsonProperty("short_message") String shortMessage,
            @JsonProperty("optional_parameters") List<UtilsRecords.OptionalParameter> optionalParameters,
            @JsonProperty("custom_parameters") Map<String, Object> customParams
    ) {
    }

    @Generated
    public record SubmitResponse(
            @JsonProperty("system_id") String systemId,
            @JsonProperty("message_id") Object messageId,
            @JsonProperty("error_message") String errorMessage) {

        public SubmitResponse(String systemId, Object messageId) {
            this(systemId, messageId, "");
        }

        public SubmitResponse(String errorMessage) {
            this(null, null, errorMessage);
        }
    }
}
