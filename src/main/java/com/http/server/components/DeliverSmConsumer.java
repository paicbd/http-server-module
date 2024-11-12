package com.http.server.components;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.http.server.utils.AppProperties;
import com.http.server.utils.CdrHandler;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.Watcher;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.utils.UtilsEnum;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.paicbd.smsc.dto.MessageEvent;
import com.http.server.dto.GlobalRecords;
import com.paicbd.smsc.dto.ServiceProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.JedisCluster;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliverSmConsumer {
    private final JedisCluster jedisCluster;
    private final AppProperties appProperties;
    private final CdrProcessor cdrProcessor;
    private final ConcurrentMap<Integer, ServiceProvider> serviceProviderByNetworkIdCache;
    private final ConcurrentMap<String, ServiceProvider> serviceProviderBySystemIdCache;

    private final RestTemplate restTemplate = new RestTemplate();
    private final AtomicInteger requestPerSecond = new AtomicInteger(0);
    private final ThreadFactory factory = Thread.ofVirtual().name("deliverSm-", 0).factory();
    private final ExecutorService executorService = Executors.newThreadPerTaskExecutor(factory);

    @PostConstruct
    public void init() {
        Thread.startVirtualThread(() -> new Watcher("DLR Processing", requestPerSecond, 1));
    }

    @Scheduled(fixedDelayString = "${queue.consumer.scheduler}")
    public void startScheduler() {
        if (jedisCluster.llen(appProperties.getDeliverSmQueue()) > 0) {
            IntStream.range(0, appProperties.getWorkers())
                    .parallel()
                    .forEach(x -> executorService.execute(processingBatch()));
        }
    }

    private Runnable processingBatch() {
        return () -> {
            try {
                List<String> deliverSmItems =
                        jedisCluster.lpop(appProperties.getDeliverSmQueue(), appProperties.getBatchSizePerWorker());
                if (deliverSmItems != null) {
                    deliverSmItems
                            .parallelStream()
                            .filter(Objects::nonNull)
                            .forEach(deliverSmItemRaw -> {
                                requestPerSecond.getAndIncrement();
                                processDeliverSm(deliverSmItemRaw);
                            });
                }
            } catch (Exception e) {
                log.warn("Error {}", e.toString());
                Thread.currentThread().interrupt();
            }
        };
    }

    protected void processDeliverSm(String deliverSmRaw) {
        try {
            MessageEvent deliverSmEvent = Converter.stringToObject(deliverSmRaw, new TypeReference<>() {
            });
            GlobalRecords.DeliverSm deliverSm = deliverSmEventToDeliverSmRecord(deliverSmEvent);

            ServiceProvider sp;
            if (Objects.nonNull(deliverSmEvent.getSystemId())) {
                sp = serviceProviderBySystemIdCache.get(deliverSmEvent.getSystemId());
            } else {
                sp = serviceProviderByNetworkIdCache.get(deliverSmEvent.getDestNetworkId());
            }
            Objects.requireNonNull(sp, "An error occurred, service provider cannot be null");

            String url = sp.getCallbackUrl();
            HttpHeaders headers = getHttpHeaders(sp);
            HttpEntity<GlobalRecords.DeliverSm> request = new HttpEntity<>(deliverSm, headers);
            GlobalRecords.DeliverSmResponse deliverSmResponse = sendPostRequest(url, request);

            CdrHandler.handlerCdrDetail(
                    deliverSmEvent, UtilsEnum.MessageType.DELIVER,
                    deliverSmResponse.status() ? UtilsEnum.CdrStatus.SENT : UtilsEnum.CdrStatus.FAILED,
                    cdrProcessor, true, deliverSmResponse.message());
        } catch (Exception e) {
            log.error("Error for processing deliverSm  {} -> {}", deliverSmRaw, e.getMessage(), e);
        }
    }

    static HttpHeaders getHttpHeaders(ServiceProvider sp) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (!"Undefined".equalsIgnoreCase(sp.getAuthenticationTypes())) {
            headers.add(sp.getHeaderSecurityName(), sp.getToken());
        }

        List<UtilsRecords.CallbackHeaderHttp> callbackHeaders = sp.getCallbackHeadersHttp();
        for (UtilsRecords.CallbackHeaderHttp callbackHeader : callbackHeaders) {
            headers.add(callbackHeader.headerName(), callbackHeader.headerValue());
        }
        return headers;
    }

    public GlobalRecords.DeliverSmResponse sendPostRequest(String url, HttpEntity<?> request) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            int statusCode = response.getStatusCode().value();
            if (response.getStatusCode().value() != 200) {
                log.info("Error status code {} -> {}", statusCode, response.getBody());
                return new GlobalRecords.DeliverSmResponse(false, "Error status code " + statusCode);
            }
            return new GlobalRecords.DeliverSmResponse(true, "sent to SP");
        } catch (Exception e) {
            log.error("Error to send request -> {}", e.getMessage());
            return new GlobalRecords.DeliverSmResponse(false, "Error to send request (" + e.getMessage() + ")");
        }
    }

    private GlobalRecords.DeliverSm deliverSmEventToDeliverSmRecord(MessageEvent deliverSm) {
        return new GlobalRecords.DeliverSm(
                deliverSm.getParentId(),
                deliverSm.getSourceAddrTon(),
                deliverSm.getSourceAddrNpi(),
                deliverSm.getSourceAddr(),
                deliverSm.getDestAddrTon(),
                deliverSm.getDestAddrNpi(),
                deliverSm.getDestinationAddr(),
                deliverSm.getEsmClass(),
                deliverSm.getDataCoding(),
                deliverSm.getShortMessage(),
                deliverSm.getStatus(),
                deliverSm.getErrorCode(),
                deliverSm.getOptionalParameters(),
                deliverSm.getMsgReferenceNumber(),
                deliverSm.getTotalSegment(),
                deliverSm.getSegmentSequence()
        );
    }
}
