package com.http.server.utils;

import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.utils.UtilsEnum;

public class CdrHandler {

    private CdrHandler() {
        throw new IllegalStateException("Utility class");
    }

    public static void handlerCdrDetail(MessageEvent deliverSmEvent, UtilsEnum.MessageType messageType, UtilsEnum.CdrStatus cdrStatus, CdrProcessor cdrProcessor, boolean createCdr, String message) {
        cdrProcessor.putCdrDetailOnRedis(
                deliverSmEvent.toCdrDetail(UtilsEnum.Module.HTTP_SERVER, messageType, cdrStatus, message.toUpperCase()));
        if (createCdr) {
            cdrProcessor.createCdr(deliverSmEvent.getMessageId());
        }
    }
}
