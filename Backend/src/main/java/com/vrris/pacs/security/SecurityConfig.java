package com.vrris.pacs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Khung bao mat toi thieu cho IN-13/IN-14 - stateless JWT, chua co login endpoint (sinh token cho
 * test qua security.JwtService truc tiep). Tat ca request can dang nhap; vai tro cu the do
 * @PreAuthorize tren tung controller method quyet dinh (xem rest/*.java), khong phai o day.
 *
 * permitAll() cho OPTIONS: config.CorsConfig dang la WebMvcConfigurer (chay trong DispatcherServlet,
 * SAU filter chain cua Spring Security) - neu khong permitAll rieng, request OPTIONS preflight cua
 * browser (khong co header Authorization) se bi chan 401 TRUOC KHI toi duoc CORS interceptor, lam
 * Frontend khong goi API duoc tu port khac. Day la quyet dinh ky thuat nho, chua ghi vao
 * doc/15-kien-truc-backend.md.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                           RestAuthenticationEntryPoint authenticationEntryPoint,
                           RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // k8s readiness/liveness probe (doc/14-devops-trien-khai.md muc 4) - probe cua
                        // kubelet khong co JWT. Chi permitAll "health" (da gioi han o application.yml,
                        // management.endpoints.web.exposure.include=health), khong mo toan bo actuator.
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
