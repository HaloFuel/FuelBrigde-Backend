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
        @ApiResponse(responseCode = "400", description = "Parámetro o cuerpo HTTP inválido: devuelve el JSON de error de Spring Boot del microservicio. El gateway conserva el cuerpo remoto; una excepción local puede usar ErrorResource mediante GlobalExceptionHandler.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring400", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":400,\"error\":\"Bad Request\",\"path\":\"/api/v1/provider-ratings\"}") })),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual. Si el 401 proviene del microservicio, el gateway conserva su JSON de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring401", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/v1/provider-ratings\"}") })),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error no controlado: devuelve el JSON de error de Spring Boot del microservicio. El gateway conserva el cuerpo remoto; una excepción local puede usar ErrorResource mediante GlobalExceptionHandler.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring500", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/v1/provider-ratings\"}") })),
        @ApiResponse(responseCode = "502", description = "Si falla la conexión con el microservicio, devuelve un JSON con el único campo message. Si el microservicio responde 502, conserva su cuerpo de error de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "servicioNoDisponible", description = "JSON exacto generado por el gateway cuando RestClient falla.", value = "{\"message\":\"Standalone service unavailable\"}"), @ExampleObject(name = "errorSpring502", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":502,\"error\":\"Bad Gateway\",\"path\":\"/api/v1/provider-ratings\"}") })),
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
        @ApiResponse(responseCode = "400", description = "Validación rechazada: devuelve texto literal con Content-Type application/json, sin serialización JSON. Los errores de lectura o conversión HTTP del microservicio usan el JSON de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "calificacionInvalida", description = "Texto literal si faltan identificadores o rating está fuera de 1 a 5.", value = "A valid company, provider and rating from 1 to 5 are required"), @ExampleObject(name = "empresaInexistente", description = "Texto literal cuando IAM indica que una empresa no existe.", value = "Buyer company or provider does not exist"), @ExampleObject(name = "errorSpring400", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":400,\"error\":\"Bad Request\",\"path\":\"/api/v1/provider-ratings\"}") })),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual. Si el 401 proviene del microservicio, el gateway conserva su JSON de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring401", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/v1/provider-ratings\"}") })),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "409", description = "La empresa compradora ya calificó al proveedor: devuelve texto literal con Content-Type application/json, sin serialización JSON.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "calificacionDuplicada", description = "Texto literal devuelto por el controlador de catalog y transmitido sin cambios por el gateway.", value = "The buyer company already rated this provider") })),
        @ApiResponse(responseCode = "500", description = "Error no controlado: devuelve el JSON de error de Spring Boot del microservicio. El gateway conserva el cuerpo remoto; una excepción local puede usar ErrorResource mediante GlobalExceptionHandler.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring500", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/v1/provider-ratings\"}") })),
        @ApiResponse(responseCode = "502", description = "Si falla la conexión con el microservicio, devuelve un JSON con el único campo message. Si el microservicio responde 502, conserva su cuerpo de error de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "servicioNoDisponible", description = "JSON exacto generado por el gateway cuando RestClient falla.", value = "{\"message\":\"Standalone service unavailable\"}"), @ExampleObject(name = "errorSpring502", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":502,\"error\":\"Bad Gateway\",\"path\":\"/api/v1/provider-ratings\"}") })),
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
        @ApiResponse(responseCode = "400", description = "Validación rechazada: devuelve texto literal con Content-Type application/json, sin serialización JSON. Los errores de lectura o conversión HTTP del microservicio usan el JSON de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "calificacionInvalida", description = "Texto literal si faltan identificadores o rating está fuera de 1 a 5.", value = "A valid company, provider and rating from 1 to 5 are required"), @ExampleObject(name = "empresaInexistente", description = "Texto literal cuando IAM indica que una empresa no existe.", value = "Buyer company or provider does not exist"), @ExampleObject(name = "empresasNoModificables", description = "Texto literal cuando se intenta modificar companyId o providerId del registro.", value = "Company and provider cannot be changed"), @ExampleObject(name = "errorSpring400", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":400,\"error\":\"Bad Request\",\"path\":\"/api/v1/provider-ratings/1201\"}") })),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual. Si el 401 proviene del microservicio, el gateway conserva su JSON de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring401", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/v1/provider-ratings/1201\"}") })),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado: sin cuerpo de respuesta.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error no controlado: devuelve el JSON de error de Spring Boot del microservicio. El gateway conserva el cuerpo remoto; una excepción local puede usar ErrorResource mediante GlobalExceptionHandler.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring500", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/v1/provider-ratings/1201\"}") })),
        @ApiResponse(responseCode = "502", description = "Si falla la conexión con el microservicio, devuelve un JSON con el único campo message. Si el microservicio responde 502, conserva su cuerpo de error de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "servicioNoDisponible", description = "JSON exacto generado por el gateway cuando RestClient falla.", value = "{\"message\":\"Standalone service unavailable\"}"), @ExampleObject(name = "errorSpring502", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":502,\"error\":\"Bad Gateway\",\"path\":\"/api/v1/provider-ratings/1201\"}") })),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<byte[]> update(@PathVariable Long id, @RequestBody byte[] body,
                                         HttpServletRequest request) {
        return client.forward(HttpMethod.PUT, "/api/v1/provider-ratings/" + id,
                new LinkedMultiValueMap<>(), body, request);
    }
}
