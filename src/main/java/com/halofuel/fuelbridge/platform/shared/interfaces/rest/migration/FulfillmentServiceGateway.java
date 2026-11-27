package com.halofuel.fuelbridge.platform.shared.interfaces.rest.migration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.halofuel.fuelbridge.platform.shared.infrastructure.http.FulfillmentServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

/**
 * Retains the public delivery, driver and vehicle routes while the standalone
 * Fulfillment service owns the bounded context. Requests, bodies, the bearer token
 * and business errors are relayed unchanged, so the web application and the
 * Reporting service keep using the same URLs.
 */
@RestController
@Tag(name = "Fulfillment", description = "Gateway de compatibilidad hacia el microservicio Fulfillment (entregas, conductores y vehículos)")
public class FulfillmentServiceGateway {

    private static final String DELIVERIES = "/api/v1/deliveries";
    private static final String DRIVERS = "/api/v1/drivers";
    private static final String VEHICLES = "/api/v1/vehicles";
    private static final String UNAVAILABLE = "{\"message\":\"Standalone service unavailable\"}";

    private final FulfillmentServiceClient client;

    public FulfillmentServiceGateway(FulfillmentServiceClient client) {
        this.client = client;
    }

    // ---------------------------------------------------------------- deliveries

    @Operation(summary = "Programar entrega",
        description = "Reenvía al microservicio Fulfillment la asignación conjunta de conductor y vehículo a un pedido PENDING; reserva stock en Inventory y solicita el despacho del pedido a Ordering.",
        security = { @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Entrega creada en estado DISPATCHED."),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "404", description = "Pedido, conductor o vehículo inexistente."),
        @ApiResponse(responseCode = "409", description = "Recursos no disponibles, stock insuficiente, entrega existente o despacho rechazado por Ordering."),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @PostMapping(DELIVERIES)
    public ResponseEntity<byte[]> createDelivery(@RequestBody byte[] body, HttpServletRequest request) {
        return client.forward(HttpMethod.POST, DELIVERIES, none(), body, request);
    }

    @Operation(summary = "Despachar entrega", description = "Reenvía al microservicio Fulfillment el despacho de la entrega indicada.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = { @Parameter(name = "deliveryId", in = ParameterIn.PATH, required = true, description = "Identificador de la entrega.", example = "901") })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entrega despachada."),
        @ApiResponse(responseCode = "404", description = "No existe la entrega indicada."),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @PostMapping(DELIVERIES + "/{deliveryId}/dispatch")
    public ResponseEntity<byte[]> dispatchDelivery(@PathVariable Long deliveryId, HttpServletRequest request) {
        return client.forward(HttpMethod.POST, DELIVERIES + "/" + deliveryId + "/dispatch", none(), null, request);
    }

    @Operation(summary = "Completar entrega",
        description = "Reenvía al microservicio Fulfillment el cierre de la entrega: registra el combustible en Equipment y solicita la recepción del pedido a Ordering.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = { @Parameter(name = "deliveryId", in = ParameterIn.PATH, required = true, description = "Identificador de la entrega.", example = "901") })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entrega en estado DELIVERED."),
        @ApiResponse(responseCode = "404", description = "No existe la entrega o su pedido."),
        @ApiResponse(responseCode = "409", description = "El pedido no está despachado."),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @PostMapping(DELIVERIES + "/{deliveryId}/complete")
    public ResponseEntity<byte[]> completeDelivery(@PathVariable Long deliveryId, HttpServletRequest request) {
        return client.forward(HttpMethod.POST, DELIVERIES + "/" + deliveryId + "/complete", none(), null, request);
    }

    @Operation(summary = "Marcar entrega como fallida", description = "Reenvía al microservicio Fulfillment el registro de una entrega fallida con su motivo.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = { @Parameter(name = "deliveryId", in = ParameterIn.PATH, required = true, description = "Identificador de la entrega.", example = "901") })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entrega en estado FAILED."),
        @ApiResponse(responseCode = "404", description = "No existe la entrega indicada."),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @PostMapping(DELIVERIES + "/{deliveryId}/fail")
    public ResponseEntity<byte[]> failDelivery(@PathVariable Long deliveryId, @RequestBody byte[] body,
                                               HttpServletRequest request) {
        return client.forward(HttpMethod.POST, DELIVERIES + "/" + deliveryId + "/fail", none(), body, request);
    }

    @Operation(summary = "Listar entregas", description = "Reenvía al microservicio Fulfillment la consulta de todas las entregas.",
        security = { @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de entregas."),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @GetMapping(DELIVERIES)
    public ResponseEntity<byte[]> getAllDeliveries(HttpServletRequest request) {
        return client.forward(HttpMethod.GET, DELIVERIES, none(), null, request);
    }

    @Operation(summary = "Listar entregas de un proveedor", description = "Reenvía al microservicio Fulfillment la consulta de entregas del proveedor indicado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = { @Parameter(name = "providerId", in = ParameterIn.PATH, required = true, description = "Identificador del proveedor.", example = "202") })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de entregas del proveedor."),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @GetMapping(DELIVERIES + "/provider/{providerId}")
    public ResponseEntity<byte[]> getDeliveriesByProvider(@PathVariable Long providerId, HttpServletRequest request) {
        return client.forward(HttpMethod.GET, DELIVERIES + "/provider/" + providerId, none(), null, request);
    }

    @Operation(summary = "Consultar entrega", description = "Reenvía al microservicio Fulfillment la consulta de la entrega indicada.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = { @Parameter(name = "deliveryId", in = ParameterIn.PATH, required = true, description = "Identificador de la entrega.", example = "901") })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entrega encontrada."),
        @ApiResponse(responseCode = "404", description = "No existe la entrega indicada.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @GetMapping(DELIVERIES + "/{deliveryId}")
    public ResponseEntity<byte[]> getDeliveryById(@PathVariable Long deliveryId, HttpServletRequest request) {
        return client.forward(HttpMethod.GET, DELIVERIES + "/" + deliveryId, none(), null, request);
    }

    @Operation(summary = "Consultar entrega de un pedido", description = "Reenvía al microservicio Fulfillment la consulta de la entrega asociada al pedido indicado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = { @Parameter(name = "orderId", in = ParameterIn.PATH, required = true, description = "Identificador del pedido de combustible.", example = "4101") })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entrega del pedido."),
        @ApiResponse(responseCode = "404", description = "El pedido no tiene entrega.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @GetMapping(DELIVERIES + "/order/{orderId}")
    public ResponseEntity<byte[]> getDeliveryByOrder(@PathVariable Long orderId, HttpServletRequest request) {
        return client.forward(HttpMethod.GET, DELIVERIES + "/order/" + orderId, none(), null, request);
    }

    // ---------------------------------------------------------------- drivers

    @Operation(summary = "Listar conductores de un proveedor", description = "Reenvía al microservicio Fulfillment la consulta de conductores del proveedor indicado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = { @Parameter(name = "providerId", in = ParameterIn.QUERY, required = true, description = "Identificador del proveedor.", example = "202") })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de conductores."),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @GetMapping(DRIVERS)
    public ResponseEntity<byte[]> getDriversByProvider(@RequestParam Long providerId, HttpServletRequest request) {
        return client.forward(HttpMethod.GET, DRIVERS, providerQuery(providerId), null, request);
    }

    @Operation(summary = "Consultar conductor", description = "Reenvía al microservicio Fulfillment la consulta del conductor indicado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = { @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Identificador del conductor.", example = "801") })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conductor encontrado."),
        @ApiResponse(responseCode = "404", description = "No existe el conductor indicado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @GetMapping(DRIVERS + "/{id}")
    public ResponseEntity<byte[]> getDriverById(@PathVariable Long id, HttpServletRequest request) {
        return client.forward(HttpMethod.GET, DRIVERS + "/" + id, none(), null, request);
    }

    @Operation(summary = "Registrar conductor", description = "Reenvía al microservicio Fulfillment el registro de un conductor; rechaza licencias duplicadas.",
        security = { @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Conductor registrado."),
        @ApiResponse(responseCode = "409", description = "Licencia ya registrada (DRIVER_CONFLICT)."),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @PostMapping(DRIVERS)
    public ResponseEntity<byte[]> createDriver(@RequestBody byte[] body, HttpServletRequest request) {
        return client.forward(HttpMethod.POST, DRIVERS, none(), body, request);
    }

    @Operation(summary = "Actualizar conductor", description = "Reenvía al microservicio Fulfillment la actualización del conductor indicado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = { @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Identificador del conductor.", example = "801") })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conductor actualizado."),
        @ApiResponse(responseCode = "404", description = "No existe el conductor indicado."),
        @ApiResponse(responseCode = "409", description = "Licencia usada por otro conductor (DRIVER_CONFLICT)."),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @PutMapping(DRIVERS + "/{id}")
    public ResponseEntity<byte[]> updateDriver(@PathVariable Long id, @RequestBody byte[] body, HttpServletRequest request) {
        return client.forward(HttpMethod.PUT, DRIVERS + "/" + id, none(), body, request);
    }

    @Operation(summary = "Eliminar conductor", description = "Reenvía al microservicio Fulfillment la eliminación del conductor indicado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = { @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Identificador del conductor.", example = "801") })
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Conductor eliminado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @DeleteMapping(DRIVERS + "/{id}")
    public ResponseEntity<byte[]> deleteDriver(@PathVariable Long id, HttpServletRequest request) {
        return client.forward(HttpMethod.DELETE, DRIVERS + "/" + id, none(), null, request);
    }

    // ---------------------------------------------------------------- vehicles

    @Operation(summary = "Listar vehículos de un proveedor", description = "Reenvía al microservicio Fulfillment la consulta de vehículos del proveedor indicado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = { @Parameter(name = "providerId", in = ParameterIn.QUERY, required = true, description = "Identificador del proveedor.", example = "202") })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de vehículos."),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @GetMapping(VEHICLES)
    public ResponseEntity<byte[]> getVehiclesByProvider(@RequestParam Long providerId, HttpServletRequest request) {
        return client.forward(HttpMethod.GET, VEHICLES, providerQuery(providerId), null, request);
    }

    @Operation(summary = "Consultar vehículo", description = "Reenvía al microservicio Fulfillment la consulta del vehículo indicado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = { @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Identificador del vehículo.", example = "701") })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehículo encontrado."),
        @ApiResponse(responseCode = "404", description = "No existe el vehículo indicado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @GetMapping(VEHICLES + "/{id}")
    public ResponseEntity<byte[]> getVehicleById(@PathVariable Long id, HttpServletRequest request) {
        return client.forward(HttpMethod.GET, VEHICLES + "/" + id, none(), null, request);
    }

    @Operation(summary = "Registrar vehículo", description = "Reenvía al microservicio Fulfillment el registro de un vehículo; rechaza placas duplicadas.",
        security = { @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Vehículo registrado."),
        @ApiResponse(responseCode = "409", description = "Placa ya registrada (VEHICLE_CONFLICT)."),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @PostMapping(VEHICLES)
    public ResponseEntity<byte[]> createVehicle(@RequestBody byte[] body, HttpServletRequest request) {
        return client.forward(HttpMethod.POST, VEHICLES, none(), body, request);
    }

    @Operation(summary = "Actualizar vehículo", description = "Reenvía al microservicio Fulfillment la actualización del vehículo indicado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = { @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Identificador del vehículo.", example = "701") })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehículo actualizado."),
        @ApiResponse(responseCode = "404", description = "No existe el vehículo indicado."),
        @ApiResponse(responseCode = "409", description = "Placa usada por otro vehículo (VEHICLE_CONFLICT)."),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @PutMapping(VEHICLES + "/{id}")
    public ResponseEntity<byte[]> updateVehicle(@PathVariable Long id, @RequestBody byte[] body, HttpServletRequest request) {
        return client.forward(HttpMethod.PUT, VEHICLES + "/" + id, none(), body, request);
    }

    @Operation(summary = "Eliminar vehículo", description = "Reenvía al microservicio Fulfillment la eliminación del vehículo indicado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = { @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "Identificador del vehículo.", example = "701") })
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Vehículo eliminado.", content = @Content),
        @ApiResponse(responseCode = "502", description = "El microservicio Fulfillment no está disponible.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "servicioNoDisponible", value = UNAVAILABLE)))
    })
    @DeleteMapping(VEHICLES + "/{id}")
    public ResponseEntity<byte[]> deleteVehicle(@PathVariable Long id, HttpServletRequest request) {
        return client.forward(HttpMethod.DELETE, VEHICLES + "/" + id, none(), null, request);
    }

    private static MultiValueMap<String, String> none() {
        return new LinkedMultiValueMap<>();
    }

    private static MultiValueMap<String, String> providerQuery(Long providerId) {
        var query = new LinkedMultiValueMap<String, String>();
        query.add("providerId", providerId.toString());
        return query;
    }
}
