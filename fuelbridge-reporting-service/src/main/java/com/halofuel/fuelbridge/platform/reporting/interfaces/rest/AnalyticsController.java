package com.halofuel.fuelbridge.platform.reporting.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.halofuel.fuelbridge.platform.reporting.application.queryservices.AnalyticsQueryService;
import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetBuyerAnalyticsQuery;
import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetPlatformSummaryQuery;
import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetProviderAnalyticsQuery;
import com.halofuel.fuelbridge.platform.reporting.interfaces.rest.resources.BuyerAnalyticsResource;
import com.halofuel.fuelbridge.platform.reporting.interfaces.rest.resources.PlatformSummaryResource;
import com.halofuel.fuelbridge.platform.reporting.interfaces.rest.resources.ProviderAnalyticsResource;
import com.halofuel.fuelbridge.platform.reporting.interfaces.rest.transform.BuyerAnalyticsResourceFromValueObjectAssembler;
import com.halofuel.fuelbridge.platform.reporting.interfaces.rest.transform.PlatformSummaryResourceFromValueObjectAssembler;
import com.halofuel.fuelbridge.platform.reporting.interfaces.rest.transform.ProviderAnalyticsResourceFromValueObjectAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@io.swagger.v3.oas.annotations.security.SecurityScheme(name = "bearerAuth", type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT emitido por FuelBridge API y validado con AUTHORIZATION_JWT_SECRET.")
@RestController
@RequestMapping(value = "/api/v1/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Analytics", description = "Analytics and reporting endpoints")
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    public AnalyticsController(AnalyticsQueryService analyticsQueryService) {
        this.analyticsQueryService = analyticsQueryService;
    }

    @Operation(
        summary = "Consultar KPIs de la plataforma",
        description = "Calcula en vivo los totales de pedidos, entregas y pagos, los ingresos de pagos COMPLETED, los pedidos PENDING y las entregas DELIVERED. Consulta Ordering, Payment y Fulfillment por HTTP. Devuelve JSON, no PDF.",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.reporting.interfaces.rest.resources.PlatformSummaryResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"totalOrders\":120,\"totalDeliveries\":96,\"totalPayments\":110,\"totalRevenue\":528000,\"pendingOrders\":18,\"completedDeliveries\":90}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: devuelve el JSON de error de Spring Boot mediante /error.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring401", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/v1/analytics/platform\"}") })),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error no controlado: devuelve el JSON de error de Spring Boot del microservicio.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring500", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/v1/analytics/platform\"}") })),
        @ApiResponse(responseCode = "502", description = "El servicio del que depende esta operación falló: devuelve el JSON de error de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring502", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":502,\"error\":\"Bad Gateway\",\"path\":\"/api/v1/analytics/platform\"}") }))
    })
    @GetMapping("/platform")
    public ResponseEntity<PlatformSummaryResource> getPlatformSummary() {
        var summary = analyticsQueryService.handle(new GetPlatformSummaryQuery());
        return new ResponseEntity<>(
                PlatformSummaryResourceFromValueObjectAssembler.toResourceFromValueObject(summary),
                HttpStatus.OK);
    }

    @Operation(
        summary = "Consultar KPIs de un proveedor",
        description = "Calcula pedidos totales, confirmados (CONFIRMED o DELIVERED), cancelados e ingresos de pagos COMPLETED vinculados a sus pedidos. Agrupa ingresos por mes usando paidAt y omite fechas nulas.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "providerId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa proveedora.", example = "202")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.reporting.interfaces.rest.resources.ProviderAnalyticsResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"providerId\":202,\"totalOrders\":24,\"confirmedOrders\":18,\"cancelledOrders\":2,\"totalRevenue\":86400,\"monthlyRevenue\":[{\"month\":\"2026-09\",\"monthIndex\":9,\"amount\":38400},{\"month\":\"2026-10\",\"monthIndex\":10,\"amount\":48000}]}"))),
        @ApiResponse(responseCode = "400", description = "Parámetro o cuerpo HTTP inválido: devuelve el JSON de error de Spring Boot del microservicio.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring400", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":400,\"error\":\"Bad Request\",\"path\":\"/api/v1/analytics/providers/abc\"}") })),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: devuelve el JSON de error de Spring Boot mediante /error.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring401", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/v1/analytics/providers/202\"}") })),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error no controlado: devuelve el JSON de error de Spring Boot del microservicio.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring500", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/v1/analytics/providers/202\"}") })),
        @ApiResponse(responseCode = "502", description = "El servicio del que depende esta operación falló: devuelve el JSON de error de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring502", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":502,\"error\":\"Bad Gateway\",\"path\":\"/api/v1/analytics/providers/202\"}") }))
    })
    @GetMapping("/providers/{providerId}")
    public ResponseEntity<ProviderAnalyticsResource> getProviderAnalytics(@PathVariable Long providerId) {
        var analytics = analyticsQueryService.handle(new GetProviderAnalyticsQuery(providerId));
        return new ResponseEntity<>(
                ProviderAnalyticsResourceFromValueObjectAssembler.toResourceFromValueObject(analytics),
                HttpStatus.OK);
    }

    @Operation(
        summary = "Consultar KPIs de un comprador",
        description = "Calcula pedidos totales, gasto de pagos COMPLETED y conteos de pagos COMPLETED y PENDING. Agrupa el gasto por mes usando paidAt; omite fechas nulas. Sin registros devuelve ceros y una lista mensual vacía.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "companyId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa compradora.", example = "101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.reporting.interfaces.rest.resources.BuyerAnalyticsResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"companyId\":101,\"totalOrders\":12,\"totalSpent\":43200,\"completedPayments\":9,\"pendingPayments\":3,\"monthlySpending\":[{\"month\":\"2026-09\",\"monthIndex\":9,\"amount\":19200},{\"month\":\"2026-10\",\"monthIndex\":10,\"amount\":24000}]}"))),
        @ApiResponse(responseCode = "400", description = "Parámetro o cuerpo HTTP inválido: devuelve el JSON de error de Spring Boot del microservicio.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring400", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":400,\"error\":\"Bad Request\",\"path\":\"/api/v1/analytics/buyers/abc\"}") })),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: devuelve el JSON de error de Spring Boot mediante /error.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring401", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/v1/analytics/buyers/101\"}") })),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error no controlado: devuelve el JSON de error de Spring Boot del microservicio.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring500", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/v1/analytics/buyers/101\"}") })),
        @ApiResponse(responseCode = "502", description = "El servicio del que depende esta operación falló: devuelve el JSON de error de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring502", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":502,\"error\":\"Bad Gateway\",\"path\":\"/api/v1/analytics/buyers/101\"}") }))
    })
    @GetMapping("/buyers/{companyId}")
    public ResponseEntity<BuyerAnalyticsResource> getBuyerAnalytics(@PathVariable Long companyId) {
        var analytics = analyticsQueryService.handle(new GetBuyerAnalyticsQuery(companyId));
        return new ResponseEntity<>(
                BuyerAnalyticsResourceFromValueObjectAssembler.toResourceFromValueObject(analytics),
                HttpStatus.OK);
    }
}
