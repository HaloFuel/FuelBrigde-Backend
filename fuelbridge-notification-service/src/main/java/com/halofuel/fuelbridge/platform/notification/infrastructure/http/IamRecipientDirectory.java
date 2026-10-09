package com.halofuel.fuelbridge.platform.notification.infrastructure.http;

import com.halofuel.fuelbridge.platform.notification.application.outboundservices.RecipientDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/** Uses the existing IAM REST contract; never connects to the monolith database. */
@Component
public class IamRecipientDirectory implements RecipientDirectory {
    private final RestClient client;

    public IamRecipientDirectory(@Value("${iam.service.url}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public Optional<Long> findByCompanyId(Long companyId) {
        return Arrays.stream(users()).filter(u -> Objects.equals(u.companyId(), companyId))
                .map(DirectoryUser::id).findFirst();
    }

    @Override
    public Optional<Long> findByProviderId(Long providerId) {
        return Arrays.stream(users()).filter(u -> Objects.equals(u.providerId(), providerId))
                .map(DirectoryUser::id).findFirst();
    }

    private DirectoryUser[] users() {
        var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        var authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        try {
            var users = client.get().uri("/api/v1/users")
                    .header(HttpHeaders.AUTHORIZATION, authorization).retrieve().body(DirectoryUser[].class);
            if (users == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty IAM response");
            return users;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Recipient directory unavailable", ex);
        }
    }

    public record DirectoryUser(Long id, Long companyId, Long providerId) {}
}
