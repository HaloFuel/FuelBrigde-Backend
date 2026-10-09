package com.halofuel.fuelbridge.platform.shared.interfaces.rest.migration;

import com.halofuel.fuelbridge.platform.shared.infrastructure.http.ReportingServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;

/** Retains analytics URLs while all calculations live in reporting-service. */
@RestController
@RequestMapping("/api/v1/analytics")
public class ReportingServiceGateway {
    private final ReportingServiceClient client;

    public ReportingServiceGateway(ReportingServiceClient client) {
        this.client = client;
    }

    @GetMapping("/platform")
    public ResponseEntity<byte[]> platform(HttpServletRequest request) {
        return get("/api/v1/analytics/platform", request);
    }

    @GetMapping("/providers/{providerId}")
    public ResponseEntity<byte[]> provider(@PathVariable Long providerId, HttpServletRequest request) {
        return get("/api/v1/analytics/providers/" + providerId, request);
    }

    @GetMapping("/buyers/{companyId}")
    public ResponseEntity<byte[]> buyer(@PathVariable Long companyId, HttpServletRequest request) {
        return get("/api/v1/analytics/buyers/" + companyId, request);
    }

    private ResponseEntity<byte[]> get(String path, HttpServletRequest request) {
        return client.forward(HttpMethod.GET, path, new LinkedMultiValueMap<>(), null, request);
    }
}
