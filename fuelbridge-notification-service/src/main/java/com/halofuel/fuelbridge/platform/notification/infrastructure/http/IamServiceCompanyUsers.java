package com.halofuel.fuelbridge.platform.notification.infrastructure.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.halofuel.fuelbridge.platform.notification.application.outboundservices.CompanyUsers;
import com.halofuel.fuelbridge.platform.notification.infrastructure.security.configuration.ServiceTokenConfiguration;
import com.halofuel.fuelbridge.platform.shared.infrastructure.security.ServiceTokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

/**
 * Reads GET /api/v1/users/company/{companyId} from Identity &amp; Access with a service
 * token, the same contract and pattern that
 * {@code fuelbridge-ordering-service}'s {@code IamServiceCompanyUsers} already uses. IAM
 * answers the company's user, or 404 when the company has none.
 */
@Component
public class IamServiceCompanyUsers implements CompanyUsers {
    private final ServiceTokenIssuer serviceTokens;
    private final RestClient client;

    public IamServiceCompanyUsers(ServiceTokenIssuer serviceTokens,
                                  @Value("${iam.service.url}") String baseUrl) {
        this.serviceTokens = serviceTokens;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public Optional<Long> findUserIdByCompanyId(Long companyId) {
        String jwt = serviceTokens.issueFor(ServiceTokenConfiguration.SERVICE_NAME);
        try {
            var user = client.get().uri("/api/v1/users/company/{companyId}", companyId)
                    .headers(headers -> headers.setBearerAuth(jwt))
                    .retrieve().body(CompanyUserResponse.class);
            return Optional.ofNullable(user).map(CompanyUserResponse::id);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) return Optional.empty();
            throw new IllegalStateException("Identity and Access answered " + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Identity and Access service unavailable", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompanyUserResponse(Long id) {}
}
