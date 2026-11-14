package com.halofuel.fuelbridge.platform.iam.infrastructure.authorization.sfs.pipeline;

import com.halofuel.fuelbridge.platform.iam.application.queryservices.RevokedTokenQueryService;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.IsTokenRevokedQuery;
import com.halofuel.fuelbridge.platform.iam.infrastructure.authorization.sfs.model.UsernamePasswordAuthenticationTokenBuilder;
import com.halofuel.fuelbridge.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class BearerAuthorizationRequestFilter extends OncePerRequestFilter {

    private final BearerTokenService tokenService;
    private final RevokedTokenQueryService revokedTokenQueryService;

    @Qualifier("defaultUserDetailsService")
    private final UserDetailsService userDetailsService;

    public BearerAuthorizationRequestFilter(BearerTokenService tokenService, UserDetailsService userDetailsService,
            RevokedTokenQueryService revokedTokenQueryService) {
        this.tokenService = tokenService;
        this.revokedTokenQueryService = revokedTokenQueryService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = tokenService.getBearerTokenFrom(request);
            // A signed, unexpired token is still rejected once it was revoked at sign-out. Any
            // failure of the blacklist lookup lands in the catch below and leaves the request
            // unauthenticated (fail closed).
            if (token != null && tokenService.validateToken(token)
                    && !revokedTokenQueryService.handle(new IsTokenRevokedQuery(token))) {
                String username = tokenService.getUsernameFromToken(token);
                var userDetails = userDetailsService.loadUserByUsername(username);
                SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationTokenBuilder.build(userDetails, request));
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
