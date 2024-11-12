package com.http.server.config;


import com.http.server.http.HttpServerManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebConfig.class)
@ExtendWith(SpringExtension.class)
class WebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HttpServerManager httpServerManager;

    @Test
    void testPreHandle_ServerInactive() throws Exception {
        when(httpServerManager.isServerActive()).thenReturn(false);

        mockMvc.perform(get("/any-path"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Server unavailable at this time."));
    }

    @Test
    void testPreHandle_ServerActive() throws Exception {
        when(httpServerManager.isServerActive()).thenReturn(true);

        mockMvc.perform(get("/unavailable"))
                .andExpect(status().isNotFound());
    }
}