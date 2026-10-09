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

import com.halofuel.fuelbridge.platform.fulfillment.application.commandservices.DeliveryCommandService;
import com.halofuel.fuelbridge.platform.fulfillment.application.queryservices.DeliveryQueryService;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.CompleteDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.DispatchDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.commands.FailDeliveryCommand;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.queries.GetAllDeliveriesQuery;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.queries.GetDeliveryByIdQuery;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.queries.GetDeliveryByOrderIdQuery;
import com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.CreateDeliveryResource;
import com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DeliveryResource;
import com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.FailDeliveryResource;
import com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.transform.CreateDeliveryCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.transform.DeliveryResourceFromEntityAssembler;
import com.halofuel.fuelbridge.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/deliveries", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Deliveries", description = "Fulfillment management endpoints")
public class DeliveriesController {

    private final DeliveryCommandService deliveryCommandService;
    private final DeliveryQueryService deliveryQueryService;

    public DeliveriesController(DeliveryCommandService deliveryCommandService,
                                DeliveryQueryService deliveryQueryService) {
        this.deliveryCommandService = deliveryCommandService;
        this.deliveryQueryService = deliveryQueryService;
    }

    @Operation(
        summary = "Programar entrega",
        description = "Programa la entrega del pedido y asigna un conductor y vehículo del proveedor. Comprueba disponibilidad, capacidad, stock y ausencia de otra entrega para el pedido.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para programar entrega.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.CreateDeliveryResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"orderId\":4101,\"providerId\":202,\"driverId\":801,\"vehicleId\":901,\"scheduledDate\":\"2026-10-15\",\"notes\":\"Entregar Diesel B5 en el tanque de la obra; coordinar con el supervisor.\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DeliveryResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":7101,\"orderId\":4101,\"providerId\":202,\"driverId\":801,\"vehicleId\":901,\"status\":\"SCHEDULED\",\"scheduledDate\":\"2026-10-15\",\"dispatchedAt\":null,\"deliveredAt\":null,\"notes\":\"Entregar Diesel B5 en el tanque de la obra; coordinar con el supervisor.\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "409", description = "La operación entra en conflicto con un registro existente o con la disponibilidad del recurso.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PostMapping
    public ResponseEntity<?> createDelivery(@RequestBody CreateDeliveryResource resource) {
        var command = CreateDeliveryCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = deliveryCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                DeliveryResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.CREATED);
    }

    @Operation(
        summary = "Despachar entrega",
        description = "Marca la entrega como DISPATCHED y registra la fecha y hora de despacho. No requiere cuerpo.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "deliveryId", in = ParameterIn.PATH, required = true, description = "Identificador de la entrega.", example = "7101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DeliveryResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":7101,\"orderId\":4101,\"providerId\":202,\"driverId\":801,\"vehicleId\":901,\"status\":\"DISPATCHED\",\"scheduledDate\":\"2026-10-15\",\"dispatchedAt\":\"2026-10-15T08:00:00\",\"deliveredAt\":null,\"notes\":\"Entregar Diesel B5 en el tanque de la obra; coordinar con el supervisor.\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PostMapping("/{deliveryId}/dispatch")
    public ResponseEntity<?> dispatchDelivery(@PathVariable Long deliveryId) {
        var result = deliveryCommandService.handle(new DispatchDeliveryCommand(deliveryId));
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                DeliveryResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK);
    }

    @Operation(
        summary = "Completar entrega",
        description = "Marca la entrega como DELIVERED, registra su finalización y actualiza el pedido asociado. No requiere cuerpo.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "deliveryId", in = ParameterIn.PATH, required = true, description = "Identificador de la entrega.", example = "7101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DeliveryResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":7101,\"orderId\":4101,\"providerId\":202,\"driverId\":801,\"vehicleId\":901,\"status\":\"DELIVERED\",\"scheduledDate\":\"2026-10-15\",\"dispatchedAt\":\"2026-10-15T08:00:00\",\"deliveredAt\":\"2026-10-15T10:30:00\",\"notes\":\"Entregar Diesel B5 en el tanque de la obra; coordinar con el supervisor.\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PostMapping("/{deliveryId}/complete")
    public ResponseEntity<?> completeDelivery(@PathVariable Long deliveryId) {
        var result = deliveryCommandService.handle(new CompleteDeliveryCommand(deliveryId));
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                DeliveryResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK);
    }

    @Operation(
        summary = "Registrar entrega fallida",
        description = "Marca la entrega como FAILED y guarda el motivo recibido en notes.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "deliveryId", in = ParameterIn.PATH, required = true, description = "Identificador de la entrega.", example = "7101")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para registrar entrega fallida.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.FailDeliveryResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"reason\":\"El acceso a la obra permanece cerrado.\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DeliveryResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":7101,\"orderId\":4101,\"providerId\":202,\"driverId\":801,\"vehicleId\":901,\"status\":\"FAILED\",\"scheduledDate\":\"2026-10-15\",\"dispatchedAt\":null,\"deliveredAt\":null,\"notes\":\"El acceso a la obra permanece cerrado.\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PostMapping("/{deliveryId}/fail")
    public ResponseEntity<?> failDelivery(@PathVariable Long deliveryId,
                                          @RequestBody FailDeliveryResource resource) {
        var result = deliveryCommandService.handle(new FailDeliveryCommand(deliveryId, resource.reason()));
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                DeliveryResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK);
    }

    @Operation(
        summary = "Listar entregas",
        description = "Devuelve las entregas registradas, sus asignaciones y estado actual.",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DeliveryResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":7101,\"orderId\":4101,\"providerId\":202,\"driverId\":801,\"vehicleId\":901,\"status\":\"SCHEDULED\",\"scheduledDate\":\"2026-10-15\",\"dispatchedAt\":null,\"deliveredAt\":null,\"notes\":\"Entregar Diesel B5 en el tanque de la obra; coordinar con el supervisor.\"}]"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<DeliveryResource>> getAllDeliveries() {
        var deliveries = deliveryQueryService.handle(new GetAllDeliveriesQuery());
        var resources = deliveries.stream().map(DeliveryResourceFromEntityAssembler::toResourceFromEntity).toList();
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
        summary = "Listar entregas de un proveedor",
        description = "Filtra las entregas por el proveedor indicado; puede devolver una lista vacía.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "providerId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa proveedora.", example = "202")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DeliveryResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":7101,\"orderId\":4101,\"providerId\":202,\"driverId\":801,\"vehicleId\":901,\"status\":\"SCHEDULED\",\"scheduledDate\":\"2026-10-15\",\"dispatchedAt\":null,\"deliveredAt\":null,\"notes\":\"Entregar Diesel B5 en el tanque de la obra; coordinar con el supervisor.\"}]"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<DeliveryResource>> getDeliveriesByProvider(@PathVariable Long providerId) {
        var resources = deliveryQueryService.handle(new GetAllDeliveriesQuery()).stream()
                .filter(delivery -> providerId.equals(delivery.getProviderId()))
                .map(DeliveryResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(
        summary = "Consultar entrega",
        description = "Devuelve la entrega identificada con su conductor, vehículo y fechas de seguimiento.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "deliveryId", in = ParameterIn.PATH, required = true, description = "Identificador de la entrega.", example = "7101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DeliveryResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":7101,\"orderId\":4101,\"providerId\":202,\"driverId\":801,\"vehicleId\":901,\"status\":\"SCHEDULED\",\"scheduledDate\":\"2026-10-15\",\"dispatchedAt\":null,\"deliveredAt\":null,\"notes\":\"Entregar Diesel B5 en el tanque de la obra; coordinar con el supervisor.\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryResource> getDeliveryById(@PathVariable Long deliveryId) {
        var result = deliveryQueryService.handle(new GetDeliveryByIdQuery(deliveryId));
        return result.map(d -> new ResponseEntity<>(
                        DeliveryResourceFromEntityAssembler.toResourceFromEntity(d), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(
        summary = "Consultar entrega de un pedido",
        description = "Devuelve la entrega asociada al pedido indicado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "orderId", in = ParameterIn.PATH, required = true, description = "Identificador del pedido de combustible.", example = "4101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.fulfillment.interfaces.rest.resources.DeliveryResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":7101,\"orderId\":4101,\"providerId\":202,\"driverId\":801,\"vehicleId\":901,\"status\":\"SCHEDULED\",\"scheduledDate\":\"2026-10-15\",\"dispatchedAt\":null,\"deliveredAt\":null,\"notes\":\"Entregar Diesel B5 en el tanque de la obra; coordinar con el supervisor.\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<DeliveryResource> getDeliveryByOrder(@PathVariable Long orderId) {
        var result = deliveryQueryService.handle(new GetDeliveryByOrderIdQuery(orderId));
        return result.map(d -> new ResponseEntity<>(
                        DeliveryResourceFromEntityAssembler.toResourceFromEntity(d), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
