package com.halofuel.fuelbridge.platform.equipment.infrastructure.http;

import com.halofuel.fuelbridge.platform.equipment.application.outboundservices.ProviderDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/** Checks provider companies through IAM's REST contract, forwarding the caller's JWT. */
@Component
public class IamProviderDirectory implements ProviderDirectory {
    private final RestClient client;

    public IamProviderDirectory(@Value("${fuelbridge.api.url}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public boolean providerExists(Long providerId) {
        var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        var authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        try {
            client.get().uri("/api/v1/provider-companies/{id}", providerId)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) return false;
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Provider directory unavailable", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Provider directory unavailable", ex);
        }
    }
}
