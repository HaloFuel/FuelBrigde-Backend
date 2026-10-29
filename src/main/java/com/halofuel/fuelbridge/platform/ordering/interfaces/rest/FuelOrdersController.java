package com.halofuel.fuelbridge.platform.ordering.interfaces.rest;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.halofuel.fuelbridge.platform.ordering.application.commandservices.FuelOrderCommandService;
import com.halofuel.fuelbridge.platform.ordering.application.queryservices.FuelOrderQueryService;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.CancelFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.ConfirmFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.commands.DispatchFuelOrderCommand;
import com.halofuel.fuelbridge.platform.ordering.domain.model.queries.GetAllFuelOrdersQuery;
import com.halofuel.fuelbridge.platform.ordering.domain.model.queries.GetFuelOrderByIdQuery;
import com.halofuel.fuelbridge.platform.ordering.domain.model.queries.GetFuelOrdersByCompanyIdQuery;
import com.halofuel.fuelbridge.platform.ordering.domain.model.queries.GetFuelOrdersByProviderIdQuery;
import com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.CreateFuelOrderResource;
import com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.FuelOrderResource;
import com.halofuel.fuelbridge.platform.ordering.interfaces.rest.transform.CreateFuelOrderCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.ordering.interfaces.rest.transform.FuelOrderResourceFromEntityAssembler;
import com.halofuel.fuelbridge.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de pedidos de combustible (Fuel Orders).
 */
@RestController
@RequestMapping(value = "/api/v1/fuel-orders", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Fuel Orders", description = "Ordering management endpoints")
public class FuelOrdersController {

    private final FuelOrderCommandService fuelOrderCommandService;
    private final FuelOrderQueryService fuelOrderQueryService;

    public FuelOrdersController(FuelOrderCommandService fuelOrderCommandService,
                                FuelOrderQueryService fuelOrderQueryService) {
        this.fuelOrderCommandService = fuelOrderCommandService;
        this.fuelOrderQueryService = fuelOrderQueryService;
    }

    @Operation(
        summary = "Crear pedido de combustible",
        description = "Registra un pedido PENDING y calcula el precio total usando el precio vigente del producto y la cantidad solicitada.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para crear pedido de combustible.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.CreateFuelOrderResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"companyId\":101,\"providerId\":202,\"fuelProductId\":303,\"equipmentId\":305,\"requestedQuantity\":1000,\"deliveryAddress\":\"Av. Separadora Industrial 2450, Villa El Salvador, Lima\",\"scheduledDate\":\"2026-10-15\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.FuelOrderResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":4101,\"requestId\":null,\"companyId\":101,\"providerId\":202,\"fuelProductId\":303,\"equipmentId\":305,\"requestedQuantity\":1000,\"totalPrice\":4800,\"status\":\"PENDING\",\"deliveryAddress\":\"Av. Separadora Industrial 2450, Villa El Salvador, Lima\",\"scheduledDate\":\"2026-10-15\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Cantidad de combustible inválida", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Cantidad de combustible inválida\",\"details\":\"requestedQuantity debe ser mayor que cero para el pedido de Diésel B5 S-50.\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Producto de combustible no encontrado", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"FUELPRODUCT_NOT_FOUND\",\"message\":\"Producto de combustible no encontrado\",\"details\":\"No existe producto de combustible con identificador 303.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: crear pedido de combustible", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: crear pedido de combustible\",\"details\":\"Ocurrió un error al procesar los datos de pedido de combustible en FuelBridge.\"}")))
    })
    @PostMapping
    public ResponseEntity<?> createFuelOrder(@RequestBody CreateFuelOrderResource resource) {
        var command = CreateFuelOrderCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = fuelOrderCommandService.handle(command);

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                FuelOrderResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.CREATED);
    }

    @Operation(
        summary = "Confirmar pedido",
        description = "Cambia el estado del pedido a CONFIRMED y publica el evento que solicita una notificación al comprador. No requiere cuerpo.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "orderId", in = ParameterIn.PATH, required = true, description = "Identificador del pedido de combustible.", example = "4101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.FuelOrderResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":4101,\"requestId\":null,\"companyId\":101,\"providerId\":202,\"fuelProductId\":303,\"equipmentId\":305,\"requestedQuantity\":1000,\"totalPrice\":4800,\"status\":\"CONFIRMED\",\"deliveryAddress\":\"Av. Separadora Industrial 2450, Villa El Salvador, Lima\",\"scheduledDate\":\"2026-10-15\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para confirmar pedido", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para confirmar pedido\",\"details\":\"El parámetro orderId=abc no es un identificador numérico válido de pedido de combustible.\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Pedido de combustible no encontrado", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"FUELORDER_NOT_FOUND\",\"message\":\"Pedido de combustible no encontrado\",\"details\":\"No existe pedido de combustible con identificador 4101.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: confirmar pedido", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: confirmar pedido\",\"details\":\"Ocurrió un error al procesar los datos de pedido de combustible en FuelBridge.\"}")))
    })
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<?> confirmOrder(@PathVariable Long orderId) {
        var result = fuelOrderCommandService.handle(new ConfirmFuelOrderCommand(orderId));

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                FuelOrderResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK);
    }

    @Operation(
        summary = "Cancelar pedido",
        description = "Cambia el estado del pedido a CANCELLED y publica el evento que solicita una notificación al comprador. No requiere cuerpo.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "orderId", in = ParameterIn.PATH, required = true, description = "Identificador del pedido de combustible.", example = "4101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.FuelOrderResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":4101,\"requestId\":null,\"companyId\":101,\"providerId\":202,\"fuelProductId\":303,\"equipmentId\":305,\"requestedQuantity\":1000,\"totalPrice\":4800,\"status\":\"CANCELLED\",\"deliveryAddress\":\"Av. Separadora Industrial 2450, Villa El Salvador, Lima\",\"scheduledDate\":\"2026-10-15\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para cancelar pedido", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para cancelar pedido\",\"details\":\"El parámetro orderId=abc no es un identificador numérico válido de pedido de combustible.\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Pedido de combustible no encontrado", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"FUELORDER_NOT_FOUND\",\"message\":\"Pedido de combustible no encontrado\",\"details\":\"No existe pedido de combustible con identificador 4101.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: cancelar pedido", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: cancelar pedido\",\"details\":\"Ocurrió un error al procesar los datos de pedido de combustible en FuelBridge.\"}")))
    })
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        var result = fuelOrderCommandService.handle(new CancelFuelOrderCommand(orderId));

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                FuelOrderResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK);
    }

    /**
     * Sprint 1 - US-12: Despacho de pedidos de combustible.
     *
     * Procesa la solicitud de despacho de un pedido existente.
     * El servicio de aplicación se encarga de ejecutar
     * las reglas de negocio correspondientes.
     *
     * @param orderId identificador del pedido de combustible
     * @return respuesta HTTP con el pedido actualizado
     */
    @Operation(
        summary = "Despachar pedido",
        description = "Despacha un pedido PENDING, cambia su estado a DISPATCHED y solicita una notificación al comprador. No requiere cuerpo.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "orderId", in = ParameterIn.PATH, required = true, description = "Identificador del pedido de combustible.", example = "4101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.FuelOrderResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":4101,\"requestId\":null,\"companyId\":101,\"providerId\":202,\"fuelProductId\":303,\"equipmentId\":305,\"requestedQuantity\":1000,\"totalPrice\":4800,\"status\":\"DISPATCHED\",\"deliveryAddress\":\"Av. Separadora Industrial 2450, Villa El Salvador, Lima\",\"scheduledDate\":\"2026-10-15\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para despachar pedido", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para despachar pedido\",\"details\":\"El parámetro orderId=abc no es un identificador numérico válido de pedido de combustible.\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Pedido de combustible no encontrado", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"FUELORDER_NOT_FOUND\",\"message\":\"Pedido de combustible no encontrado\",\"details\":\"No existe pedido de combustible con identificador 4101.\"}"))),
        @ApiResponse(responseCode = "500", description = "Pedido en un estado que no permite despacho, fallo de persistencia o error en la llamada al servicio de notificaciones.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "El pedido no permite el despacho en su estado actual", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"El pedido no permite el despacho en su estado actual\",\"details\":\"El pedido orderId=4101 ya fue despachado; solo un pedido CONFIRMED permite esta transición.\"}")))
    })
    @PostMapping("/{orderId}/dispatch")
    public ResponseEntity<?> dispatchOrder(@PathVariable Long orderId) {
        var result = fuelOrderCommandService.handle(new DispatchFuelOrderCommand(orderId));

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                FuelOrderResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK);
    }

    @Operation(
        summary = "Listar pedidos de combustible",
        description = "Devuelve todos los pedidos registrados con sus cantidades, precios, destinos y estados.",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.FuelOrderResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":4101,\"requestId\":null,\"companyId\":101,\"providerId\":202,\"fuelProductId\":303,\"equipmentId\":305,\"requestedQuantity\":1000,\"totalPrice\":4800,\"status\":\"PENDING\",\"deliveryAddress\":\"Av. Separadora Industrial 2450, Villa El Salvador, Lima\",\"scheduledDate\":\"2026-10-15\"}]"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: listar pedidos de combustible", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar pedidos de combustible\",\"details\":\"Ocurrió un error al procesar los datos de pedido de combustible en FuelBridge.\"}")))
    })
    @GetMapping
    public ResponseEntity<List<FuelOrderResource>> getAllOrders() {
        var orders = fuelOrderQueryService.handle(new GetAllFuelOrdersQuery());
        var resources = orders.stream()
                .map(FuelOrderResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
        summary = "Consultar pedido de combustible",
        description = "Devuelve el pedido identificado con su producto, equipo y fecha programada.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "orderId", in = ParameterIn.PATH, required = true, description = "Identificador del pedido de combustible.", example = "4101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.FuelOrderResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":4101,\"requestId\":null,\"companyId\":101,\"providerId\":202,\"fuelProductId\":303,\"equipmentId\":305,\"requestedQuantity\":1000,\"totalPrice\":4800,\"status\":\"PENDING\",\"deliveryAddress\":\"Av. Separadora Industrial 2450, Villa El Salvador, Lima\",\"scheduledDate\":\"2026-10-15\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para consultar pedido de combustible", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para consultar pedido de combustible\",\"details\":\"El parámetro orderId=abc no es un identificador numérico válido de pedido de combustible.\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado: sin cuerpo de respuesta.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: consultar pedido de combustible", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar pedido de combustible\",\"details\":\"Ocurrió un error al procesar los datos de pedido de combustible en FuelBridge.\"}")))
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<FuelOrderResource> getOrderById(@PathVariable Long orderId) {
        var result = fuelOrderQueryService.handle(new GetFuelOrderByIdQuery(orderId));

        return result.map(o -> new ResponseEntity<>(
                        FuelOrderResourceFromEntityAssembler.toResourceFromEntity(o),
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(
        summary = "Listar pedidos de una empresa",
        description = "Devuelve los pedidos de la empresa compradora; una lista vacía indica que no tiene pedidos.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "companyId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa compradora.", example = "101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.FuelOrderResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":4101,\"requestId\":null,\"companyId\":101,\"providerId\":202,\"fuelProductId\":303,\"equipmentId\":305,\"requestedQuantity\":1000,\"totalPrice\":4800,\"status\":\"PENDING\",\"deliveryAddress\":\"Av. Separadora Industrial 2450, Villa El Salvador, Lima\",\"scheduledDate\":\"2026-10-15\"}]"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para listar pedidos de una empresa", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para listar pedidos de una empresa\",\"details\":\"El parámetro companyId=abc no es un identificador numérico válido de pedido de combustible.\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: listar pedidos de una empresa", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar pedidos de una empresa\",\"details\":\"Ocurrió un error al procesar los datos de pedido de combustible en FuelBridge.\"}")))
    })
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<FuelOrderResource>> getOrdersByCompany(@PathVariable Long companyId) {
        var orders = fuelOrderQueryService.handle(new GetFuelOrdersByCompanyIdQuery(companyId));
        var resources = orders.stream()
                .map(FuelOrderResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
        summary = "Listar pedidos de un proveedor",
        description = "Devuelve los pedidos asignados al proveedor indicado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "providerId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa proveedora.", example = "202")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.ordering.interfaces.rest.resources.FuelOrderResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":4101,\"requestId\":null,\"companyId\":101,\"providerId\":202,\"fuelProductId\":303,\"equipmentId\":305,\"requestedQuantity\":1000,\"totalPrice\":4800,\"status\":\"PENDING\",\"deliveryAddress\":\"Av. Separadora Industrial 2450, Villa El Salvador, Lima\",\"scheduledDate\":\"2026-10-15\"}]"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para listar pedidos de un proveedor", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para listar pedidos de un proveedor\",\"details\":\"El parámetro providerId=abc no es un identificador numérico válido de pedido de combustible.\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: listar pedidos de un proveedor", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar pedidos de un proveedor\",\"details\":\"Ocurrió un error al procesar los datos de pedido de combustible en FuelBridge.\"}")))
    })
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<FuelOrderResource>> getOrdersByProvider(@PathVariable Long providerId) {
        var orders = fuelOrderQueryService.handle(new GetFuelOrdersByProviderIdQuery(providerId));
        var resources = orders.stream()
                .map(FuelOrderResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }
}
