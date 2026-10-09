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
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para consultar kpis de la plataforma.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite consultar kpis de la plataforma.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: consultar kpis de la plataforma", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores del cliente HTTP actual usan el formato de Spring.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar kpis de la plataforma\",\"details\":\"Ocurrió un error al procesar los datos de reporte de indicadores en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo consultar el servicio requerido por FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores del cliente HTTP actual usan el formato de Spring.", value = "{\"code\":\"BAD_GATEWAY\",\"message\":\"No se pudo consultar el servicio requerido por FuelBridge\",\"details\":\"El servicio remoto no respondió correctamente al intentar consultar kpis de la plataforma.\"}")))
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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Solicitud inválida para consultar kpis de un proveedor", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores del cliente HTTP actual usan el formato de Spring.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para consultar kpis de un proveedor\",\"details\":\"El parámetro providerId=abc no es un identificador numérico válido de reporte de indicadores.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para consultar kpis de un proveedor.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite consultar kpis de un proveedor.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: consultar kpis de un proveedor", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores del cliente HTTP actual usan el formato de Spring.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar kpis de un proveedor\",\"details\":\"Ocurrió un error al procesar los datos de reporte de indicadores en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo consultar el servicio requerido por FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores del cliente HTTP actual usan el formato de Spring.", value = "{\"code\":\"BAD_GATEWAY\",\"message\":\"No se pudo consultar el servicio requerido por FuelBridge\",\"details\":\"El servicio remoto no respondió correctamente al intentar consultar kpis de un proveedor.\"}")))
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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Solicitud inválida para consultar kpis de un comprador", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores del cliente HTTP actual usan el formato de Spring.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para consultar kpis de un comprador\",\"details\":\"El parámetro companyId=abc no es un identificador numérico válido de reporte de indicadores.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para consultar kpis de un comprador.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite consultar kpis de un comprador.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: consultar kpis de un comprador", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores del cliente HTTP actual usan el formato de Spring.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar kpis de un comprador\",\"details\":\"Ocurrió un error al procesar los datos de reporte de indicadores en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo consultar el servicio requerido por FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores del cliente HTTP actual usan el formato de Spring.", value = "{\"code\":\"BAD_GATEWAY\",\"message\":\"No se pudo consultar el servicio requerido por FuelBridge\",\"details\":\"El servicio remoto no respondió correctamente al intentar consultar kpis de un comprador.\"}")))
    })
    @GetMapping("/buyers/{companyId}")
    public ResponseEntity<BuyerAnalyticsResource> getBuyerAnalytics(@PathVariable Long companyId) {
        var analytics = analyticsQueryService.handle(new GetBuyerAnalyticsQuery(companyId));
        return new ResponseEntity<>(
                BuyerAnalyticsResourceFromValueObjectAssembler.toResourceFromValueObject(analytics),
                HttpStatus.OK);
    }
}
