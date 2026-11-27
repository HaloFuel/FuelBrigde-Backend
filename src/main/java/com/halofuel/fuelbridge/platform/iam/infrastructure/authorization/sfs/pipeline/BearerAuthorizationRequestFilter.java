package com.halofuel.fuelbridge.platform.iam.infrastructure.authorization.sfs.pipeline;

import com.halofuel.fuelbridge.platform.iam.application.queryservices.RevokedTokenQueryService;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.IsTokenRevokedQuery;
import com.halofuel.fuelbridge.platform.iam.domain.model.valueobjects.ServiceIdentity;
import com.halofuel.fuelbridge.platform.iam.infrastructure.authorization.sfs.model.UsernamePasswordAuthenticationTokenBuilder;
import com.halofuel.fuelbridge.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class BearerAuthorizationRequestFilter extends OncePerRequestFilter {

    /** Authority granted to service callers; distinct from every user role. */
    public static final String SERVICE_AUTHORITY = "ROLE_SERVICE";

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
            // The signature and expiry check is the only gate for every token. The subject prefix
            // below only chooses how to build the authentication AFTER the token is proven valid.
            if (token != null && tokenService.validateToken(token)) {
                String subject = tokenService.getUsernameFromToken(token);
                if (ServiceIdentity.isServiceSubject(subject)) {
                    // Service token: no user lookup and no revocation list (it only holds user sessions).
                    var servicePrincipal = User.withUsername(subject).password("")
                            .authorities(new SimpleGrantedAuthority(SERVICE_AUTHORITY)).build();
                    SecurityContextHolder.getContext().setAuthentication(
                            UsernamePasswordAuthenticationTokenBuilder.build(servicePrincipal, request));
                } else if (!revokedTokenQueryService.handle(new IsTokenRevokedQuery(token))) {
                    // User token: a signed, unexpired token is still rejected once it was revoked at
                    // sign-out. Any failure of the blacklist lookup lands in the catch below.
                    var userDetails = userDetailsService.loadUserByUsername(subject);
                    SecurityContextHolder.getContext().setAuthentication(
                            UsernamePasswordAuthenticationTokenBuilder.build(userDetails, request));
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
