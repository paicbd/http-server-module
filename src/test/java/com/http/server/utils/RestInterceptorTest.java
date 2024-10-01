package com.http.server.utils;

import com.http.server.http.HttpServerManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RestInterceptorTest {

    @Mock
    private HttpServerManager mockHttpServerManager;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    private RestInterceptor restInterceptor;

    @BeforeEach
    void setUp() {
        restInterceptor = new RestInterceptor(mockHttpServerManager);
    }

    @Test
    void preHandle_serverActive() throws Exception {
        when(mockHttpServerManager.isServerActive()).thenReturn(true);
        Object mockHandler = new Object();
        boolean result = restInterceptor.preHandle(mockRequest, mockResponse, mockHandler);
        assertTrue(result);
        verify(mockResponse, never()).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    @Test
    void preHandle_serverInactive() throws Exception {
        when(mockHttpServerManager.isServerActive()).thenReturn(false);
        PrintWriter mockWriter = mock(PrintWriter.class);
        when(mockResponse.getWriter()).thenReturn(mockWriter);

        Object mockHandler = new Object();
        boolean result = restInterceptor.preHandle(mockRequest, mockResponse, mockHandler);

        assertFalse(result);
        verify(mockResponse).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(mockWriter).write("Server unavailable at this time.");
        verify(mockWriter).flush();
        verify(mockWriter).close();
    }

    @Test
    void preHandle_nullParameters() {
        assertThrows(Exception.class, () ->
                restInterceptor.preHandle(null, mockResponse, new Object())
        );

        assertThrows(Exception.class, () ->
                restInterceptor.preHandle(mockRequest, null, new Object())
        );

        assertThrows(Exception.class, () ->
                restInterceptor.preHandle(mockRequest, mockResponse, null)
        );

        verifyNoInteractions(mockHttpServerManager);
    }
}
