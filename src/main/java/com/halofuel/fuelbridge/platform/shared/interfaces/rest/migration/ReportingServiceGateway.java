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
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual. Si el 401 proviene del microservicio, el gateway conserva su JSON de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring401", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/v1/analytics/platform\"}") })),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error no controlado: devuelve el JSON de error de Spring Boot del microservicio. El gateway conserva el cuerpo remoto; una excepción local puede usar ErrorResource mediante GlobalExceptionHandler.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring500", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/v1/analytics/platform\"}") })),
        @ApiResponse(responseCode = "502", description = "Si falla la conexión con el microservicio, devuelve un JSON con el único campo message. Si el microservicio responde 502, conserva su cuerpo de error de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "servicioNoDisponible", description = "JSON exacto generado por el gateway cuando RestClient falla.", value = "{\"message\":\"Standalone service unavailable\"}"), @ExampleObject(name = "errorSpring502", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":502,\"error\":\"Bad Gateway\",\"path\":\"/api/v1/analytics/platform\"}") })),
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
        @ApiResponse(responseCode = "400", description = "Parámetro o cuerpo HTTP inválido: devuelve el JSON de error de Spring Boot del microservicio. El gateway conserva el cuerpo remoto; una excepción local puede usar ErrorResource mediante GlobalExceptionHandler.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring400", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":400,\"error\":\"Bad Request\",\"path\":\"/api/v1/analytics/providers/abc\"}") })),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual. Si el 401 proviene del microservicio, el gateway conserva su JSON de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring401", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/v1/analytics/providers/202\"}") })),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error no controlado: devuelve el JSON de error de Spring Boot del microservicio. El gateway conserva el cuerpo remoto; una excepción local puede usar ErrorResource mediante GlobalExceptionHandler.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring500", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/v1/analytics/providers/202\"}") })),
        @ApiResponse(responseCode = "502", description = "Si falla la conexión con el microservicio, devuelve un JSON con el único campo message. Si el microservicio responde 502, conserva su cuerpo de error de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "servicioNoDisponible", description = "JSON exacto generado por el gateway cuando RestClient falla.", value = "{\"message\":\"Standalone service unavailable\"}"), @ExampleObject(name = "errorSpring502", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":502,\"error\":\"Bad Gateway\",\"path\":\"/api/v1/analytics/providers/202\"}") })),
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
        @ApiResponse(responseCode = "400", description = "Parámetro o cuerpo HTTP inválido: devuelve el JSON de error de Spring Boot del microservicio. El gateway conserva el cuerpo remoto; una excepción local puede usar ErrorResource mediante GlobalExceptionHandler.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring400", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":400,\"error\":\"Bad Request\",\"path\":\"/api/v1/analytics/buyers/abc\"}") })),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual. Si el 401 proviene del microservicio, el gateway conserva su JSON de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring401", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/v1/analytics/buyers/101\"}") })),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error no controlado: devuelve el JSON de error de Spring Boot del microservicio. El gateway conserva el cuerpo remoto; una excepción local puede usar ErrorResource mediante GlobalExceptionHandler.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring500", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/v1/analytics/buyers/101\"}") })),
        @ApiResponse(responseCode = "502", description = "Si falla la conexión con el microservicio, devuelve un JSON con el único campo message. Si el microservicio responde 502, conserva su cuerpo de error de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "servicioNoDisponible", description = "JSON exacto generado por el gateway cuando RestClient falla.", value = "{\"message\":\"Standalone service unavailable\"}"), @ExampleObject(name = "errorSpring502", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":502,\"error\":\"Bad Gateway\",\"path\":\"/api/v1/analytics/buyers/101\"}") })),
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
