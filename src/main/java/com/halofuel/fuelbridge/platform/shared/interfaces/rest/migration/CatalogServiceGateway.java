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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content),
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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "409", description = "La operación entra en conflicto con un registro existente o con la disponibilidad del recurso.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content),
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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<byte[]> update(@PathVariable Long id, @RequestBody byte[] body,
                                         HttpServletRequest request) {
        return client.forward(HttpMethod.PUT, "/api/v1/provider-ratings/" + id,
                new LinkedMultiValueMap<>(), body, request);
    }
}
