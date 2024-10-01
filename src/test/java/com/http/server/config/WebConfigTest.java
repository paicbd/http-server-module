package com.http.server.config;

import com.http.server.http.HttpServerManager;
import com.http.server.utils.RestInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebConfigTest {

    @Mock
    private HttpServerManager mockHttpServerManager;

    @Mock
    private InterceptorRegistry mockInterceptorRegistry;

    private WebConfig webConfig;

    @BeforeEach
    void setUp() {
        webConfig = new WebConfig(mockHttpServerManager);
    }

    @Test
    void addInterceptors() {
        InterceptorRegistration mockRegistration = mock(InterceptorRegistration.class);
        when(mockInterceptorRegistry.addInterceptor(any(RestInterceptor.class))).thenReturn(mockRegistration);
        webConfig.addInterceptors(mockInterceptorRegistry);
        verify(mockInterceptorRegistry).addInterceptor(any(RestInterceptor.class));
    }
}
