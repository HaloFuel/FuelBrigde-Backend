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

import com.halofuel.fuelbridge.platform.fulfillment.domain.model.aggregates.Vehicle;
import com.halofuel.fuelbridge.platform.fulfillment.domain.repositories.VehicleRepository;
import com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.VehicleResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * VehiclesController
 *
 * REST controller that exposes CRUD endpoints for vehicle management (TS-19).
 * Allows providers to register, update, retrieve and delete vehicles
 * assigned to their fleet within the Fulfillment bounded context.
 *
 * Base path: /api/v1/vehicles
 */

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehiclesController {

    private final VehicleRepository repository;

    public VehiclesController(VehicleRepository repository) {
        this.repository = repository;
    }

    @Operation(
        summary = "Listar vehículos de un proveedor",
        description = "Devuelve los registros de vehículo del proveedor indicado por el filtro obligatorio providerId.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "providerId", in = ParameterIn.QUERY, required = true, description = "Filtro obligatorio por empresa proveedora.", example = "202")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.VehicleResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":901,\"providerId\":202,\"licensePlate\":\"BFG-482\",\"brand\":\"Volvo\",\"model\":\"FM 460 Cisterna\",\"capacity\":12000,\"unit\":\"L\",\"status\":\"AVAILABLE\"}]"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para listar vehículos de un proveedor", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para listar vehículos de un proveedor\",\"details\":\"El parámetro providerId=abc no es un identificador numérico válido de vehículo cisterna.\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: listar vehículos de un proveedor", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar vehículos de un proveedor\",\"details\":\"Ocurrió un error al procesar los datos de vehículo cisterna en FuelBridge.\"}")))
    })
    @GetMapping
    public List<VehicleResource> getByProvider(@RequestParam Long providerId) {
        return repository.findByProviderId(providerId).stream().map(VehiclesController::toResource).toList();
    }

    @Operation(
        summary = "Consultar vehículo",
        description = "Devuelve los datos del vehículo identificado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Identificador del vehículo.", example = "901")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.VehicleResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":901,\"providerId\":202,\"licensePlate\":\"BFG-482\",\"brand\":\"Volvo\",\"model\":\"FM 460 Cisterna\",\"capacity\":12000,\"unit\":\"L\",\"status\":\"AVAILABLE\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para consultar vehículo", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para consultar vehículo\",\"details\":\"El parámetro id=abc no es un identificador numérico válido de vehículo cisterna.\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado: sin cuerpo de respuesta.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: consultar vehículo", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar vehículo\",\"details\":\"Ocurrió un error al procesar los datos de vehículo cisterna en FuelBridge.\"}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResource> getById(@PathVariable Long id) {
        return repository.findById(id).map(VehiclesController::toResource)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Registrar vehículo",
        description = "Registra un vehículo del proveedor. Si status está vacío usa AVAILABLE. Si unit no está indicado usa LITERS.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para registrar vehículo.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.VehicleResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"providerId\":202,\"licensePlate\":\"BFG-482\",\"brand\":\"Volvo\",\"model\":\"FM 460 Cisterna\",\"capacity\":12000,\"unit\":\"L\",\"status\":\"AVAILABLE\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.VehicleResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":901,\"providerId\":202,\"licensePlate\":\"BFG-482\",\"brand\":\"Volvo\",\"model\":\"FM 460 Cisterna\",\"capacity\":12000,\"unit\":\"L\",\"status\":\"AVAILABLE\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para registrar vehículo", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para registrar vehículo\",\"details\":\"La cisterna de placa B7T-842 debe indicar una capacidad mayor que cero.\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: registrar vehículo", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: registrar vehículo\",\"details\":\"Ocurrió un error al procesar los datos de vehículo cisterna en FuelBridge.\"}")))
    })
    @PostMapping
    public ResponseEntity<VehicleResource> create(@RequestBody VehicleResource resource) {
        var vehicle = new Vehicle(resource.providerId(), resource.licensePlate(), resource.brand(),
                resource.model(), resource.capacity(), defaultUnit(resource.unit()),
                defaultStatus(resource.status()));
        return new ResponseEntity<>(toResource(repository.save(vehicle)), HttpStatus.CREATED);
    }

    @Operation(
        summary = "Actualizar vehículo",
        description = "Actualiza los datos del vehículo existente. Si providerId es nulo conserva el proveedor actual; si status está vacío usa AVAILABLE.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Identificador del vehículo.", example = "901")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para actualizar vehículo.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.VehicleResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"providerId\":202,\"licensePlate\":\"BFG-482\",\"brand\":\"Volvo\",\"model\":\"FM 460 Cisterna\",\"capacity\":12000,\"unit\":\"L\",\"status\":\"AVAILABLE\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.VehicleResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":901,\"providerId\":202,\"licensePlate\":\"BFG-482\",\"brand\":\"Volvo\",\"model\":\"FM 460 Cisterna\",\"capacity\":12000,\"unit\":\"L\",\"status\":\"AVAILABLE\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para actualizar vehículo", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para actualizar vehículo\",\"details\":\"La cisterna de placa B7T-842 debe indicar una capacidad mayor que cero.\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado: sin cuerpo de respuesta.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: actualizar vehículo", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: actualizar vehículo\",\"details\":\"Ocurrió un error al procesar los datos de vehículo cisterna en FuelBridge.\"}")))
    })
    @PutMapping("/{id}")
    public ResponseEntity<VehicleResource> update(@PathVariable Long id, @RequestBody VehicleResource resource) {
        var vehicle = repository.findById(id).orElse(null);
        if (vehicle == null) return ResponseEntity.notFound().build();
        vehicle.update(resource.providerId() != null ? resource.providerId() : vehicle.getProviderId(),
                resource.licensePlate(), resource.brand(), resource.model(), resource.capacity(),
                defaultUnit(resource.unit()), defaultStatus(resource.status()));
        return ResponseEntity.ok(toResource(repository.save(vehicle)));
    }

    @Operation(
        summary = "Eliminar vehículo",
        description = "Elimina el vehículo identificado y devuelve una respuesta sin cuerpo.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Identificador del vehículo.", example = "901")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Recurso eliminado. Ejemplo HTTP: HTTP/1.1 204 No Content (sin cuerpo).", content = @Content),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para eliminar vehículo", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para eliminar vehículo\",\"details\":\"El parámetro id=abc no es un identificador numérico válido de vehículo cisterna.\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado: sin cuerpo de respuesta.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: eliminar vehículo", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: eliminar vehículo\",\"details\":\"Ocurrió un error al procesar los datos de vehículo cisterna en FuelBridge.\"}")))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (repository.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private static String defaultUnit(String unit) {
        return unit == null || unit.isBlank() ? "LITERS" : unit;
    }

    private static String defaultStatus(String status) {
        return status == null || status.isBlank() ? "AVAILABLE" : status;
    }

    private static VehicleResource toResource(Vehicle vehicle) {
        return new VehicleResource(vehicle.getId(), vehicle.getProviderId(), vehicle.getLicensePlate(),
                vehicle.getBrand(), vehicle.getModel(), vehicle.getCapacity(), vehicle.getUnit(),
                vehicle.getStatus());
    }
}
