package com.http.server.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.http.server.dto.GlobalRecords;
import com.http.server.utils.AppProperties;
import com.http.server.components.AutoRegister;
import com.paicbd.smsc.dto.GeneralSettings;
import com.paicbd.smsc.dto.ServiceProvider;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.ws.SocketSession;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    @Getter
    private final Map<String, ServiceProvider> serviceProviderCache = new ConcurrentHashMap<>();
    @Getter
    private final Map<Integer, ServiceProvider> serviceProviderByNetworkIdCache = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, GeneralSettings> generalSettingsCache = new ConcurrentHashMap<>();

    @Getter
    GeneralSettings generalSettings;

    private final AutoRegister autoRegister;
    @Setter
    @Getter
    private String state;

    @PostConstruct
    public void initializeCaches() {
        this.autoRegister.register();
        this.manageServerHandler();
        loadServiceProviderCache();
        loadOrUpdateGeneralSettingsCache();
    }

    public void loadServiceProviderCache() {
        loadConfigsToCache(appProperties.getServiceProvidersHashTable(), new TypeReference<ServiceProvider>() {
                },
                (key, data) -> {
                    serviceProviderCache.put(key, data);
                    serviceProviderByNetworkIdCache.put(data.getNetworkId(), data);
                });
    }

    public void loadOrUpdateGeneralSettingsCache() {
        String stringGeneralSettings = jedisCluster.hget(appProperties.getHttpGeneralSettingsHash(), appProperties.getHttpGeneralSettingsKey());
        if (stringGeneralSettings == null) {
            log.error("General settings not found in Redis for key {}", appProperties.getHttpGeneralSettingsKey());
            return;
        }

        this.generalSettings = Converter.stringToObject(stringGeneralSettings, new TypeReference<>() {
        });
    }

    private <T> void loadConfigsToCache(String hashKey, TypeReference<T> typeReference, CacheLoader<T> cacheLoader) {
        Map<String, String> hashValues = jedisCluster.hgetAll(hashKey);
        hashValues.forEach((key, data) -> {
            try {
                T configData = Converter.stringToObject(data, typeReference);
                cacheLoader.addToCache(key, configData);
                log.info("Added {} to cache: {}", typeReference.getType(), configData);
            } catch (Exception e) {
                log.error("Error initializing cache for key {}: {}", key, e.getMessage());
            }
        });
    }

    @FunctionalInterface
    interface CacheLoader<T> {
        void addToCache(String key, T data);
    }

    public void updateServiceProvider(String systemId) {
        log.info("Updating service provider {}", systemId);
        try {
            String serviceProviderJson = jedisCluster.hget(appProperties.getServiceProvidersHashTable(), systemId);
            if (serviceProviderJson == null) {
                log.error("Service provider not found in Redis for systemId {}", systemId);
                return;
            }
            ServiceProvider serviceProviderDTO = Converter.stringToObject(serviceProviderJson, new TypeReference<>() {
            });
            updateServiceProviderInCache(systemId, serviceProviderDTO);
            if (serviceProviderDTO.getEnabled() == 0) {
                log.warn("Sending websocket notification for service provider {}", systemId);
                socketSession.getStompSession().send(WEBSOCKET_STATUS_ENDPOINT, String.format("%s,%s,%s,%s", TYPE, systemId, PARAM_UPDATE_STATUS, STOPPED));
            }
        } catch (Exception e) {
            log.error("Error updating service provider {}: {}", systemId, e.getMessage());
        }
    }

    private void updateServiceProviderInCache(String systemId, ServiceProvider serviceProvider) {
        serviceProviderCache.put(systemId, serviceProvider);
        serviceProviderByNetworkIdCache.put(serviceProvider.getNetworkId(), serviceProvider);
    }

    public ServiceProvider getServiceProvider(String systemId) {
        return serviceProviderCache.get(systemId);
    }

    public ServiceProvider getServiceProviderByNetworkId(Integer networkId) {
        return serviceProviderByNetworkIdCache.get(networkId);
    }

    /**
     * Removes a ServiceProvider configuration from the cache.
     *
     * @param key The key of the configuration to remove
     */
    void removeServiceProviderFromCache(String key) {
        ServiceProvider config = serviceProviderCache.remove(key);
        if (config != null) {
            serviceProviderByNetworkIdCache.values().remove(config);
        }
        log.warn("Removed ServiceProvider configuration with key {}", key);
    }

    public void removeServiceProvider(String systemId) {
        removeServiceProviderFromCache(systemId);
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
            log.info(this.state);
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
        return this.state.equalsIgnoreCase(STARTED);
    }
}
