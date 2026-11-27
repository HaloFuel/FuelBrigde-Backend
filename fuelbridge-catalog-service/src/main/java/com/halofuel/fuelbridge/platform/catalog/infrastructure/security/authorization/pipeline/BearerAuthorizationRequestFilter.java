package com.halofuel.fuelbridge.platform.catalog.infrastructure.security.authorization.pipeline;

import com.halofuel.fuelbridge.platform.catalog.infrastructure.security.authorization.model.UsernamePasswordAuthenticationTokenBuilder;
import com.halofuel.fuelbridge.platform.catalog.infrastructure.security.tokens.jwt.BearerTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class BearerAuthorizationRequestFilter extends OncePerRequestFilter {

    private final BearerTokenService tokenService;

    public BearerAuthorizationRequestFilter(BearerTokenService tokenService) {
        this.tokenService = tokenService;

    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = tokenService.getBearerTokenFrom(request);
            if (token != null && tokenService.validateToken(token)) {
                String username = tokenService.getUsernameFromToken(token);
                var userDetails = User.withUsername(username).password("").authorities(java.util.List.of()).build();
                SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationTokenBuilder.build(userDetails, request));
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
