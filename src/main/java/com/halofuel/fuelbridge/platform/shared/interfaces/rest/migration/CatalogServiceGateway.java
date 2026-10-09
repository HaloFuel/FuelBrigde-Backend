package com.halofuel.fuelbridge.platform.shared.interfaces.rest.migration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

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

    @Operation(
        summary = "Listar calificaciones de proveedores",
        description = "Reenvía al microservicio Catalog los filtros companyId y providerId y devuelve sus calificaciones sin transformar la respuesta.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "companyId", in = ParameterIn.QUERY, description = "Filtro opcional por empresa compradora.", example = "101", schema = @Schema(type = "integer", format = "int64")),
            @Parameter(name = "providerId", in = ParameterIn.QUERY, description = "Filtro opcional por empresa proveedora.", example = "202", schema = @Schema(type = "integer", format = "int64"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(type = "object")), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":1201,\"companyId\":101,\"providerId\":202,\"rating\":5}]"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Solicitud inválida para listar calificaciones de proveedores", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para listar calificaciones de proveedores\",\"details\":\"El parámetro companyId=abc no es un identificador numérico válido de calificación de proveedor.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para listar calificaciones de proveedores.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite listar calificaciones de proveedores.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: listar calificaciones de proveedores", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar calificaciones de proveedores\",\"details\":\"Ocurrió un error al procesar los datos de calificación de proveedor en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo consultar el servicio requerido por FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"BAD_GATEWAY\",\"message\":\"No se pudo consultar el servicio requerido por FuelBridge\",\"details\":\"El servicio remoto no respondió correctamente al intentar listar calificaciones de proveedores.\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @GetMapping
    public ResponseEntity<byte[]> getAll(@RequestParam MultiValueMap<String, String> query,
                                         HttpServletRequest request) {
        return client.forward(HttpMethod.GET, "/api/v1/provider-ratings", query, null, request);
    }

    @Operation(
        summary = "Calificar proveedor",
        description = "Reenvía el JWT y la calificación al microservicio Catalog. rating debe ser un entero de 1 a 5; companyId y providerId identifican empresas existentes. Conserva los errores del servicio.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para calificar proveedor.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object", requiredProperties = { "companyId", "providerId", "rating" }, description = "companyId y providerId identifican empresas; rating es un entero de 1 a 5."),
                examples = { @ExampleObject(name = "principal", value = "{\"companyId\":101,\"providerId\":202,\"rating\":5}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(type = "object"), examples = @ExampleObject(name = "respuesta", value = "{\"id\":1201,\"companyId\":101,\"providerId\":202,\"rating\":5}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "La calificación del proveedor debe estar entre 1 y 5", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"La calificación del proveedor debe estar entre 1 y 5\",\"details\":\"companyId=101, providerId=202, rating=6: la calificación supera el máximo permitido.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para calificar proveedor.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite calificar proveedor.\"}"))),
        @ApiResponse(responseCode = "409", description = "La operación entra en conflicto con un registro existente o con la disponibilidad del recurso.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "La empresa compradora ya calificó a este proveedor", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"PROVIDERRATING_CONFLICT\",\"message\":\"La empresa compradora ya calificó a este proveedor\",\"details\":\"Ya existe una calificación para companyId=101 y providerId=202.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: calificar proveedor", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: calificar proveedor\",\"details\":\"Ocurrió un error al procesar los datos de calificación de proveedor en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo consultar el servicio requerido por FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"BAD_GATEWAY\",\"message\":\"No se pudo consultar el servicio requerido por FuelBridge\",\"details\":\"El servicio remoto no respondió correctamente al intentar calificar proveedor.\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @PostMapping
    public ResponseEntity<byte[]> create(@RequestBody byte[] body, HttpServletRequest request) {
        return client.forward(HttpMethod.POST, "/api/v1/provider-ratings", new LinkedMultiValueMap<>(), body, request);
    }

    @Operation(
        summary = "Actualizar calificación",
        description = "Reenvía al microservicio Catalog la actualización de una calificación. Los identificadores empresariales no pueden cambiar; rating debe ser un entero de 1 a 5.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Identificador del calificación.", example = "1201")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para actualizar calificación.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object", requiredProperties = { "companyId", "providerId", "rating" }, description = "companyId y providerId identifican empresas; rating es un entero de 1 a 5."),
                examples = { @ExampleObject(name = "principal", value = "{\"companyId\":101,\"providerId\":202,\"rating\":4}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(type = "object"), examples = @ExampleObject(name = "respuesta", value = "{\"id\":1201,\"companyId\":101,\"providerId\":202,\"rating\":4}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Datos de calificación inválidos", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Datos de calificación inválidos\",\"details\":\"companyId=101 y providerId=202 deben identificar empresas existentes; rating debe estar entre 1 y 5.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para actualizar calificación.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite actualizar calificación.\"}"))),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Calificación de proveedor no encontrada", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"PROVIDERRATING_NOT_FOUND\",\"message\":\"Calificación de proveedor no encontrada\",\"details\":\"No existe calificación de proveedor con identificador 1201.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: actualizar calificación", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: actualizar calificación\",\"details\":\"Ocurrió un error al procesar los datos de calificación de proveedor en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo consultar el servicio requerido por FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El gateway conserva el cuerpo recibido del microservicio o del manejador HTTP.", value = "{\"code\":\"BAD_GATEWAY\",\"message\":\"No se pudo consultar el servicio requerido por FuelBridge\",\"details\":\"El servicio remoto no respondió correctamente al intentar actualizar calificación.\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<byte[]> update(@PathVariable Long id, @RequestBody byte[] body,
                                         HttpServletRequest request) {
        return client.forward(HttpMethod.PUT, "/api/v1/provider-ratings/" + id,
                new LinkedMultiValueMap<>(), body, request);
    }
}
