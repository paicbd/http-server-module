package com.http.server.config;

import com.http.server.http.HttpServerManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.http.server.utils.RestInterceptor;

/**
 * Configuration class for web-related settings.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
	private final HttpServerManager httpServerManager;
	
	/**
     * Adds interceptors for handling web requests.
     *
     * @param registry The InterceptorRegistry instance
     */
	@Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RestInterceptor(httpServerManager))
                .addPathPatterns("/**");
    }
}
