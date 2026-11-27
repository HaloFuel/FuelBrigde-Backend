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
import io.swagger.v3.oas.annotations.tags.Tag;

import com.halofuel.fuelbridge.platform.shared.infrastructure.http.EquipmentServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;

/** Retains the public equipment routes while the standalone Equipment service owns the BC. */
@RestController
@RequestMapping("/api/v1/equipment")
@Tag(name = "Equipment", description = "Gateway de compatibilidad hacia el microservicio Equipment")
public class EquipmentServiceGateway {
    private static final String BASE = "/api/v1/equipment";
    private final EquipmentServiceClient client;

    public EquipmentServiceGateway(EquipmentServiceClient client) {
        this.client = client;
    }

    @Operation(
        summary = "Listar equipos",
        description = "Reenvía al microservicio Equipment la consulta de todos los equipos registrados.",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de equipos.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "respuesta", value = "[{\"id\":305,\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}]"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Equipment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @GetMapping
    public ResponseEntity<byte[]> getAll(HttpServletRequest request) {
        return client.forward(HttpMethod.GET, BASE, new LinkedMultiValueMap<>(), null, request);
    }

    @Operation(
        summary = "Consultar equipo",
        description = "Reenvía al microservicio Equipment la consulta del equipo identificado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "equipmentId", in = ParameterIn.PATH, required = true, description = "Identificador del equipo.", example = "305")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Equipo encontrado.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "respuesta", value = "{\"id\":305,\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}"))),
        @ApiResponse(responseCode = "404", description = "No existe el equipo indicado.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Equipment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @GetMapping("/{equipmentId}")
    public ResponseEntity<byte[]> getById(@PathVariable Long equipmentId, HttpServletRequest request) {
        return client.forward(HttpMethod.GET, BASE + "/" + equipmentId, new LinkedMultiValueMap<>(), null, request);
    }

    @Operation(
        summary = "Listar equipos de una empresa",
        description = "Reenvía al microservicio Equipment la consulta de los equipos de la empresa compradora.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "companyId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa compradora.", example = "101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Equipos de la empresa.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "respuesta", value = "[{\"id\":305,\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}]"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Equipment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @GetMapping("/company/{companyId}")
    public ResponseEntity<byte[]> getByCompany(@PathVariable Long companyId, HttpServletRequest request) {
        return client.forward(HttpMethod.GET, BASE + "/company/" + companyId, new LinkedMultiValueMap<>(), null, request);
    }

    @Operation(
        summary = "Registrar equipo",
        description = "Reenvía al microservicio Equipment el registro de un equipo de la empresa compradora.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos del equipo a registrar.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"),
                examples = { @ExampleObject(name = "principal", value = "{\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Equipo registrado.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "respuesta", value = "{\"id\":305,\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}"))),
        @ApiResponse(responseCode = "400", description = "Cuerpo o parámetros inválidos.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Equipment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @PostMapping
    public ResponseEntity<byte[]> create(@RequestBody byte[] body, HttpServletRequest request) {
        return client.forward(HttpMethod.POST, BASE, new LinkedMultiValueMap<>(), body, request);
    }

    @Operation(
        summary = "Actualizar equipo",
        description = "Reenvía al microservicio Equipment la actualización del equipo sin cambiar su empresa propietaria.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "equipmentId", in = ParameterIn.PATH, required = true, description = "Identificador del equipo.", example = "305")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos actualizados del equipo.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"),
                examples = { @ExampleObject(name = "principal", value = "{\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"favoriteProviderId\":202}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Equipo actualizado.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "respuesta", value = "{\"id\":305,\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}"))),
        @ApiResponse(responseCode = "400", description = "Cuerpo o parámetros inválidos.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No existe el equipo indicado.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Equipment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @PostMapping("/{equipmentId}/update")
    public ResponseEntity<byte[]> update(@PathVariable Long equipmentId, @RequestBody byte[] body,
                                         HttpServletRequest request) {
        return client.forward(HttpMethod.POST, BASE + "/" + equipmentId + "/update", new LinkedMultiValueMap<>(), body, request);
    }

    @Operation(
        summary = "Asignar proveedor favorito",
        description = "Reenvía al microservicio Equipment la asignación del proveedor favorito del equipo.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "equipmentId", in = ParameterIn.PATH, required = true, description = "Identificador del equipo.", example = "305")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Proveedor favorito.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"),
                examples = { @ExampleObject(name = "principal", value = "{\"providerId\":202}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Proveedor favorito asignado.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "respuesta", value = "{\"id\":305,\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}"))),
        @ApiResponse(responseCode = "404", description = "No existe el equipo indicado.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Equipment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @PostMapping("/{equipmentId}/favorite-provider")
    public ResponseEntity<byte[]> assignFavoriteProvider(@PathVariable Long equipmentId, @RequestBody byte[] body,
                                                         HttpServletRequest request) {
        return client.forward(HttpMethod.POST, BASE + "/" + equipmentId + "/favorite-provider", new LinkedMultiValueMap<>(), body, request);
    }

    @Operation(
        summary = "Registrar recepción de combustible",
        description = "Reenvía al microservicio Equipment los litros entregados; el nivel nunca supera la capacidad del tanque.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "equipmentId", in = ParameterIn.PATH, required = true, description = "Identificador del equipo.", example = "305")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Litros entregados; deben ser mayores que cero.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"),
                examples = { @ExampleObject(name = "principal", value = "{\"quantity\":240}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Nivel de combustible actualizado.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "respuesta", value = "{\"id\":305,\"name\":\"Excavadora Caterpillar 320\",\"equipmentType\":\"EXCAVATOR\",\"licensePlate\":\"EQ-320-LIM\",\"fuelType\":\"DIESEL\",\"tankCapacity\":400,\"currentLevel\":80,\"location\":\"Obra Villa El Salvador, Lima\",\"status\":\"ACTIVE\",\"autoRefill\":true,\"refillThreshold\":25,\"lastRefillDate\":\"2026-10-08\",\"companyId\":101,\"favoriteProviderId\":202}"))),
        @ApiResponse(responseCode = "400", description = "Cuerpo o parámetros inválidos.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No existe el equipo indicado.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Equipment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @PostMapping("/{equipmentId}/fuel-receipts")
    public ResponseEntity<byte[]> receiveFuel(@PathVariable Long equipmentId, @RequestBody byte[] body,
                                              HttpServletRequest request) {
        return client.forward(HttpMethod.POST, BASE + "/" + equipmentId + "/fuel-receipts", new LinkedMultiValueMap<>(), body, request);
    }

    @Operation(
        summary = "Eliminar equipo",
        description = "Reenvía al microservicio Equipment la eliminación del equipo identificado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "equipmentId", in = ParameterIn.PATH, required = true, description = "Identificador del equipo.", example = "305")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Equipo eliminado; sin cuerpo de respuesta.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No existe el equipo indicado.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Equipment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = "{\"message\":\"Standalone service unavailable\"}"))),
        @ApiResponse(responseCode = "default", description = "Otro código transmitido por el microservicio; se conserva su cuerpo y tipo de contenido.", content = @Content)
    })
    @DeleteMapping("/{equipmentId}")
    public ResponseEntity<byte[]> delete(@PathVariable Long equipmentId, HttpServletRequest request) {
        return client.forward(HttpMethod.DELETE, BASE + "/" + equipmentId, new LinkedMultiValueMap<>(), null, request);
    }
}
