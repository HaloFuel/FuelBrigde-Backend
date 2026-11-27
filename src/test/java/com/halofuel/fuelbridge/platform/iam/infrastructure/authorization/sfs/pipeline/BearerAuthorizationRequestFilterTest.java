package com.halofuel.fuelbridge.platform.iam.infrastructure.authorization.sfs.pipeline;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.halofuel.fuelbridge.platform.iam.application.queryservices.RevokedTokenQueryService;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.IsTokenRevokedQuery;
import com.halofuel.fuelbridge.platform.iam.infrastructure.tokens.jwt.services.TokenServiceImpl;
import com.halofuel.fuelbridge.platform.shared.infrastructure.security.ServiceTokenIssuer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;
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
import java.util.Base64;
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
 * que la firma sigue siendo obligatoria y que los tokens de usuario conservan su flujo. Los rechazos se
 * distinguen por la causa que registra TokenServiceImpl (firma inválida, expirado, sin firma, mal formado),
 * no solo por la ausencia de autenticación, que cualquier excepción produciría.
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
    private Logger tokenServiceLogger;
    private ListAppender<ILoggingEvent> tokenServiceLog;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userDetailsService = mock(UserDetailsService.class);
        revokedTokenQueryService = mock(RevokedTokenQueryService.class);
        tokenService = new TokenServiceImpl();
        ReflectionTestUtils.setField(tokenService, "secret", SECRET);
        ReflectionTestUtils.setField(tokenService, "expirationDays", 7);
        filter = new BearerAuthorizationRequestFilter(tokenService, userDetailsService, revokedTokenQueryService);
        tokenServiceLogger = (Logger) LoggerFactory.getLogger(TokenServiceImpl.class);
        tokenServiceLog = new ListAppender<>();
        tokenServiceLog.start();
        tokenServiceLogger.addAppender(tokenServiceLog);
    }

    @AfterEach
    void tearDown() {
        tokenServiceLogger.detachAppender(tokenServiceLog);
        SecurityContextHolder.clearContext();
    }

    private void runFilterWith(String token) throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    /** Messages TokenServiceImpl logged while validating; each rejection cause has its own prefix. */
    private List<String> rejectionReasons() {
        return tokenServiceLog.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private String unsignedToken(String subject) {
        var now = System.currentTimeMillis();
        // No signWith(): the JWT header says alg=none and the signature segment is empty.
        return Jwts.builder().subject(subject).issuedAt(new Date(now)).expiration(new Date(now + 60_000)).compact();
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
        assertThat(rejectionReasons()).singleElement().asString().startsWith("Invalid JWT signature");
        verifyNoInteractions(userDetailsService, revokedTokenQueryService);
    }

    @Test
    void expiredServiceTokenDoesNotAuthenticate() throws Exception {
        var now = System.currentTimeMillis();
        var expired = tokenSignedWith(SECRET, SERVICE_SUBJECT, new Date(now - 120_000), new Date(now - 60_000));

        runFilterWith(expired);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // Correctly signed, so the only reason left is the expiry: not a signature failure.
        assertThat(rejectionReasons()).singleElement().asString().startsWith("JWT expired");
        verifyNoInteractions(userDetailsService, revokedTokenQueryService);
    }

    @Test
    void unsignedServiceTokenWithAlgNoneDoesNotAuthenticate() throws Exception {
        runFilterWith(unsignedToken(SERVICE_SUBJECT));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(rejectionReasons()).singleElement().asString().startsWith("JWT unsupported");
        verifyNoInteractions(userDetailsService, revokedTokenQueryService);
    }

    @Test
    void unsignedUserTokenDoesNotAuthenticateEither() throws Exception {
        runFilterWith(unsignedToken("compras.costasur"));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(rejectionReasons()).singleElement().asString().startsWith("JWT unsupported");
        verifyNoInteractions(userDetailsService, revokedTokenQueryService);
    }

    @Test
    void serviceTokenWithItsSignatureStrippedDoesNotAuthenticate() throws Exception {
        var valid = new ServiceTokenIssuer(SECRET).issueFor(SERVICE_SUBJECT);
        var stripped = valid.substring(0, valid.lastIndexOf('.') + 1);

        runFilterWith(stripped);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(rejectionReasons()).singleElement().asString()
                .startsWith("Invalid JWT: ").contains("missing the required signature");
        verifyNoInteractions(userDetailsService, revokedTokenQueryService);
    }

    @Test
    void serviceTokenWithAForgedSubjectAndTheOriginalSignatureDoesNotAuthenticate() throws Exception {
        var user = tokenService.generateToken("compras.costasur");
        var parts = user.split("\\.");
        var forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + SERVICE_SUBJECT + "\",\"iat\":1,\"exp\":4102444800}")
                        .getBytes(StandardCharsets.UTF_8));

        runFilterWith(parts[0] + "." + forgedPayload + "." + parts[2]);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(rejectionReasons()).singleElement().asString().startsWith("Invalid JWT signature");
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
