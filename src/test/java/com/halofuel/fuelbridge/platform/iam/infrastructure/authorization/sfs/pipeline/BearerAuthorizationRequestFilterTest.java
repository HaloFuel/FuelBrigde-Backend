package com.halofuel.fuelbridge.platform.iam.infrastructure.authorization.sfs.pipeline;

import com.halofuel.fuelbridge.platform.iam.application.queryservices.RevokedTokenQueryService;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.IsTokenRevokedQuery;
import com.halofuel.fuelbridge.platform.iam.infrastructure.tokens.jwt.services.TokenServiceImpl;
import com.halofuel.fuelbridge.platform.shared.infrastructure.security.ServiceTokenIssuer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @summary Pruebas unitarias de BearerAuthorizationRequestFilter.
 * @remarks Usa el TokenServiceImpl real para que la firma y la expiración se validen de verdad, y mocks
 * del UserDetailsService y de la consulta de tokens revocados. Verifica que los tokens de servicio
 * (subject con prefijo fuelbridge-) autentican como ROLE_SERVICE sin consultar usuarios ni revocados,
 * que la firma sigue siendo obligatoria y que los tokens de usuario conservan su flujo.
 * @author HaloFuel
 */
class BearerAuthorizationRequestFilterTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes-of-length!!";
    private static final String OTHER_SECRET = "another-secret-with-at-least-32-bytes-long!";
    private static final String SERVICE_SUBJECT = "fuelbridge-ordering";

    private UserDetailsService userDetailsService;
    private RevokedTokenQueryService revokedTokenQueryService;
    private TokenServiceImpl tokenService;
    private BearerAuthorizationRequestFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userDetailsService = mock(UserDetailsService.class);
        revokedTokenQueryService = mock(RevokedTokenQueryService.class);
        tokenService = new TokenServiceImpl();
        ReflectionTestUtils.setField(tokenService, "secret", SECRET);
        ReflectionTestUtils.setField(tokenService, "expirationDays", 7);
        filter = new BearerAuthorizationRequestFilter(tokenService, userDetailsService, revokedTokenQueryService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void runFilterWith(String token) throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    private String tokenSignedWith(String secret, String subject, Date issuedAt, Date expiration) {
        return Jwts.builder().subject(subject).issuedAt(issuedAt).expiration(expiration)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }

    @Test
    void validServiceTokenAuthenticatesAsServiceWithoutTouchingUsersOrRevokedTokens() throws Exception {
        var token = new ServiceTokenIssuer(SECRET).issueFor(SERVICE_SUBJECT);

        runFilterWith(token);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo(SERVICE_SUBJECT);
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_SERVICE");
        verifyNoInteractions(userDetailsService, revokedTokenQueryService);
    }

    @Test
    void serviceTokenWithInvalidSignatureDoesNotAuthenticate() throws Exception {
        var now = new Date();
        var forged = tokenSignedWith(OTHER_SECRET, SERVICE_SUBJECT, now, new Date(now.getTime() + 60_000));

        runFilterWith(forged);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService, revokedTokenQueryService);
    }

    @Test
    void expiredServiceTokenDoesNotAuthenticate() throws Exception {
        var now = System.currentTimeMillis();
        var expired = tokenSignedWith(SECRET, SERVICE_SUBJECT, new Date(now - 120_000), new Date(now - 60_000));

        runFilterWith(expired);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService, revokedTokenQueryService);
    }

    @Test
    void validUserTokenLoadsTheUserAndChecksRevokedTokens() throws Exception {
        var token = tokenService.generateToken("compras.costasur");
        var userDetails = User.withUsername("compras.costasur").password("hash").authorities("ROLE_BUYER").build();
        when(revokedTokenQueryService.handle(any(IsTokenRevokedQuery.class))).thenReturn(false);
        when(userDetailsService.loadUserByUsername("compras.costasur")).thenReturn(userDetails);

        runFilterWith(token);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("compras.costasur");
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_BUYER");
        verify(revokedTokenQueryService).handle(new IsTokenRevokedQuery(token));
        verify(userDetailsService).loadUserByUsername("compras.costasur");
    }

    @Test
    void revokedUserTokenIsRejectedWithoutLoadingTheUser() throws Exception {
        var token = tokenService.generateToken("compras.costasur");
        when(revokedTokenQueryService.handle(any(IsTokenRevokedQuery.class))).thenReturn(true);

        runFilterWith(token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(revokedTokenQueryService).handle(new IsTokenRevokedQuery(token));
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void userTokenWhoseUserDoesNotExistStaysUnauthenticated() throws Exception {
        var token = tokenService.generateToken("ghost");
        when(revokedTokenQueryService.handle(any(IsTokenRevokedQuery.class))).thenReturn(false);
        when(userDetailsService.loadUserByUsername("ghost")).thenThrow(new UsernameNotFoundException("ghost"));

        runFilterWith(token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
