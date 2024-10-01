package com.http.server.controllers;

import com.http.server.dto.GlobalRecords;
import com.http.server.services.MessageService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @PostMapping("/message")
    public Mono<ResponseEntity<GlobalRecords.SubmitResponse>> message(@RequestBody GlobalRecords.MessageRequest messageRequest) {
        var response = messageService.message(messageRequest);
        if (response.errorMessage().isEmpty()) {
            return Mono.just(new ResponseEntity<>(response, HttpStatus.OK));
        } else {
            return Mono.just(new ResponseEntity<>(response, HttpStatus.BAD_REQUEST));
        }
    }
}
