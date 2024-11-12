package com.http.server.utils;

import lombok.Generated;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Represents the properties used by the application.
 */
@Getter
@Component
@Generated
public class AppProperties {
    @Value("#{'${redis.cluster.nodes}'.split(',')}")
    private List<String> redisNodes;

    @Value("${redis.threadPool.maxTotal:20}")
    private int redisMaxTotal;

    @Value("${redis.threadPool.maxIdle:20}")
    private int redisMaxIdle ;

    @Value("${redis.threadPool.minIdle:1}")
    private int redisMinIdle;

    @Value("${redis.threadPool.blockWhenExhausted:true}")
    private boolean redisBlockWhenExhausted;

    @Value("${redis.queue.deliver}")
    private String deliverSmQueue;

    @Value("${queue.consumer.workers:11}")
    private int workers;

    @Value("${queue.consumer.batch.size:1000}")
    private int batchSizePerWorker;

    @Value("${websocket.server.host:localhost}")
    private String wsHost;

    @Value("${websocket.server.port:9000}")
    private int wsPort;

    @Value("${websocket.server.path:/ws}")
    private String wsPath;

    @Value("${websocket.server.enabled:false}")
    private boolean wsEnabled;

    @Value("${websocket.header.name:Authorization}")
    private String wsHeaderName;

    @Value("${websocket.header.value}")
    private String wsHeaderValue;

    @Value("${websocket.retry.intervalSeconds}")
    private int wsRetryInterval;

    @Value("${smpp.serviceProvidersHashName:service_providers}")
    private String serviceProvidersHashTable;

    @Value("${smpp.serviceProvidersHashName:service_providers}")
    private String serviceProvidersHashName;

    @Value("${spring.application.name:http_server}")
    private String instanceName;

    @Value("${smpp.server.configurationHashName:configurations}")
    private String configurationHash;

    @Value("${spring.application.name:http-server-instance-01}")
    private String serverName;

    @Value("${server.ip:127.0.0.1}")
    private String instanceIp;

    @Value("${server.port:8080}")
    private String instancePort;

    @Value("${instance.initial.status:STOPPED}")
    private String instanceInitialStatus;

    @Value("${instance.protocol:HTTP}")
    private String instanceProtocol;

    @Value("${instance.scheme:http}")
    private String instanceScheme;

    @Value("${instance.ratingRequest.apiKey}")
    private String instanceApiKey;

    @Value("${http.server.general.settings.hash:general_settings}")
    private String httpGeneralSettingsHash;

    @Value("${http.server.general.settings.key:smpp_http}")
    private String httpGeneralSettingsKey;

    @Value("${redis.preMessageList:preMessage}")
    private String preMessageList;

    @Value("${redis.queue.deliver:http_dlr}")
    private String prefixQueueDeliver;
}
