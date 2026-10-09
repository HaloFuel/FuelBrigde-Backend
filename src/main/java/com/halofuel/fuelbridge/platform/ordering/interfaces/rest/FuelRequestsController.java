package com.halofuel.fuelbridge.platform.ordering.interfaces.rest;

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

import com.halofuel.fuelbridge.platform.ordering.application.internal.commandservices.FuelRequestService;
import com.halofuel.fuelbridge.platform.ordering.infrastructure.persistence.jpa.entities.FuelRequestPersistenceEntity;
import com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.*;
import com.halofuel.fuelbridge.platform.ordering.interfaces.rest.transform.FuelOrderResourceFromEntityAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/fuel-requests")
public class FuelRequestsController {
    private final FuelRequestService service;

    public FuelRequestsController(FuelRequestService service) {
        this.service = service;
    }

    @Operation(
        summary = "Crear solicitud de combustible",
        description = "Registra una solicitud PENDING y comprueba que el producto exista y pertenezca al proveedor indicado. El origen predeterminado es MANUAL.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para crear solicitud de combustible.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.CreateFuelRequestResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"buyerCompanyId\":101,\"providerId\":202,\"equipmentId\":305,\"fuelProductId\":303,\"fuelType\":\"DIESEL\",\"productName\":\"Diesel B5 S-50\",\"quantity\":1000,\"unit\":\"L\",\"unitPrice\":4.8,\"deliveryAddress\":\"Av. Separadora Industrial 2450, Villa El Salvador, Lima\",\"deliveryDate\":\"2026-10-15\",\"source\":\"MANUAL\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.FuelRequestResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":5101,\"buyerCompanyId\":101,\"providerId\":202,\"equipmentId\":305,\"fuelProductId\":303,\"fuelType\":\"DIESEL\",\"productName\":\"Diesel B5 S-50\",\"quantity\":1000,\"unit\":\"L\",\"unitPrice\":4.8,\"deliveryAddress\":\"Av. Separadora Industrial 2450, Villa El Salvador, Lima\",\"deliveryDate\":\"2026-10-15\",\"status\":\"PENDING\",\"source\":\"MANUAL\",\"rejectionReason\":null,\"createdAt\":\"2026-10-09T14:30:00.000Z\",\"updatedAt\":\"2026-10-09T14:30:00.000Z\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PostMapping
    public ResponseEntity<FuelRequestResource> create(@RequestBody CreateFuelRequestResource resource) {
        return new ResponseEntity<>(toResource(service.create(resource)), HttpStatus.CREATED);
    }

    @Operation(
        summary = "Listar solicitudes de combustible",
        description = "Lista las solicitudes. Si buyerCompanyId está presente, filtra por comprador; de lo contrario usa providerId cuando esté presente. Sin filtros devuelve todas.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "buyerCompanyId", in = ParameterIn.QUERY, required = false, description = "Filtro opcional por empresa compradora.", example = "101"),
            @Parameter(name = "providerId", in = ParameterIn.QUERY, required = false, description = "Filtro opcional por empresa proveedora.", example = "202")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.FuelRequestResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":5101,\"buyerCompanyId\":101,\"providerId\":202,\"equipmentId\":305,\"fuelProductId\":303,\"fuelType\":\"DIESEL\",\"productName\":\"Diesel B5 S-50\",\"quantity\":1000,\"unit\":\"L\",\"unitPrice\":4.8,\"deliveryAddress\":\"Av. Separadora Industrial 2450, Villa El Salvador, Lima\",\"deliveryDate\":\"2026-10-15\",\"status\":\"PENDING\",\"source\":\"MANUAL\",\"rejectionReason\":null,\"createdAt\":\"2026-10-09T14:30:00.000Z\",\"updatedAt\":\"2026-10-09T14:30:00.000Z\"}]"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping
    public List<FuelRequestResource> findAll(@RequestParam(required = false) Long buyerCompanyId,
                                             @RequestParam(required = false) Long providerId) {
        return service.findAll(buyerCompanyId, providerId).stream().map(FuelRequestsController::toResource).toList();
    }

    @Operation(
        summary = "Aceptar solicitud de combustible",
        description = "Acepta una solicitud PENDING, crea un pedido con el precio vigente del producto y marca la solicitud como APPROVED. Devuelve el pedido creado. No requiere cuerpo.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "requestId", in = ParameterIn.PATH, required = true, description = "Identificador de la solicitud de combustible.", example = "5101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.FuelOrderResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":4101,\"requestId\":5101,\"companyId\":101,\"providerId\":202,\"fuelProductId\":303,\"equipmentId\":305,\"requestedQuantity\":1000,\"totalPrice\":4800,\"status\":\"PENDING\",\"deliveryAddress\":\"Av. Separadora Industrial 2450, Villa El Salvador, Lima\",\"scheduledDate\":\"2026-10-15\"}"))),
        @ApiResponse(responseCode = "400", description = "La solicitud o su producto no existen.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "La solicitud ya no está PENDING o ocurrió un error al crear el pedido.", content = @Content)
    })
    @PostMapping("/{requestId}/accept")
    public ResponseEntity<?> accept(@PathVariable Long requestId) {
        return ResponseEntity.ok(FuelOrderResourceFromEntityAssembler.toResourceFromEntity(service.accept(requestId)));
    }

    @Operation(
        summary = "Rechazar solicitud de combustible",
        description = "Rechaza una solicitud PENDING y registra un motivo obligatorio, eliminando espacios al inicio y al final.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "requestId", in = ParameterIn.PATH, required = true, description = "Identificador de la solicitud de combustible.", example = "5101")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para rechazar solicitud de combustible.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.RejectFuelRequestResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"reason\":\"No hay disponibilidad para la fecha solicitada.\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.FuelRequestResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":5101,\"buyerCompanyId\":101,\"providerId\":202,\"equipmentId\":305,\"fuelProductId\":303,\"fuelType\":\"DIESEL\",\"productName\":\"Diesel B5 S-50\",\"quantity\":1000,\"unit\":\"L\",\"unitPrice\":4.8,\"deliveryAddress\":\"Av. Separadora Industrial 2450, Villa El Salvador, Lima\",\"deliveryDate\":\"2026-10-15\",\"status\":\"REJECTED\",\"source\":\"MANUAL\",\"rejectionReason\":\"No hay disponibilidad para la fecha solicitada.\",\"createdAt\":\"2026-10-09T14:30:00.000Z\",\"updatedAt\":\"2026-10-09T14:30:00.000Z\"}"))),
        @ApiResponse(responseCode = "400", description = "La solicitud no existe o el motivo de rechazo está vacío.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "La solicitud ya no está PENDING o ocurrió un error al persistir el rechazo.", content = @Content)
    })
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<FuelRequestResource> reject(@PathVariable Long requestId,
                                                      @RequestBody RejectFuelRequestResource resource) {
        return ResponseEntity.ok(toResource(service.reject(requestId, resource.reason())));
    }

    private static FuelRequestResource toResource(FuelRequestPersistenceEntity r) {
        return new FuelRequestResource(r.getId(), r.getBuyerCompanyId(), r.getProviderId(), r.getEquipmentId(),
                r.getFuelProductId(), r.getFuelType(), r.getProductName(), r.getQuantity(), r.getUnit(),
                r.getUnitPrice(), r.getDeliveryAddress(), r.getDeliveryDate(), r.getStatus(), r.getSource(),
                r.getRejectionReason(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
