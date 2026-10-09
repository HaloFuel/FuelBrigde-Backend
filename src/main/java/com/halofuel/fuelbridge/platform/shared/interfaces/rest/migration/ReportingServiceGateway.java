package com.halofuel.fuelbridge.platform.shared.interfaces.rest.migration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

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

    @Operation(
        summary = "Consultar KPIs de la plataforma",
        description = "Reenvía el JWT al microservicio Reporting y devuelve sus KPIs globales en JSON. Conserva el código HTTP y el cuerpo del servicio.",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(type = "object"), examples = @ExampleObject(name = "respuesta", value = "{\"totalOrders\":120,\"totalDeliveries\":96,\"totalPayments\":110,\"totalRevenue\":528000,\"pendingOrders\":18,\"completedDeliveries\":90}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @GetMapping("/platform")
    public ResponseEntity<byte[]> platform(HttpServletRequest request) {
        return get("/api/v1/analytics/platform", request);
    }

    @Operation(
        summary = "Consultar KPIs de un proveedor",
        description = "Solicita al microservicio Reporting los KPIs del proveedor identificado, incluidos los ingresos mensuales de sus pagos completados.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "providerId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa proveedora.", example = "202")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(type = "object"), examples = @ExampleObject(name = "respuesta", value = "{\"providerId\":202,\"totalOrders\":24,\"confirmedOrders\":18,\"cancelledOrders\":2,\"totalRevenue\":86400,\"monthlyRevenue\":[{\"month\":\"2026-09\",\"monthIndex\":9,\"amount\":38400},{\"month\":\"2026-10\",\"monthIndex\":10,\"amount\":48000}]}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @GetMapping("/providers/{providerId}")
    public ResponseEntity<byte[]> provider(@PathVariable Long providerId, HttpServletRequest request) {
        return get("/api/v1/analytics/providers/" + providerId, request);
    }

    @Operation(
        summary = "Consultar KPIs de un comprador",
        description = "Solicita al microservicio Reporting los KPIs del comprador identificado, incluidos sus gastos mensuales y conteos de pagos.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "companyId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa compradora.", example = "101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(type = "object"), examples = @ExampleObject(name = "respuesta", value = "{\"companyId\":101,\"totalOrders\":12,\"totalSpent\":43200,\"completedPayments\":9,\"pendingPayments\":3,\"monthlySpending\":[{\"month\":\"2026-09\",\"monthIndex\":9,\"amount\":19200},{\"month\":\"2026-10\",\"monthIndex\":10,\"amount\":24000}]}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @GetMapping("/buyers/{companyId}")
    public ResponseEntity<byte[]> buyer(@PathVariable Long companyId, HttpServletRequest request) {
        return get("/api/v1/analytics/buyers/" + companyId, request);
    }

    private ResponseEntity<byte[]> get(String path, HttpServletRequest request) {
        return client.forward(HttpMethod.GET, path, new LinkedMultiValueMap<>(), null, request);
    }
}
