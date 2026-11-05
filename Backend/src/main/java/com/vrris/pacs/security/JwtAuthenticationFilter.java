package com.vrris.pacs.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Doc Bearer token, dat Authentication vao SecurityContext neu token hop le. Khong co token hoac
 * token khong hop le -> khong dat Authentication, de SecurityConfig.anyRequest().authenticated()
 * tra 401 (xem security.RestAuthenticationEntryPoint).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Optional<Claims> claims = jwtService.parse(token);
            claims.ifPresent(c -> SecurityContextHolder.getContext().setAuthentication(toAuthentication(c)));
        }
        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken toAuthentication(Claims claims) {
        String username = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        Collection<GrantedAuthority> authorities = (roles == null ? List.<String>of() : roles).stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }
}
