package com.http.server.utils;

import com.http.server.dto.GlobalRecords;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SendRequestHttp {
	private final RestTemplate restTemplate = new RestTemplate();
	
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
}
