package com.halofuel.fuelbridge.platform.payment.interfaces.rest;

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

import com.halofuel.fuelbridge.platform.payment.application.commandservices.PaymentCommandService;
import com.halofuel.fuelbridge.platform.payment.application.queryservices.PaymentQueryService;
import com.halofuel.fuelbridge.platform.payment.domain.model.commands.CompletePaymentCommand;
import com.halofuel.fuelbridge.platform.payment.domain.model.commands.RefundPaymentCommand;
import com.halofuel.fuelbridge.platform.payment.domain.model.queries.GetAllPaymentsQuery;
import com.halofuel.fuelbridge.platform.payment.domain.model.queries.GetPaymentByIdQuery;
import com.halofuel.fuelbridge.platform.payment.domain.model.queries.GetPaymentByOrderIdQuery;
import com.halofuel.fuelbridge.platform.payment.domain.model.queries.GetPaymentsByCompanyIdQuery;
import com.halofuel.fuelbridge.platform.payment.interfaces.rest.resources.CompletePaymentResource;
import com.halofuel.fuelbridge.platform.payment.interfaces.rest.resources.CreatePaymentResource;
import com.halofuel.fuelbridge.platform.payment.interfaces.rest.resources.PaymentResource;
import com.halofuel.fuelbridge.platform.payment.interfaces.rest.transform.CreatePaymentCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.payment.interfaces.rest.transform.PaymentResourceFromEntityAssembler;
import com.halofuel.fuelbridge.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/payments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Payments", description = "Payment management endpoints")
public class PaymentsController {

    private final PaymentCommandService paymentCommandService;
    private final PaymentQueryService paymentQueryService;

    public PaymentsController(PaymentCommandService paymentCommandService,
                              PaymentQueryService paymentQueryService) {
        this.paymentCommandService = paymentCommandService;
        this.paymentQueryService = paymentQueryService;
    }

    @Operation(
        summary = "Registrar pago",
        description = "Crea un pago PENDING para el pedido, empresa compradora, importe y método indicados. Rechaza un segundo pago para el mismo pedido.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para registrar pago.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.payment.interfaces.rest.resources.CreatePaymentResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"orderId\":4101,\"companyId\":101,\"amount\":4800,\"paymentMethod\":\"BANK_TRANSFER\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.payment.interfaces.rest.resources.PaymentResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":6101,\"orderId\":4101,\"companyId\":101,\"amount\":4800,\"status\":\"PENDING\",\"paymentMethod\":\"BANK_TRANSFER\",\"transactionReference\":null,\"paidAt\":null}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "409", description = "La operación entra en conflicto con un registro existente o con la disponibilidad del recurso.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PostMapping
    public ResponseEntity<?> createPayment(@RequestBody CreatePaymentResource resource) {
        var command = CreatePaymentCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = paymentCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                PaymentResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.CREATED);
    }

    @Operation(
        summary = "Completar pago",
        description = "Marca el pago como COMPLETED, registra la referencia de transacción y fecha de pago, y cambia el pedido asociado a PAID.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "paymentId", in = ParameterIn.PATH, required = true, description = "Identificador del pago.", example = "6101")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para completar pago.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.payment.interfaces.rest.resources.CompletePaymentResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"transactionReference\":\"BCP-20261009-004801\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.payment.interfaces.rest.resources.PaymentResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":6101,\"orderId\":4101,\"companyId\":101,\"amount\":4800,\"status\":\"COMPLETED\",\"paymentMethod\":\"BANK_TRANSFER\",\"transactionReference\":\"BCP-20261009-004801\",\"paidAt\":\"2026-10-09T14:30:00\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PostMapping("/{paymentId}/complete")
    public ResponseEntity<?> completePayment(@PathVariable Long paymentId,
                                             @RequestBody CompletePaymentResource resource) {
        var result = paymentCommandService.handle(new CompletePaymentCommand(paymentId, resource.transactionReference()));
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                PaymentResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK);
    }

    @Operation(
        summary = "Reembolsar pago",
        description = "Marca como REFUNDED un pago que esté COMPLETED. No requiere cuerpo.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "paymentId", in = ParameterIn.PATH, required = true, description = "Identificador del pago.", example = "6101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.payment.interfaces.rest.resources.PaymentResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":6101,\"orderId\":4101,\"companyId\":101,\"amount\":4800,\"status\":\"REFUNDED\",\"paymentMethod\":\"BANK_TRANSFER\",\"transactionReference\":\"BCP-20261009-004801\",\"paidAt\":\"2026-10-09T14:30:00\"}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<?> refundPayment(@PathVariable Long paymentId) {
        var result = paymentCommandService.handle(new RefundPaymentCommand(paymentId));
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                PaymentResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK);
    }

    @Operation(
        summary = "Listar pagos",
        description = "Devuelve los pagos registrados con su importe, método y estado.",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.payment.interfaces.rest.resources.PaymentResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":6101,\"orderId\":4101,\"companyId\":101,\"amount\":4800,\"status\":\"PENDING\",\"paymentMethod\":\"BANK_TRANSFER\",\"transactionReference\":null,\"paidAt\":null}]"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<PaymentResource>> getAllPayments() {
        var payments = paymentQueryService.handle(new GetAllPaymentsQuery());
        var resources = payments.stream().map(PaymentResourceFromEntityAssembler::toResourceFromEntity).toList();
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
        summary = "Consultar pago",
        description = "Devuelve el pago identificado y, cuando existan, la referencia de transacción y fecha de pago.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "paymentId", in = ParameterIn.PATH, required = true, description = "Identificador del pago.", example = "6101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.payment.interfaces.rest.resources.PaymentResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":6101,\"orderId\":4101,\"companyId\":101,\"amount\":4800,\"status\":\"PENDING\",\"paymentMethod\":\"BANK_TRANSFER\",\"transactionReference\":null,\"paidAt\":null}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResource> getPaymentById(@PathVariable Long paymentId) {
        var result = paymentQueryService.handle(new GetPaymentByIdQuery(paymentId));
        return result.map(p -> new ResponseEntity<>(
                        PaymentResourceFromEntityAssembler.toResourceFromEntity(p), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(
        summary = "Consultar pago de un pedido",
        description = "Devuelve el pago asociado al pedido indicado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "orderId", in = ParameterIn.PATH, required = true, description = "Identificador del pedido de combustible.", example = "4101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.payment.interfaces.rest.resources.PaymentResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":6101,\"orderId\":4101,\"companyId\":101,\"amount\":4800,\"status\":\"PENDING\",\"paymentMethod\":\"BANK_TRANSFER\",\"transactionReference\":null,\"paidAt\":null}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResource> getPaymentByOrder(@PathVariable Long orderId) {
        var result = paymentQueryService.handle(new GetPaymentByOrderIdQuery(orderId));
        return result.map(p -> new ResponseEntity<>(
                        PaymentResourceFromEntityAssembler.toResourceFromEntity(p), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(
        summary = "Listar pagos de una empresa",
        description = "Devuelve los pagos de la empresa compradora; devuelve una lista vacía si no hay pagos.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "companyId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa compradora.", example = "101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.payment.interfaces.rest.resources.PaymentResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":6101,\"orderId\":4101,\"companyId\":101,\"amount\":4800,\"status\":\"PENDING\",\"paymentMethod\":\"BANK_TRANSFER\",\"transactionReference\":null,\"paidAt\":null}]"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
    })
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<PaymentResource>> getPaymentsByCompany(@PathVariable Long companyId) {
        var payments = paymentQueryService.handle(new GetPaymentsByCompanyIdQuery(companyId));
        var resources = payments.stream().map(PaymentResourceFromEntityAssembler::toResourceFromEntity).toList();
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }
}
