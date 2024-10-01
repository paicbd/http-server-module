package com.http.server.utils;

import com.http.server.http.HttpServerManager;
import lombok.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * RestInterceptor is a class that implements the HandlerInterceptor interface to intercept incoming HTTP requests
 * and check if the server is active before processing the request.
 */
@RequiredArgsConstructor
public class RestInterceptor implements HandlerInterceptor {

	private final HttpServerManager httpServerManager;
	
	/**
     * Pre-handle method invoked before handling the request.
     *
     * @param request  The HTTP request
     * @param response The HTTP response
     * @param handler  The handler object
     * @return {@code true} if the request should proceed, {@code false} otherwise
     * @throws Exception if an error occurs during pre-handling
     */
	@Override
    public boolean preHandle(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response,@NonNull Object handler)
            throws Exception {
		if (!httpServerManager.isServerActive()) {
	        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
	        response.getWriter().write("Server unavailable at this time.");
	        response.getWriter().flush();
	        response.getWriter().close();
	        return false;
	    }
		
        return true;
    }
}
