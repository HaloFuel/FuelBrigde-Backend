package com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest;

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

import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Driver;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.DriverRepository;
import com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DriverResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DriversController
 *
 * REST controller that exposes CRUD endpoints for driver management (TS-18).
 * Allows providers to register, update, retrieve and delete drivers
 * assigned to their fleet within the Fulfillment bounded context.
 *
 * Base path: /api/v1/drivers
 */

@RestController
@RequestMapping("/api/v1/drivers")
public class DriversController {

    private final DriverRepository repository;

    public DriversController(DriverRepository repository) {
        this.repository = repository;
    }

    @Operation(
        summary = "Listar conductores de un proveedor",
        description = "Devuelve los registros de conductor del proveedor indicado por el filtro obligatorio providerId.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "providerId", in = ParameterIn.QUERY, required = true, description = "Filtro obligatorio por empresa proveedora.", example = "202")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DriverResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":801,\"providerId\":202,\"firstName\":\"Carlos\",\"lastName\":\"Quispe\",\"licenseNumber\":\"Q45892176\",\"phoneNumber\":\"+51987654321\",\"email\":\"carlos.quispe@andina-combustibles.example\",\"status\":\"AVAILABLE\"}]"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping
    public List<DriverResource> getByProvider(@RequestParam Long providerId) {
        return repository.findByProviderId(providerId).stream().map(DriversController::toResource).toList();
    }

    @Operation(
        summary = "Consultar conductor",
        description = "Devuelve los datos del conductor identificado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Identificador del conductor.", example = "801")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DriverResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":801,\"providerId\":202,\"firstName\":\"Carlos\",\"lastName\":\"Quispe\",\"licenseNumber\":\"Q45892176\",\"phoneNumber\":\"+51987654321\",\"email\":\"carlos.quispe@andina-combustibles.example\",\"status\":\"AVAILABLE\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<DriverResource> getById(@PathVariable Long id) {
        return repository.findById(id).map(DriversController::toResource)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Registrar conductor",
        description = "Registra un conductor del proveedor. Si status está vacío usa AVAILABLE.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para registrar conductor.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DriverResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"providerId\":202,\"firstName\":\"Carlos\",\"lastName\":\"Quispe\",\"licenseNumber\":\"Q45892176\",\"phoneNumber\":\"+51987654321\",\"email\":\"carlos.quispe@andina-combustibles.example\",\"status\":\"AVAILABLE\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DriverResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":801,\"providerId\":202,\"firstName\":\"Carlos\",\"lastName\":\"Quispe\",\"licenseNumber\":\"Q45892176\",\"phoneNumber\":\"+51987654321\",\"email\":\"carlos.quispe@andina-combustibles.example\",\"status\":\"AVAILABLE\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PostMapping
    public ResponseEntity<DriverResource> create(@RequestBody DriverResource resource) {
        var driver = new Driver(resource.providerId(), resource.firstName(), resource.lastName(),
                resource.licenseNumber(), resource.phoneNumber(), resource.email(),
                defaultStatus(resource.status()));
        return new ResponseEntity<>(toResource(repository.save(driver)), HttpStatus.CREATED);
    }

    @Operation(
        summary = "Actualizar conductor",
        description = "Actualiza los datos del conductor existente. Si providerId es nulo conserva el proveedor actual; si status está vacío usa AVAILABLE.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Identificador del conductor.", example = "801")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para actualizar conductor.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DriverResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"providerId\":202,\"firstName\":\"Carlos\",\"lastName\":\"Quispe\",\"licenseNumber\":\"Q45892176\",\"phoneNumber\":\"+51987654321\",\"email\":\"carlos.quispe@andina-combustibles.example\",\"status\":\"AVAILABLE\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DriverResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":801,\"providerId\":202,\"firstName\":\"Carlos\",\"lastName\":\"Quispe\",\"licenseNumber\":\"Q45892176\",\"phoneNumber\":\"+51987654321\",\"email\":\"carlos.quispe@andina-combustibles.example\",\"status\":\"AVAILABLE\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<DriverResource> update(@PathVariable Long id, @RequestBody DriverResource resource) {
        var driver = repository.findById(id).orElse(null);
        if (driver == null) return ResponseEntity.notFound().build();
        driver.update(resource.providerId() != null ? resource.providerId() : driver.getProviderId(),
                resource.firstName(), resource.lastName(), resource.licenseNumber(),
                resource.phoneNumber(), resource.email(), defaultStatus(resource.status()));
        return ResponseEntity.ok(toResource(repository.save(driver)));
    }

    @Operation(
        summary = "Eliminar conductor",
        description = "Elimina el conductor identificado y devuelve una respuesta sin cuerpo.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Identificador del conductor.", example = "801")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Recurso eliminado. Ejemplo HTTP: HTTP/1.1 204 No Content (sin cuerpo).", content = @Content),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (repository.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private static String defaultStatus(String status) {
        return status == null || status.isBlank() ? "AVAILABLE" : status;
    }

    private static DriverResource toResource(Driver driver) {
        return new DriverResource(driver.getId(), driver.getProviderId(), driver.getFirstName(),
                driver.getLastName(), driver.getLicenseNumber(), driver.getPhoneNumber(),
                driver.getEmail(), driver.getStatus());
    }
}
