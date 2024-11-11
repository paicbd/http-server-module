package com.http.server.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.http.server.dto.GlobalRecords;
import com.http.server.utils.AppProperties;
import com.paicbd.smsc.dto.GeneralSettings;
import com.paicbd.smsc.dto.ServiceProvider;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.ws.SocketSession;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import redis.clients.jedis.JedisCluster;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static com.http.server.utils.Constants.WEBSOCKET_STATUS_ENDPOINT;
import static com.http.server.utils.Constants.PARAM_UPDATE_STATUS;
import static com.http.server.utils.Constants.TYPE;
import static com.http.server.utils.Constants.STOPPED;
import static com.http.server.utils.Constants.STARTED;

@Slf4j
@RequiredArgsConstructor
@Component
public class HttpServerManager {
    private final JedisCluster jedisCluster;
    private final AppProperties appProperties;
    private final SocketSession socketSession;
    private final ConcurrentMap<Integer, String> systemIdByNetworkIdCache;
    private final ConcurrentMap<String, ServiceProvider> serviceProviderBySystemIdCache;
    private final ConcurrentMap<Integer, ServiceProvider> serviceProviderByNetworkIdCache;

    private String state;
    @Getter
    private GeneralSettings generalSettings;

    @PostConstruct
    public void initializeCaches() {
        this.manageServerHandler();
        this.loadServiceProviderCache();
        this.loadOrUpdateGeneralSettingsCache();
    }

    public void loadServiceProviderCache() {
        Map<String, String> serviceProvidersHashTable = jedisCluster.hgetAll(appProperties.getServiceProvidersHashTable());
        if (serviceProvidersHashTable.isEmpty()) {
            log.error("Service providers not found in Redis for key {}", appProperties.getServiceProvidersHashTable());
            return;
        }

        serviceProvidersHashTable.forEach((networkId, serviceProviderInRaw) -> {
            try {
                ServiceProvider serviceProvider = Converter.stringToObject(serviceProviderInRaw, ServiceProvider.class);
                Assert.notNull(serviceProvider, "An error occurred while converting the service provider");

                if (!"HTTP".equalsIgnoreCase(serviceProvider.getProtocol())) {
                    log.warn("Service provider with networkId {} is not HTTP, Skipping", serviceProvider.getNetworkId());
                    return;
                }
                serviceProviderBySystemIdCache.put(serviceProvider.getSystemId(), serviceProvider);
                serviceProviderByNetworkIdCache.put(serviceProvider.getNetworkId(), serviceProvider);
                systemIdByNetworkIdCache.put(serviceProvider.getNetworkId(), serviceProvider.getSystemId());
                log.info("Added ServiceProvider to cache: {}", serviceProvider);
            } catch (Exception e) {
                log.error("Error initializing cache for key {}: {}", networkId, e.getMessage());
            }
        });
    }

    public void loadOrUpdateGeneralSettingsCache() {
        String stringGeneralSettings = jedisCluster.hget(appProperties.getHttpGeneralSettingsHash(), appProperties.getHttpGeneralSettingsKey());
        if (stringGeneralSettings == null) {
            log.error("General settings not found in Redis for key {}", appProperties.getHttpGeneralSettingsKey());
            return;
        }

        this.generalSettings = Converter.stringToObject(stringGeneralSettings, GeneralSettings.class);
    }

    public void updateServiceProvider(String stringNetworkId) {
        try {
            String serviceProviderJson = jedisCluster.hget(appProperties.getServiceProvidersHashTable(), stringNetworkId);
            if (serviceProviderJson == null) {
                log.error("Service provider not found in Redis for networkId {}", stringNetworkId);
                return;
            }
            ServiceProvider serviceProviderDTO = Converter.stringToObject(serviceProviderJson, new TypeReference<>() {
            });

            if (!"HTTP".equalsIgnoreCase(serviceProviderDTO.getProtocol())) {
                log.warn("Service provider with networkId {} is not HTTP, Skipping with update", stringNetworkId);
                return;
            }

            updateServiceProviderInCache(serviceProviderDTO);
            if (serviceProviderDTO.getEnabled() == 0) {
                log.warn("Sending websocket notification for service provider with networkId {}", stringNetworkId);
                socketSession.getStompSession().send(WEBSOCKET_STATUS_ENDPOINT, String.format("%s,%s,%s,%s", TYPE, serviceProviderDTO.getNetworkId(), PARAM_UPDATE_STATUS, STOPPED));
            }
        } catch (Exception e) {
            log.error("Error updating service provider {}: {}", stringNetworkId, e.getMessage());
        }
    }

    private void updateServiceProviderInCache(ServiceProvider serviceProvider) {
        serviceProviderBySystemIdCache.put(serviceProvider.getSystemId(), serviceProvider);
        serviceProviderByNetworkIdCache.put(serviceProvider.getNetworkId(), serviceProvider);
        systemIdByNetworkIdCache.put(serviceProvider.getNetworkId(), serviceProvider.getSystemId());
    }

    /**
     * Manages the server handler by retrieving its state from Redis.
     */
    public void manageServerHandler() {
        try {
            String serverHandlerJson = this.jedisCluster.hget(this.appProperties.getConfigurationHash(), this.appProperties.getServerName());
            if (serverHandlerJson == null) {
                log.error("ServerHandler not found in Redis");
            }
            GlobalRecords.ServerHandler serverHandler = Converter.stringToObject(serverHandlerJson, new TypeReference<>() {
            });
            this.state = serverHandler.state();
            log.info("State: {}", this.state);
        } catch (Exception e) {
            log.error("Error on getServerHandler: {}", e.getMessage());
        }
    }

    /**
     * Checks if the server is active.
     *
     * @return true if the server is active, false otherwise
     */
    public boolean isServerActive() {
        return STARTED.equalsIgnoreCase(state);
    }
}
