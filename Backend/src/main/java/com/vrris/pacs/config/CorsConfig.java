package com.vrris.pacs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Frontend (Frontend/vite.config.ts, port 5173) goi truc tiep API backend (port 8080),
 * khong qua proxy -> can mo CORS cho GET trong 3 endpoint tra cuu nay.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PATCH", "PUT", "OPTIONS")
                .allowedHeaders("*");
    }
}
