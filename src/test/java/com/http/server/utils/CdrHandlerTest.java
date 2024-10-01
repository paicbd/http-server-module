package com.http.server.utils;

import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.utils.UtilsEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CdrHandlerTest {

    @Mock
    private CdrProcessor mockCdrProcessor;

    @Test
    void testPrivateConstructor() throws NoSuchMethodException {
        Constructor<CdrHandler> constructor = CdrHandler.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    @Test
    void handlerCdrDetail_createsCdr() {
        MessageEvent messageEvent = new MessageEvent();
        UtilsEnum.MessageType messageType = UtilsEnum.MessageType.DELIVER;
        UtilsEnum.CdrStatus cdrStatus = UtilsEnum.CdrStatus.SENT;
        boolean createCdr = true;
        String message = "Test Message";
        CdrHandler.handlerCdrDetail(messageEvent, messageType, cdrStatus, mockCdrProcessor, createCdr, message);
        verify(mockCdrProcessor).putCdrDetailOnRedis(any());
        verify(mockCdrProcessor).createCdr(messageEvent.getMessageId());
    }

    @Test
    void handlerCdrDetail_doesNotCreateCdr() {
        MessageEvent messageEvent = new MessageEvent();
        UtilsEnum.MessageType messageType = UtilsEnum.MessageType.DELIVER;
        UtilsEnum.CdrStatus cdrStatus = UtilsEnum.CdrStatus.FAILED;
        boolean createCdr = false;
        String message = "Test Message";
        CdrHandler.handlerCdrDetail(messageEvent, messageType, cdrStatus, mockCdrProcessor, createCdr, message);
        verify(mockCdrProcessor).putCdrDetailOnRedis(any());
        verify(mockCdrProcessor, never()).createCdr(any());
    }
}
