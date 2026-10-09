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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Destinatario de la notificación no encontrado", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Si no existe el destinatario, el controlador actual devuelve texto.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Destinatario de la notificación no encontrado\",\"details\":\"La empresa compradora companyId=101 no tiene un usuario asociado.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para crear notificación.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite crear notificación.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: crear notificación", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: crear notificación\",\"details\":\"Ocurrió un error al procesar los datos de notificación en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo consultar el servicio requerido por FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores del cliente HTTP actual usan el formato de Spring.", value = "{\"code\":\"BAD_GATEWAY\",\"message\":\"No se pudo consultar el servicio requerido por FuelBridge\",\"details\":\"El servicio remoto no respondió correctamente al intentar crear notificación.\"}")))
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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Solicitud inválida para marcar notificación como leída", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores de parámetros o del cuerpo HTTP pueden usar el formato de Spring.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para marcar notificación como leída\",\"details\":\"El parámetro notificationId=abc no es un identificador numérico válido de notificación.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para marcar notificación como leída.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite marcar notificación como leída.\"}"))),
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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Solicitud inválida para consultar notificación", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores de parámetros o del cuerpo HTTP pueden usar el formato de Spring.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para consultar notificación\",\"details\":\"El parámetro notificationId=abc no es un identificador numérico válido de notificación.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para consultar notificación.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite consultar notificación.\"}"))),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Notificación no encontrada", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El controlador actual puede devolver 404 sin cuerpo.", value = "{\"code\":\"NOTIFICATION_NOT_FOUND\",\"message\":\"Notificación no encontrada\",\"details\":\"No existe notificación con identificador 1301.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: consultar notificación", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El formato del error inesperado lo determina el manejador de Spring.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar notificación\",\"details\":\"Ocurrió un error al procesar los datos de notificación en FuelBridge.\"}")))
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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Solicitud inválida para listar notificaciones de un usuario", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores de parámetros o del cuerpo HTTP pueden usar el formato de Spring.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para listar notificaciones de un usuario\",\"details\":\"El parámetro userId=abc no es un identificador numérico válido de notificación.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para listar notificaciones de un usuario.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite listar notificaciones de un usuario.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: listar notificaciones de un usuario", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El formato del error inesperado lo determina el manejador de Spring.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar notificaciones de un usuario\",\"details\":\"Ocurrió un error al procesar los datos de notificación en FuelBridge.\"}")))
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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Solicitud inválida para listar notificaciones de un comprador", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores de parámetros o del cuerpo HTTP pueden usar el formato de Spring.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para listar notificaciones de un comprador\",\"details\":\"El parámetro companyId=abc no es un identificador numérico válido de notificación.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para listar notificaciones de un comprador.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite listar notificaciones de un comprador.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: listar notificaciones de un comprador", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El formato del error inesperado lo determina el manejador de Spring.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar notificaciones de un comprador\",\"details\":\"Ocurrió un error al procesar los datos de notificación en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo consultar el servicio requerido por FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores del cliente HTTP actual usan el formato de Spring.", value = "{\"code\":\"BAD_GATEWAY\",\"message\":\"No se pudo consultar el servicio requerido por FuelBridge\",\"details\":\"El servicio remoto no respondió correctamente al intentar listar notificaciones de un comprador.\"}")))
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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Solicitud inválida para listar notificaciones de un proveedor", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores de parámetros o del cuerpo HTTP pueden usar el formato de Spring.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para listar notificaciones de un proveedor\",\"details\":\"El parámetro providerId=abc no es un identificador numérico válido de notificación.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para listar notificaciones de un proveedor.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite listar notificaciones de un proveedor.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: listar notificaciones de un proveedor", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El formato del error inesperado lo determina el manejador de Spring.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar notificaciones de un proveedor\",\"details\":\"Ocurrió un error al procesar los datos de notificación en FuelBridge.\"}"))),
        @ApiResponse(responseCode = "502", description = "No se pudo obtener una respuesta válida del servicio del que depende esta operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo consultar el servicio requerido por FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores del cliente HTTP actual usan el formato de Spring.", value = "{\"code\":\"BAD_GATEWAY\",\"message\":\"No se pudo consultar el servicio requerido por FuelBridge\",\"details\":\"El servicio remoto no respondió correctamente al intentar listar notificaciones de un proveedor.\"}")))
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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Solicitud inválida para listar notificaciones no leídas", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). Los errores de parámetros o del cuerpo HTTP pueden usar el formato de Spring.", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para listar notificaciones no leídas\",\"details\":\"El parámetro userId=abc no es un identificador numérico válido de notificación.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para listar notificaciones no leídas.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite listar notificaciones no leídas.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "No se pudo completar la operación: listar notificaciones no leídas", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El formato del error inesperado lo determina el manejador de Spring.", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar notificaciones no leídas\",\"details\":\"Ocurrió un error al procesar los datos de notificación en FuelBridge.\"}")))
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
