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
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para consultar kpis de la plataforma.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite consultar kpis de la plataforma.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: consultar kpis de la plataforma", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar kpis de la plataforma\",\"details\":\"Ocurrió un error al procesar los datos de reporte de indicadores en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo consultar el servicio requerido por FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"BAD_GATEWAY\",\"message\":\"No se pudo consultar el servicio requerido por FuelBridge\",\"details\":\"El servicio remoto no respondió correctamente al intentar consultar kpis de la plataforma.\"}"))),
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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Solicitud inválida para consultar kpis de un proveedor", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para consultar kpis de un proveedor\",\"details\":\"El parámetro providerId=abc no es un identificador numérico válido de reporte de indicadores.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para consultar kpis de un proveedor.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite consultar kpis de un proveedor.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: consultar kpis de un proveedor", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar kpis de un proveedor\",\"details\":\"Ocurrió un error al procesar los datos de reporte de indicadores en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo consultar el servicio requerido por FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"BAD_GATEWAY\",\"message\":\"No se pudo consultar el servicio requerido por FuelBridge\",\"details\":\"El servicio remoto no respondió correctamente al intentar consultar kpis de un proveedor.\"}"))),
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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Solicitud inválida para consultar kpis de un comprador", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para consultar kpis de un comprador\",\"details\":\"El parámetro companyId=abc no es un identificador numérico válido de reporte de indicadores.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para consultar kpis de un comprador.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite consultar kpis de un comprador.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: consultar kpis de un comprador", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar kpis de un comprador\",\"details\":\"Ocurrió un error al procesar los datos de reporte de indicadores en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo consultar el servicio requerido por FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"BAD_GATEWAY\",\"message\":\"No se pudo consultar el servicio requerido por FuelBridge\",\"details\":\"El servicio remoto no respondió correctamente al intentar consultar kpis de un comprador.\"}"))),
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
