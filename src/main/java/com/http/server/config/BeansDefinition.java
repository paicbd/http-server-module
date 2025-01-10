package com.http.server.config;

import com.http.server.utils.AppProperties;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.ServiceProvider;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.Generated;
import com.paicbd.smsc.ws.SocketSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import redis.clients.jedis.JedisCluster;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Generated
@Configuration
@RequiredArgsConstructor
public class BeansDefinition {
    private final AppProperties appProperties;

    @Bean
    public ConcurrentMap<String, ServiceProvider> serviceProviderBySystemIdCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public ConcurrentMap<Integer, ServiceProvider> serviceProviderByNetworkIdCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public ConcurrentMap<Integer, String> systemIdByNetworkIdCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public JedisCluster jedisCluster() {
        return Converter.paramsToJedisCluster(
                new UtilsRecords.JedisConfigParams(appProperties.getRedisNodes(), appProperties.getRedisMaxTotal(),
                        appProperties.getRedisMaxIdle(), appProperties.getRedisMinIdle(),
                        appProperties.isRedisBlockWhenExhausted(), appProperties.getRedisConnectionTimeout(),
                        appProperties.getRedisSoTimeout(), appProperties.getRedisMaxAttempts(),
                        appProperties.getRedisUser(), appProperties.getRedisPassword())
        );
    }

    @Bean
    public SocketSession socketSession() {
        return new SocketSession("sp"); // Service provider
    }

    @Bean
    public CdrProcessor cdrProcessor(JedisCluster jedisCluster) {
        return new CdrProcessor(jedisCluster);
    }
}
