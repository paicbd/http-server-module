package com.http.server.components;

import com.http.server.utils.AppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

/**
 * Class responsible for registering and unregistering an instance in redis.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoRegister {
    private final AppProperties appProperties;
    private final JedisCluster jedisCluster;

    @PostConstruct
    public void register() {
        log.info("Registering instance in service registry");
        String instance = createInstance(""); // empty state means the instance is starting, by default take the initial status defined in the appProperties
        jedisCluster.hset(appProperties.getConfigurationHash(), appProperties.getInstanceName(), instance);
    }

    public String createInstance(String state) {
        return String.format("{\"name\":\"%s\",\"ip\":\"%s\",\"port\":\"%s\",\"protocol\":\"%s\",\"scheme\":\"%s\",\"apiKey\":\"%s\",\"state\":\"%s\"}", appProperties.getInstanceName(), appProperties.getInstanceIp(), appProperties.getInstancePort(), appProperties.getInstanceProtocol(), appProperties.getInstanceScheme(), appProperties.getInstanceApiKey(), state.isEmpty() ? appProperties.getInstanceInitialStatus() : state);
    }

    @PreDestroy
    public void unregister() {
        log.info("Unregistering instance from service registry");
        String instance = createInstance("STOPPED");
        jedisCluster.hset(appProperties.getConfigurationHash(), appProperties.getInstanceName(), instance);
        jedisCluster.hdel(appProperties.getConfigurationHash(), appProperties.getInstanceName());
    }
}
