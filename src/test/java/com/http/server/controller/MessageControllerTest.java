package com.http.server.controller;

import com.http.server.dto.GlobalRecords;
import com.http.server.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@WebFluxTest(MessageController.class)
class MessageControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MessageService messageService;

    @Test
    void testMessage_Success() {
        GlobalRecords.MessageRequest messageRequest = new GlobalRecords.MessageRequest(
                "systemId",
                1,
                1,
                "111111",
                1,
                1,
                "222222",
                0,
                120,
                1,
                0,
                0,
                "Hello, World!",
                null,
                null
        );
        GlobalRecords.SubmitResponse submitResponse = new GlobalRecords.SubmitResponse(
                "ok", "1234"
        );

        when(messageService.onMessageReceive(messageRequest)).thenReturn(submitResponse);

        webTestClient.post()
                .uri("/message")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(messageRequest), GlobalRecords.MessageRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GlobalRecords.SubmitResponse.class)
                .isEqualTo(submitResponse);
    }

    @Test
    void testMessage_Error() {
        GlobalRecords.MessageRequest messageRequest = new GlobalRecords.MessageRequest(
                "systemId",
                1,
                1,
                "111111",
                1,
                1,
                "222222",
                0,
                120,
                1,
                0,
                0,
                "Hello, World!",
                null,
                null
        );
        GlobalRecords.SubmitResponse errorResponse = new GlobalRecords.SubmitResponse("Error message");

        when(messageService.onMessageReceive(messageRequest)).thenReturn(errorResponse);

        webTestClient.post()
                .uri("/message")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(messageRequest), GlobalRecords.MessageRequest.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(GlobalRecords.SubmitResponse.class)
                .isEqualTo(errorResponse);
    }
}