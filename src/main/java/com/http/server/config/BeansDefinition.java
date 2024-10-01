package com.http.server.config;

import com.http.server.utils.AppProperties;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.ws.SocketSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import redis.clients.jedis.JedisCluster;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class BeansDefinition {
    private final AppProperties appProperties;

    @Bean
    public JedisCluster jedisCluster() {
        return Converter.paramsToJedisCluster(getJedisClusterParams(appProperties.getRedisNodes(), appProperties.getRedisMaxTotal(),
                appProperties.getRedisMinIdle(), appProperties.getRedisMaxIdle(), appProperties.isRedisBlockWhenExhausted()));
    }

    @Bean
    public SocketSession socketSession() {
        return new SocketSession("sp"); // Service provider
    }

    private UtilsRecords.JedisConfigParams getJedisClusterParams(List<String> nodes, int maxTotal, int minIdle, int maxIdle, boolean blockWhenExhausted) {
        return new UtilsRecords.JedisConfigParams(nodes, maxTotal, minIdle, maxIdle, blockWhenExhausted);
    }

    @Bean
    public CdrProcessor cdrProcessor(JedisCluster jedisCluster) {
        return new CdrProcessor(jedisCluster);
    }
}
