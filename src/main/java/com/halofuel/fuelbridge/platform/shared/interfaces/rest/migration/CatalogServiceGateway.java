package com.halofuel.fuelbridge.platform.shared.interfaces.rest.migration;

import com.halofuel.fuelbridge.platform.shared.infrastructure.http.CatalogServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

/** Retains the public ratings routes while the standalone service owns the BC. */
@RestController
@RequestMapping("/api/v1/provider-ratings")
public class CatalogServiceGateway {
    private final CatalogServiceClient client;

    public CatalogServiceGateway(CatalogServiceClient client) {
        this.client = client;
    }

    @GetMapping
    public ResponseEntity<byte[]> getAll(@RequestParam MultiValueMap<String, String> query,
                                         HttpServletRequest request) {
        return client.forward(HttpMethod.GET, "/api/v1/provider-ratings", query, null, request);
    }

    @PostMapping
    public ResponseEntity<byte[]> create(@RequestBody byte[] body, HttpServletRequest request) {
        return client.forward(HttpMethod.POST, "/api/v1/provider-ratings", new LinkedMultiValueMap<>(), body, request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<byte[]> update(@PathVariable Long id, @RequestBody byte[] body,
                                         HttpServletRequest request) {
        return client.forward(HttpMethod.PUT, "/api/v1/provider-ratings/" + id,
                new LinkedMultiValueMap<>(), body, request);
    }
}
