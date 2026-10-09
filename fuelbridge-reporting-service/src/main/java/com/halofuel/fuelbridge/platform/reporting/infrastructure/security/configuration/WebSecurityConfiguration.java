package com.halofuel.fuelbridge.platform.reporting.infrastructure.security.configuration;

import com.halofuel.fuelbridge.platform.reporting.infrastructure.security.authorization.pipeline.BearerAuthorizationRequestFilter;
import com.halofuel.fuelbridge.platform.reporting.infrastructure.security.tokens.jwt.BearerTokenService;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class WebSecurityConfiguration {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, BearerTokenService tokenService,
                                   AuthenticationEntryPoint entryPoint, @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        http.cors(config -> config.configurationSource(cors))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new BearerAuthorizationRequestFilter(tokenService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
