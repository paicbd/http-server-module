package com.http.server.utils;

import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.utils.UtilsEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class CdrHandlerTest {
    @Mock
    CdrProcessor cdrProcessor;

    @Test
    void testPrivateConstructor() throws NoSuchMethodException {
        Constructor<CdrHandler> constructor = CdrHandler.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    @Test
    void handlerCdrDetail() {
        MessageEvent deliverSmEvent = new MessageEvent();
        deliverSmEvent.setMessageId("messageId");
        assertDoesNotThrow(() ->
                CdrHandler.handlerCdrDetail(deliverSmEvent, UtilsEnum.MessageType.MESSAGE, UtilsEnum.CdrStatus.RECEIVED, cdrProcessor, false, "message"));

        assertDoesNotThrow(() ->
                CdrHandler.handlerCdrDetail(deliverSmEvent, UtilsEnum.MessageType.MESSAGE, UtilsEnum.CdrStatus.RECEIVED, cdrProcessor, true, "message"));
    }
}