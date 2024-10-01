package com.http.server.services;

import com.http.server.components.CreditHandler;
import com.http.server.dto.GlobalRecords;
import com.http.server.http.HttpServerManager;
import com.http.server.utils.Constants;
import com.paicbd.smsc.dto.GeneralSettings;
import com.paicbd.smsc.utils.SmppEncoding;
import com.paicbd.smsc.utils.Watcher;
import com.paicbd.smsc.dto.MessageEvent;
import com.http.server.utils.AppProperties;
import com.http.server.utils.CdrHandler;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.ServiceProvider;
import com.paicbd.smsc.utils.UtilsEnum;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import redis.clients.jedis.JedisCluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static com.http.server.utils.Constants.IS_STARTED;

/**
 * Service class for processing submit_sm messages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {
    private final AtomicInteger requestPerSecond = new AtomicInteger(0);
    private final JedisCluster jedisCluster;
    private final CdrProcessor cdrProcessor;
    private final AppProperties appProperties;
    private final HttpServerManager httpServerManager;
    private final CreditHandler handlerCreditBalance;


    @PostConstruct
    public void init() {
        Thread.startVirtualThread(() -> new Watcher("Message Processing", requestPerSecond, 1));
    }

    /**
     * Processes the message.
     *
     * @param messageRequest The message request DTO to process
     * @return A Mono wrapping the ResponseEntity with a success message if processing is successful,
     * otherwise an error response
     */
    public GlobalRecords.SubmitResponse message(GlobalRecords.MessageRequest messageRequest) {
        try {
            if (!SmppEncoding.isValidDataCoding(messageRequest.dataCoding())) {
                log.info("Invalid data coding {} for submit_sm: {}", messageRequest.dataCoding(), messageRequest);
                return new GlobalRecords.SubmitResponse("Invalid data coding");
            }

            String systemId = messageRequest.systemId();
            if (httpServerManager.getServiceProvider(systemId).getEnabled() != IS_STARTED) {
                return new GlobalRecords.SubmitResponse("Service not found");
            }

            if (handlerCreditBalance.hasCredit(systemId)) {
                MessageEvent messageEventBase = this.getMessageEventBase(messageRequest);
                return switch (messageRequest.destinationAddr()) {
                    case String destination -> this.processSingleDestinationMessage(messageEventBase, destination);
                    case List<?> destinationArrays -> this.processMultiDestinationMessage(messageEventBase, destinationArrays);
                    default -> new GlobalRecords.SubmitResponse("Bad type format of destination");
                };
            }
            return new GlobalRecords.SubmitResponse("Service provider don't have enough credits");

        } catch (Exception e) {
            log.error("Error on process messages -> {}", e.getMessage());
            return new GlobalRecords.SubmitResponse(e.getMessage());
        }
    }

    private MessageEvent getMessageEventBase(GlobalRecords.MessageRequest messageRequest) {
        GeneralSettings httpGeneralSettings = httpServerManager.getGeneralSettings();
        ServiceProvider sp = httpServerManager.getServiceProvider(messageRequest.systemId());
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setSystemId(messageRequest.systemId());
        messageEvent.setOriginNetworkId(sp.getNetworkId());
        messageEvent.setOriginNetworkType("SP");
        messageEvent.setOriginProtocol("HTTP");
        messageEvent.setDataCoding(Objects.isNull(messageRequest.dataCoding()) ? httpGeneralSettings.getEncodingGsm7() : messageRequest.dataCoding());
        messageEvent.setSourceAddrTon(messageRequest.sourceAddrTon());
        messageEvent.setSourceAddrNpi(messageRequest.sourceAddrNpi());
        messageEvent.setSourceAddr(messageRequest.sourceAddr());
        messageEvent.setDestAddrTon(messageRequest.destAddrTon());
        messageEvent.setDestAddrNpi(messageRequest.destAddrNpi());
        messageEvent.setEsmClass(messageRequest.esmClass());
        messageEvent.setValidityPeriod(messageRequest.validityPeriod());
        messageEvent.setRegisteredDelivery(messageRequest.registeredDelivery());
        messageEvent.setDataCoding(messageRequest.dataCoding());
        messageEvent.setSmDefaultMsgId(messageRequest.smDefaultMsgId());
        messageEvent.setShortMessage(messageRequest.shortMessage());
        messageEvent.setOptionalParameters(messageRequest.optionalParameters());
        return messageEvent;
    }

    private MessageEvent prepareMessage(MessageEvent messageEvent, String destination) {
        var messageId = System.currentTimeMillis() + "-" + System.nanoTime();
        var message =  new MessageEvent().clone(messageEvent);
        message.setId(messageId);
        message.setMessageId(messageId);
        message.setParentId(messageId);
        message.setDestinationAddr(destination);
        return message;
    }

    private void sendMessageToRedis(MessageEvent messageEvent) {
        this.requestPerSecond.getAndIncrement();
        jedisCluster.lpush(appProperties.getPreMessageList(), messageEvent.toString());
        CdrHandler.handlerCdrDetail(messageEvent, UtilsEnum.MessageType.MESSAGE, UtilsEnum.CdrStatus.RECEIVED, cdrProcessor, false, "Received from SP");
    }

    private GlobalRecords.SubmitResponse processSingleDestinationMessage(MessageEvent messageEventBase, String destination) {
        var message = this.prepareMessage(messageEventBase, destination);
        this.sendMessageToRedis(message);
        return new GlobalRecords.SubmitResponse(message.getSystemId(), message.getMessageId());
    }

    private GlobalRecords.SubmitResponse processMultiDestinationMessage(MessageEvent messageEventBase, List<?> destinationArrays) {
        if (destinationArrays.size() > Constants.MAX_DESTINATIONS) {
            return new GlobalRecords.SubmitResponse("The maximum number of recipients has been exceeded");
        }
        var listOfMessage = new ArrayList<MessageEvent>(destinationArrays.size());
        destinationArrays.forEach(destination -> {
            var message =  this.prepareMessage(messageEventBase, (String) destination);
            listOfMessage.add(message);
        });
        Flux.fromIterable(listOfMessage).doOnNext(this::sendMessageToRedis).subscribeOn(Schedulers.boundedElastic()).subscribe();
        var messageIdList = listOfMessage.stream().map(MessageEvent::getMessageId).toList();
        return new GlobalRecords.SubmitResponse(messageEventBase.getSystemId(), messageIdList);
    }

}
