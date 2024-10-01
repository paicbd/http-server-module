package com.http.server.dto;

import com.http.server.dto.GlobalRecords.DeliverSm;
import com.http.server.dto.GlobalRecords.DeliverSmResponse;
import com.paicbd.smsc.dto.UtilsRecords;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GlobalRecordsTest {

    @Test
    void testOptionalParameter_record() {
        short tag = 1;
        String value = "testValue";
        UtilsRecords.OptionalParameter optionalParameter = new UtilsRecords.OptionalParameter(tag, value);

        assertEquals(tag, optionalParameter.tag());
        assertEquals(value, optionalParameter.value());
    }

    @Test
    void testDeliverSm_record() {
        String messageId = "msg123";
        Integer sourceAddrTon = 1;
        Integer sourceAddrNpi = 1;
        String sourceAddr = "sourceAddr";
        Integer destAddrTon = 1;
        Integer destAddrNpi = 1;
        String destinationAddr = "destAddr";
        Integer esmClass = 1;
        Integer dataCoding = 1;
        String shortMessage = "shortMessage";
        String status = "status";
        String errorCode = "errorCode";
        List<UtilsRecords.OptionalParameter> optionalParameters = List.of(new UtilsRecords.OptionalParameter((short) 1, "value"));

        DeliverSm deliverSm = new DeliverSm(
                messageId, sourceAddrTon, sourceAddrNpi, sourceAddr,
                destAddrTon, destAddrNpi, destinationAddr, esmClass,
                dataCoding, shortMessage, status, errorCode, optionalParameters,
                null, null, null
        );

        assertEquals(messageId, deliverSm.messageId());
        assertEquals(sourceAddrTon, deliverSm.sourceAddrTon());
        assertEquals(sourceAddrNpi, deliverSm.sourceAddrNpi());
        assertEquals(sourceAddr, deliverSm.sourceAddr());
        assertEquals(destAddrTon, deliverSm.destAddrTon());
        assertEquals(destAddrNpi, deliverSm.destAddrNpi());
        assertEquals(destinationAddr, deliverSm.destinationAddr());
        assertEquals(esmClass, deliverSm.esmClass());
        assertEquals(dataCoding, deliverSm.dataCoding());
        assertEquals(shortMessage, deliverSm.shortMessage());
        assertEquals(status, deliverSm.status());
        assertEquals(errorCode, deliverSm.errorCode());
        assertEquals(optionalParameters, deliverSm.optionalParameters());
    }

    @Test
    void testDeliverSmResponse_record() {
        boolean status = true;
        String message = "Success";

        DeliverSmResponse deliverSmResponse = new DeliverSmResponse(status, message);

        assertEquals(status, deliverSmResponse.status());
        assertEquals(message, deliverSmResponse.message());
    }

    @Test
    void testGlobalRecords() {
        GlobalRecords globalRecords = new GlobalRecords();
        assertNotNull(globalRecords);
    }

    @Test
    void testRecordConstructionAndAccessors() {
        GlobalRecords.ServerHandler serverHandler = new GlobalRecords.ServerHandler(
                "Server1",
                "192.168.1.1",
                "8080",
                "HTTP",
                "http",
                "12345",
                "RUNNING"
        );

        assertEquals("Server1", serverHandler.name(), "Name should match");
        assertEquals("192.168.1.1", serverHandler.ip(), "IP should match");
        assertEquals("8080", serverHandler.port(), "Port should match");
        assertEquals("HTTP", serverHandler.protocol(), "Protocol should match");
        assertEquals("http", serverHandler.scheme(), "Scheme should match");
        assertEquals("12345", serverHandler.apiKey(), "API Key should match");
        assertEquals("RUNNING", serverHandler.state(), "State should match");
    }

    @Test
    void testToString() {
        GlobalRecords.ServerHandler serverHandler = new GlobalRecords.ServerHandler(
                "Server1",
                "192.168.1.1",
                "8080",
                "HTTP",
                "http",
                "12345",
                "RUNNING"
        );

        String expectedString = "ServerHandler[name=Server1, ip=192.168.1.1, port=8080, protocol=HTTP, scheme=http, apiKey=12345, state=RUNNING]";
        assertEquals(expectedString, serverHandler.toString(), "ToString representation should match");
    }

    @Test
    void testConstructor_withSystemIdAndMessageId() {
        String systemId = "system123";
        String messageId = "message123";
        GlobalRecords.SubmitResponse submitResponse = new GlobalRecords.SubmitResponse(systemId, messageId);

        assertEquals(systemId, submitResponse.systemId());
        assertEquals(messageId, submitResponse.messageId());
        assertEquals("", submitResponse.errorMessage());
    }

    @Test
    void testConstructor_withErrorMessage() {
        String errorMessage = "Error occurred";
        GlobalRecords.SubmitResponse submitResponse = new GlobalRecords.SubmitResponse(errorMessage);

        assertNull(submitResponse.systemId());
        assertNull(submitResponse.messageId());
        assertEquals(errorMessage, submitResponse.errorMessage());
    }
}
