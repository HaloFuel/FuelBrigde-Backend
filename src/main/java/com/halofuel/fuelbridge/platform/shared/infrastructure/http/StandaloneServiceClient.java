package com.halofuel.fuelbridge.platform.shared.infrastructure.http;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.nio.charset.StandardCharsets;

/** HTTP boundary for capabilities extracted from this API. */
public class StandaloneServiceClient {
    private final RestClient client;

    protected StandaloneServiceClient(String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public ResponseEntity<byte[]> forward(HttpMethod method, String path,
                                          MultiValueMap<String, String> query, byte[] body,
                                          HttpServletRequest request) {
        var outgoing = client.method(method).uri(uri -> uri.path(path).queryParams(query).build());
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null) outgoing.header(HttpHeaders.AUTHORIZATION, authorization);
        if (body != null) {
            var contentType = request.getContentType();
            if (contentType != null) outgoing.header(HttpHeaders.CONTENT_TYPE, contentType);
            outgoing.body(body);
        }
        try {
            // exchange preserves non-2xx responses instead of replacing business errors.
            return outgoing.exchange((sent, received) -> {
                var headers = new HttpHeaders();
                var contentType = received.getHeaders().getContentType();
                if (contentType != null) headers.setContentType(contentType);
                var disposition = received.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
                if (disposition != null) headers.set(HttpHeaders.CONTENT_DISPOSITION, disposition);
                return new ResponseEntity<>(received.getBody().readAllBytes(), headers, received.getStatusCode());
            });
        } catch (RestClientException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"Standalone service unavailable\"}".getBytes(StandardCharsets.UTF_8));
        }
    }
}
