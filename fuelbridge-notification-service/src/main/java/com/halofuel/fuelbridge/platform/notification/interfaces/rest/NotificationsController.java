package com.halofuel.fuelbridge.platform.notification.interfaces.rest;

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

import com.halofuel.fuelbridge.platform.notification.application.commandservices.NotificationCommandService;
import com.halofuel.fuelbridge.platform.notification.application.queryservices.NotificationQueryService;
import com.halofuel.fuelbridge.platform.notification.domain.model.commands.MarkNotificationAsReadCommand;
import com.halofuel.fuelbridge.platform.notification.domain.model.queries.GetNotificationByIdQuery;
import com.halofuel.fuelbridge.platform.notification.domain.model.queries.GetNotificationsByUserIdQuery;
import com.halofuel.fuelbridge.platform.notification.domain.model.queries.GetUnreadNotificationsByUserIdQuery;
import com.halofuel.fuelbridge.platform.notification.interfaces.rest.resources.CreateNotificationResource;
import com.halofuel.fuelbridge.platform.notification.interfaces.rest.resources.NotificationResource;
import com.halofuel.fuelbridge.platform.notification.interfaces.rest.transform.CreateNotificationCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.notification.interfaces.rest.transform.NotificationResourceFromEntityAssembler;
import com.halofuel.fuelbridge.platform.notification.application.outboundservices.RecipientDirectory;
import com.halofuel.fuelbridge.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@io.swagger.v3.oas.annotations.security.SecurityScheme(name = "bearerAuth", type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT emitido por FuelBridge API y validado con AUTHORIZATION_JWT_SECRET.")
@RestController
@RequestMapping(value = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationsController {

    private final NotificationCommandService notificationCommandService;
    private final NotificationQueryService notificationQueryService;
    private final RecipientDirectory userRepository;

    public NotificationsController(NotificationCommandService notificationCommandService,
                                   NotificationQueryService notificationQueryService,
                                   RecipientDirectory userRepository) {
        this.notificationCommandService = notificationCommandService;
        this.notificationQueryService = notificationQueryService;
        this.userRepository = userRepository;
    }

    @Operation(
        summary = "Crear notificación",
        description = "Guarda una notificación inicialmente no leída. Usa userId si está presente; de lo contrario resuelve companyId y luego providerId consultando IAM. referenceId permite vincularla a un pedido u otro recurso.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para crear notificación.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.notification.interfaces.rest.resources.CreateNotificationResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"userId\":1101,\"companyId\":null,\"providerId\":null,\"type\":\"ORDER_DISPATCHED\",\"title\":\"Pedido despachado\",\"message\":\"El pedido número 4101 ha sido despachado.\",\"referenceId\":4101}"), @ExampleObject(name = "porComprador", value = "{\"companyId\":101,\"type\":\"ORDER_CONFIRMED\",\"title\":\"Pedido confirmado\",\"message\":\"El pedido número 4101 ha sido confirmado.\",\"referenceId\":4101}"), @ExampleObject(name = "porProveedor", value = "{\"providerId\":202,\"type\":\"PAYMENT_RECEIVED\",\"title\":\"Pago recibido\",\"message\":\"Se recibió el pago del pedido 4101.\",\"referenceId\":4101}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.notification.interfaces.rest.resources.NotificationResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":1301,\"userId\":1101,\"type\":\"ORDER_DISPATCHED\",\"title\":\"Pedido despachado\",\"message\":\"El pedido número 4101 ha sido despachado.\",\"read\":false,\"referenceId\":4101,\"createdAt\":\"2026-10-09T14:30:00Z\"}"))),
        @ApiResponse(responseCode = "400", description = "Destinatario inexistente: devuelve el texto literal Notification recipient does not exist con Content-Type application/json, sin serializarlo como JSON. Un cuerpo HTTP mal formado puede terminar en 401 sin cuerpo al despachar /error.", content = @Content(mediaType = "application/json", examples = { @ExampleObject(name = "destinatarioNoEncontrado", description = "Texto literal del controlador; no es un objeto ErrorResource ni una cadena JSON entre comillas.", value = "Notification recipient does not exist") })),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: crear notificación", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: crear notificación\",\"details\":\"Ocurrió un error al procesar los datos de notificación en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "Un fallo de IAM origina un 502 interno, pero el despacho a /error está protegido: actualmente termina en 401 sin cuerpo de respuesta, en lugar de este 502.", content = @Content)
    })
    @PostMapping
    public ResponseEntity<?> createNotification(@RequestBody CreateNotificationResource resource) {
        var userId = resolveUserId(resource);
        if (userId == null) {
            return ResponseEntity.badRequest().body("Notification recipient does not exist");
        }
        var resolved = new CreateNotificationResource(userId, resource.companyId(), resource.providerId(),
                resource.type(), resource.title(), resource.message(), resource.referenceId());
        var command = CreateNotificationCommandFromResourceAssembler.toCommandFromResource(resolved);
        var result = notificationCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                NotificationResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.CREATED);
    }

    @Operation(
        summary = "Marcar notificación como leída",
        description = "Marca como leída una notificación existente y devuelve su estado actualizado. Repetir la operación mantiene read=true. No requiere cuerpo.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "notificationId", in = ParameterIn.PATH, required = true, description = "Identificador del notificación.", example = "1301")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.notification.interfaces.rest.resources.NotificationResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":1301,\"userId\":1101,\"type\":\"ORDER_DISPATCHED\",\"title\":\"Pedido despachado\",\"message\":\"El pedido número 4101 ha sido despachado.\",\"read\":true,\"referenceId\":4101,\"createdAt\":\"2026-10-09T14:30:00Z\"}"))),
        @ApiResponse(responseCode = "400", description = "Un parámetro o cuerpo HTTP inválido origina un error de validación, pero el despacho a /error está protegido: actualmente termina en 401 sin cuerpo de respuesta, en lugar de este 400.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Notificación no encontrada", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"NOTIFICATION_NOT_FOUND\",\"message\":\"Notificación no encontrada\",\"details\":\"No existe notificación con identificador 1301.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: marcar notificación como leída", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: marcar notificación como leída\",\"details\":\"Ocurrió un error al procesar los datos de notificación en FuelBridge.\"}")))
    })
    @PostMapping("/{notificationId}/mark-as-read")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId) {
        var result = notificationCommandService.handle(new MarkNotificationAsReadCommand(notificationId));
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                NotificationResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK);
    }

    @Operation(
        summary = "Consultar notificación",
        description = "Devuelve la notificación identificada, incluido su estado de lectura y recurso de referencia.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "notificationId", in = ParameterIn.PATH, required = true, description = "Identificador del notificación.", example = "1301")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.notification.interfaces.rest.resources.NotificationResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":1301,\"userId\":1101,\"type\":\"ORDER_DISPATCHED\",\"title\":\"Pedido despachado\",\"message\":\"El pedido número 4101 ha sido despachado.\",\"read\":false,\"referenceId\":4101,\"createdAt\":\"2026-10-09T14:30:00Z\"}"))),
        @ApiResponse(responseCode = "400", description = "Un parámetro o cuerpo HTTP inválido origina un error de validación, pero el despacho a /error está protegido: actualmente termina en 401 sin cuerpo de respuesta, en lugar de este 400.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado: sin cuerpo de respuesta.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Un error no controlado origina un 500 interno, pero el despacho a /error está protegido: actualmente termina en 401 sin cuerpo de respuesta, en lugar de este 500.", content = @Content)
    })
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResource> getNotificationById(@PathVariable Long notificationId) {
        var result = notificationQueryService.handle(new GetNotificationByIdQuery(notificationId));
        return result.map(n -> new ResponseEntity<>(
                        NotificationResourceFromEntityAssembler.toResourceFromEntity(n), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(
        summary = "Listar notificaciones de un usuario",
        description = "Devuelve todas las notificaciones del usuario identificado, leídas y no leídas.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "userId", in = ParameterIn.PATH, required = true, description = "Identificador del usuario destinatario.", example = "1101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.notification.interfaces.rest.resources.NotificationResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":1301,\"userId\":1101,\"type\":\"ORDER_DISPATCHED\",\"title\":\"Pedido despachado\",\"message\":\"El pedido número 4101 ha sido despachado.\",\"read\":false,\"referenceId\":4101,\"createdAt\":\"2026-10-09T14:30:00Z\"}]"))),
        @ApiResponse(responseCode = "400", description = "Un parámetro o cuerpo HTTP inválido origina un error de validación, pero el despacho a /error está protegido: actualmente termina en 401 sin cuerpo de respuesta, en lugar de este 400.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Un error no controlado origina un 500 interno, pero el despacho a /error está protegido: actualmente termina en 401 sin cuerpo de respuesta, en lugar de este 500.", content = @Content)
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResource>> getNotificationsByUser(@PathVariable Long userId) {
        var notifications = notificationQueryService.handle(new GetNotificationsByUserIdQuery(userId));
        var resources = notifications.stream().map(NotificationResourceFromEntityAssembler::toResourceFromEntity).toList();
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
        summary = "Listar notificaciones de un comprador",
        description = "Resuelve el usuario de la empresa compradora consultando IAM y devuelve sus notificaciones. Si no existe usuario asociado devuelve una lista vacía.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "companyId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa compradora.", example = "101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.notification.interfaces.rest.resources.NotificationResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":1301,\"userId\":1101,\"type\":\"ORDER_DISPATCHED\",\"title\":\"Pedido despachado\",\"message\":\"El pedido número 4101 ha sido despachado.\",\"read\":false,\"referenceId\":4101,\"createdAt\":\"2026-10-09T14:30:00Z\"}]"))),
        @ApiResponse(responseCode = "400", description = "Un parámetro o cuerpo HTTP inválido origina un error de validación, pero el despacho a /error está protegido: actualmente termina en 401 sin cuerpo de respuesta, en lugar de este 400.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Un error no controlado origina un 500 interno, pero el despacho a /error está protegido: actualmente termina en 401 sin cuerpo de respuesta, en lugar de este 500.", content = @Content),
        @ApiResponse(responseCode = "502", description = "Un fallo de IAM origina un 502 interno, pero el despacho a /error está protegido: actualmente termina en 401 sin cuerpo de respuesta, en lugar de este 502.", content = @Content)
    })
    @GetMapping("/buyer/{companyId}")
    public ResponseEntity<List<NotificationResource>> getNotificationsByBuyer(@PathVariable Long companyId) {
        return userRepository.findByCompanyId(companyId)
                .map(user -> getNotificationsByUser(user))
                .orElse(ResponseEntity.ok(List.of()));
    }

    @Operation(
        summary = "Listar notificaciones de un proveedor",
        description = "Resuelve el usuario del proveedor consultando IAM y devuelve sus notificaciones. Si no existe usuario asociado devuelve una lista vacía.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "providerId", in = ParameterIn.PATH, required = true, description = "Identificador de la empresa proveedora.", example = "202")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.notification.interfaces.rest.resources.NotificationResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":1301,\"userId\":1101,\"type\":\"ORDER_DISPATCHED\",\"title\":\"Pedido despachado\",\"message\":\"El pedido número 4101 ha sido despachado.\",\"read\":false,\"referenceId\":4101,\"createdAt\":\"2026-10-09T14:30:00Z\"}]"))),
        @ApiResponse(responseCode = "400", description = "Un parámetro o cuerpo HTTP inválido origina un error de validación, pero el despacho a /error está protegido: actualmente termina en 401 sin cuerpo de respuesta, en lugar de este 400.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Un error no controlado origina un 500 interno, pero el despacho a /error está protegido: actualmente termina en 401 sin cuerpo de respuesta, en lugar de este 500.", content = @Content),
        @ApiResponse(responseCode = "502", description = "Un fallo de IAM origina un 502 interno, pero el despacho a /error está protegido: actualmente termina en 401 sin cuerpo de respuesta, en lugar de este 502.", content = @Content)
    })
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<NotificationResource>> getNotificationsByProvider(@PathVariable Long providerId) {
        return userRepository.findByProviderId(providerId)
                .map(user -> getNotificationsByUser(user))
                .orElse(ResponseEntity.ok(List.of()));
    }

    @Operation(
        summary = "Listar notificaciones no leídas",
        description = "Devuelve únicamente las notificaciones con read=false del usuario identificado.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "userId", in = ParameterIn.PATH, required = true, description = "Identificador del usuario destinatario.", example = "1101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.notification.interfaces.rest.resources.NotificationResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":1301,\"userId\":1101,\"type\":\"ORDER_DISPATCHED\",\"title\":\"Pedido despachado\",\"message\":\"El pedido número 4101 ha sido despachado.\",\"read\":false,\"referenceId\":4101,\"createdAt\":\"2026-10-09T14:30:00Z\"}]"))),
        @ApiResponse(responseCode = "400", description = "Un parámetro o cuerpo HTTP inválido origina un error de validación, pero el despacho a /error está protegido: actualmente termina en 401 sin cuerpo de respuesta, en lugar de este 400.", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "500", description = "Un error no controlado origina un 500 interno, pero el despacho a /error está protegido: actualmente termina en 401 sin cuerpo de respuesta, en lugar de este 500.", content = @Content)
    })
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationResource>> getUnreadByUser(@PathVariable Long userId) {
        var notifications = notificationQueryService.handle(new GetUnreadNotificationsByUserIdQuery(userId));
        var resources = notifications.stream().map(NotificationResourceFromEntityAssembler::toResourceFromEntity).toList();
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    private Long resolveUserId(CreateNotificationResource resource) {
        if (resource.userId() != null) return resource.userId();
        if (resource.companyId() != null) {
            return userRepository.findByCompanyId(resource.companyId()).map(user -> user).orElse(null);
        }
        if (resource.providerId() != null) {
            return userRepository.findByProviderId(resource.providerId()).map(user -> user).orElse(null);
        }
        return null;
    }
}
