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
        @ApiResponse(responseCode = "400", description = "Parámetro o cuerpo HTTP inválido: devuelve el JSON de error de Spring Boot del microservicio.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring400", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":400,\"error\":\"Bad Request\",\"path\":\"/api/v1/provider-ratings\"}") })),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: devuelve el JSON de error de Spring Boot mediante /error.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring401", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/v1/provider-ratings\"}") })),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error no controlado: devuelve el JSON de error de Spring Boot del microservicio.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring500", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/v1/provider-ratings\"}") }))
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
        @ApiResponse(responseCode = "400", description = "Validación rechazada: devuelve texto literal con Content-Type application/json, sin serialización JSON. Los errores de lectura o conversión HTTP del microservicio usan el JSON de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "calificacionInvalida", description = "Texto literal si faltan identificadores o rating está fuera de 1 a 5.", value = "A valid company, provider and rating from 1 to 5 are required"), @ExampleObject(name = "empresaInexistente", description = "Texto literal cuando IAM indica que una empresa no existe.", value = "Buyer company or provider does not exist"), @ExampleObject(name = "errorSpring400", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":400,\"error\":\"Bad Request\",\"path\":\"/api/v1/provider-ratings\"}") })),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: devuelve el JSON de error de Spring Boot mediante /error.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring401", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/v1/provider-ratings\"}") })),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "409", description = "La empresa compradora ya calificó al proveedor: devuelve texto literal con Content-Type application/json, sin serialización JSON.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "calificacionDuplicada", description = "Texto literal devuelto por el controlador de catalog y transmitido sin cambios por el gateway.", value = "The buyer company already rated this provider") })),
        @ApiResponse(responseCode = "500", description = "Error no controlado: devuelve el JSON de error de Spring Boot del microservicio.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring500", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/v1/provider-ratings\"}") })),
        @ApiResponse(responseCode = "502", description = "El servicio del que depende esta operación falló: devuelve el JSON de error de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring502", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":502,\"error\":\"Bad Gateway\",\"path\":\"/api/v1/provider-ratings\"}") }))
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
        @ApiResponse(responseCode = "400", description = "Validación rechazada: devuelve texto literal con Content-Type application/json, sin serialización JSON. Los errores de lectura o conversión HTTP del microservicio usan el JSON de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "calificacionInvalida", description = "Texto literal si faltan identificadores o rating está fuera de 1 a 5.", value = "A valid company, provider and rating from 1 to 5 are required"), @ExampleObject(name = "empresaInexistente", description = "Texto literal cuando IAM indica que una empresa no existe.", value = "Buyer company or provider does not exist"), @ExampleObject(name = "empresasNoModificables", description = "Texto literal cuando se intenta modificar companyId o providerId del registro.", value = "Company and provider cannot be changed"), @ExampleObject(name = "errorSpring400", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":400,\"error\":\"Bad Request\",\"path\":\"/api/v1/provider-ratings/1201\"}") })),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: devuelve el JSON de error de Spring Boot mediante /error.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring401", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/v1/provider-ratings/1201\"}") })),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado: sin cuerpo de respuesta.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error no controlado: devuelve el JSON de error de Spring Boot del microservicio.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring500", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/v1/provider-ratings/1201\"}") })),
        @ApiResponse(responseCode = "502", description = "El servicio del que depende esta operación falló: devuelve el JSON de error de Spring Boot.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "errorSpring502", description = "JSON de Spring Boot para una solicitud con Accept: application/json. timestamp y path dependen de la solicitud; la configuración actual omite message y trace.", value = "{\"timestamp\":\"2026-10-09T22:48:12.439Z\",\"status\":502,\"error\":\"Bad Gateway\",\"path\":\"/api/v1/provider-ratings/1201\"}") }))
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
