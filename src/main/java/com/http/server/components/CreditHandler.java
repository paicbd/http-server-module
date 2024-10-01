package com.http.server.components;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.http.server.http.HttpServerManager;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditHandler {
	private final ConcurrentMap<String, Boolean> hasAvailableCredit = new ConcurrentHashMap<>();
	private final HttpServerManager httpServerManager;

	// load the used credits stored in Redis into memory.
	@PostConstruct
	public void init() {
		httpServerManager.getServiceProviderCache().forEach((k,v) -> hasAvailableCredit.put(k, v.getHasAvailableCredit()));
	}

	public boolean hasCredit(String systemId) {
		return Boolean.TRUE.equals(hasAvailableCredit.computeIfAbsent(systemId, this::getCreditConfigured));
	}

	public Boolean getCreditConfigured(String systemId) {
		try {
			//new credit package + available credits + used credits
			return httpServerManager.getServiceProvider(systemId).getHasAvailableCredit();
		} catch (Exception e) {
			return false;
		}
	}

	public void removeFromRedisAndCache(String systemId) {
		hasAvailableCredit.remove(systemId);
	}
}
