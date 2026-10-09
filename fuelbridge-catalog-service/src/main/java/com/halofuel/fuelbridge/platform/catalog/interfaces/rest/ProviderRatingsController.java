package com.halofuel.fuelbridge.platform.catalog.interfaces.rest;

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

import com.halofuel.fuelbridge.platform.catalog.domain.model.aggregates.ProviderRating;
import com.halofuel.fuelbridge.platform.catalog.domain.repositories.ProviderRatingRepository;
import com.halofuel.fuelbridge.platform.catalog.interfaces.rest.resources.ProviderRatingResource;
import com.halofuel.fuelbridge.platform.catalog.application.outboundservices.CompanyDirectory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@io.swagger.v3.oas.annotations.security.SecurityScheme(name = "bearerAuth", type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT emitido por FuelBridge API y validado con AUTHORIZATION_JWT_SECRET.")
@RestController
@RequestMapping("/api/v1/provider-ratings")
public class ProviderRatingsController {

    private final ProviderRatingRepository ratingRepository;
    private final CompanyDirectory companyDirectory;


    public ProviderRatingsController(ProviderRatingRepository ratingRepository,
                                     CompanyDirectory companyDirectory) {
        this.ratingRepository = ratingRepository;
        this.companyDirectory = companyDirectory;

    }

    @Operation(
        summary = "Listar calificaciones de proveedores",
        description = "Devuelve las calificaciones registradas. companyId y providerId son filtros opcionales y se pueden combinar; sin filtros devuelve todas.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "companyId", in = ParameterIn.QUERY, required = false, description = "Filtro opcional por empresa compradora.", example = "101"),
            @Parameter(name = "providerId", in = ParameterIn.QUERY, required = false, description = "Filtro opcional por empresa proveedora.", example = "202")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.catalog.interfaces.rest.resources.ProviderRatingResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":1201,\"companyId\":101,\"providerId\":202,\"rating\":5}]"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Solicitud inválida para listar calificaciones de proveedores", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El controlador actual puede devolver texto o el formato de error de Spring.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para listar calificaciones de proveedores\",\"details\":\"El parámetro companyId=abc no es un identificador numérico válido de calificación de proveedor.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para listar calificaciones de proveedores.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite listar calificaciones de proveedores.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: listar calificaciones de proveedores", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El controlador actual puede devolver texto o el formato de error de Spring.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar calificaciones de proveedores\",\"details\":\"Ocurrió un error al procesar los datos de calificación de proveedor en FuelBridge.\"}")))
    })
    @GetMapping
    public List<ProviderRatingResource> getAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long providerId) {
        return ratingRepository.findAll(companyId, providerId).stream()
                .map(ProviderRatingsController::toResource).toList();
    }

    @Operation(
        summary = "Calificar proveedor",
        description = "Registra una calificación entera entre 1 y 5. Comprueba por HTTP que comprador y proveedor existan en IAM y permite una sola calificación por pareja.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para calificar proveedor.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.catalog.interfaces.rest.resources.ProviderRatingResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"companyId\":101,\"providerId\":202,\"rating\":5}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.catalog.interfaces.rest.resources.ProviderRatingResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":1201,\"companyId\":101,\"providerId\":202,\"rating\":5}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "La calificación del proveedor debe estar entre 1 y 5", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El controlador actual puede devolver texto o el formato de error de Spring.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"La calificación del proveedor debe estar entre 1 y 5\",\"details\":\"companyId=101, providerId=202, rating=6: la calificación supera el máximo permitido.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para calificar proveedor.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite calificar proveedor.\"}"))),
        @ApiResponse(responseCode = "409", description = "La operación entra en conflicto con un registro existente o con la disponibilidad del recurso.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "La empresa compradora ya calificó a este proveedor", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El controlador actual puede devolver texto o el formato de error de Spring.", value = "{\"code\":\"PROVIDERRATING_CONFLICT\",\"message\":\"La empresa compradora ya calificó a este proveedor\",\"details\":\"Ya existe una calificación para companyId=101 y providerId=202.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: calificar proveedor", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El controlador actual puede devolver texto o el formato de error de Spring.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: calificar proveedor\",\"details\":\"Ocurrió un error al procesar los datos de calificación de proveedor en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo consultar el servicio requerido por FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El controlador actual puede devolver texto o el formato de error de Spring.", value = "{\"code\":\"BAD_GATEWAY\",\"message\":\"No se pudo consultar el servicio requerido por FuelBridge\",\"details\":\"El servicio remoto no respondió correctamente al intentar calificar proveedor.\"}")))
    })
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProviderRatingResource resource) {
        var validation = validate(resource);
        if (validation != null) return validation;
        if (ratingRepository.findByCompanyIdAndProviderId(resource.companyId(), resource.providerId()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("The buyer company already rated this provider");
        }
        try {
            var saved = ratingRepository.save(
                    new ProviderRating(resource.companyId(), resource.providerId(), resource.rating()));
            return new ResponseEntity<>(toResource(saved), HttpStatus.CREATED);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @Operation(
        summary = "Actualizar calificación",
        description = "Cambia la calificación a un entero entre 1 y 5; companyId y providerId deben coincidir con los del registro existente. Comprueba la existencia de ambas empresas en IAM.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Identificador del calificación.", example = "1201")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para actualizar calificación.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.catalog.interfaces.rest.resources.ProviderRatingResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"companyId\":101,\"providerId\":202,\"rating\":4}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.catalog.interfaces.rest.resources.ProviderRatingResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":1201,\"companyId\":101,\"providerId\":202,\"rating\":4}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Datos de calificación inválidos", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El controlador actual puede devolver texto o el formato de error de Spring.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Datos de calificación inválidos\",\"details\":\"companyId=101 y providerId=202 deben identificar empresas existentes; rating debe estar entre 1 y 5.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para actualizar calificación.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite actualizar calificación.\"}"))),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Calificación de proveedor no encontrada", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Esta ruta actualmente devuelve 404 sin cuerpo.", value = "{\"code\":\"PROVIDERRATING_NOT_FOUND\",\"message\":\"Calificación de proveedor no encontrada\",\"details\":\"No existe calificación de proveedor con identificador 1201.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: actualizar calificación", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El controlador actual puede devolver texto o el formato de error de Spring.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: actualizar calificación\",\"details\":\"Ocurrió un error al procesar los datos de calificación de proveedor en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo consultar el servicio requerido por FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El controlador actual puede devolver texto o el formato de error de Spring.", value = "{\"code\":\"BAD_GATEWAY\",\"message\":\"No se pudo consultar el servicio requerido por FuelBridge\",\"details\":\"El servicio remoto no respondió correctamente al intentar actualizar calificación.\"}")))
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ProviderRatingResource resource) {
        var validation = validate(resource);
        if (validation != null) return validation;
        var rating = ratingRepository.findById(id).orElse(null);
        if (rating == null) return ResponseEntity.notFound().build();
        if (!rating.getCompanyId().equals(resource.companyId())
                || !rating.getProviderId().equals(resource.providerId())) {
            return ResponseEntity.badRequest().body("Company and provider cannot be changed");
        }
        try {
            rating.changeRating(resource.rating());
            return ResponseEntity.ok(toResource(ratingRepository.save(rating)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    private ResponseEntity<?> validate(ProviderRatingResource resource) {
        if (resource.companyId() == null || resource.providerId() == null
                || resource.rating() == null || resource.rating() < 1 || resource.rating() > 5) {
            return ResponseEntity.badRequest().body("A valid company, provider and rating from 1 to 5 are required");
        }
        if (!companyDirectory.buyerExists(resource.companyId())
                || !companyDirectory.providerExists(resource.providerId())) {
            return ResponseEntity.badRequest().body("Buyer company or provider does not exist");
        }
        return null;
    }

    private static ProviderRatingResource toResource(ProviderRating rating) {
        return new ProviderRatingResource(rating.getId(), rating.getCompanyId(),
                rating.getProviderId(), rating.getRating());
    }
}
