package com.http.server.controllers;

import com.http.server.dto.GlobalRecords;
import com.http.server.services.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageControllerTest {

    @Mock
    private MessageService mockSubmitSmService;

    @InjectMocks
    private MessageController messageController;

    private GlobalRecords.MessageRequest messageRequest;

    @Test
    void messageSuccess() {
        when(mockSubmitSmService.message(any())).thenReturn(
                new GlobalRecords.SubmitResponse("httpSp01", "1725031614385-36954524923741")
        );
        Mono<ResponseEntity<GlobalRecords.SubmitResponse>> response = messageController.message(messageRequest);
        StepVerifier.create(response)
                .expectNextMatches(res -> res.getStatusCode() == HttpStatus.OK)
                .verifyComplete();
    }

    @Test
    void messageError() {
        when(mockSubmitSmService.message(any())).thenReturn(
                new GlobalRecords.SubmitResponse("Service provider don't have enough credits")
        );
        Mono<ResponseEntity<GlobalRecords.SubmitResponse>> response = messageController.message(messageRequest);
        StepVerifier.create(response)
                .expectNextMatches(res -> res.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verifyComplete();
    }

}
