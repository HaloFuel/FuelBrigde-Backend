package com.halofuel.fuelbridge.platform.iam.interfaces.rest;

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

import com.halofuel.fuelbridge.platform.iam.application.queryservices.UserQueryService;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.GetAllUsersQuery;
import com.halofuel.fuelbridge.platform.iam.domain.model.queries.GetUserByIdQuery;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.UserResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Users management endpoints")
public class UsersController {

    private final UserQueryService userQueryService;

    public UsersController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @Operation(
        summary = "Listar usuarios",
        description = "Devuelve los usuarios registrados con sus roles y vínculos empresariales; excluye contraseñas y tokens.",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.UserResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":1101,\"username\":\"compras.costasur\",\"roles\":[\"ROLE_BUYER\"],\"companyId\":101,\"providerId\":null}]"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para listar usuarios.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite listar usuarios.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: listar usuarios", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: listar usuarios\",\"details\":\"Ocurrió un error al procesar los datos de usuario en FuelBridge.\"}")))
    })
    @GetMapping
    public ResponseEntity<List<UserResource>> getAllUsers() {
        var users = userQueryService.handle(new GetAllUsersQuery());
        var resources = users.stream().map(UserResourceFromEntityAssembler::toResourceFromEntity).toList();
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
        summary = "Consultar usuario",
        description = "Devuelve los datos públicos del usuario identificado, sin contraseña.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "userId", in = ParameterIn.PATH, required = true, description = "Identificador del usuario destinatario.", example = "1101")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.UserResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":1101,\"username\":\"compras.costasur\",\"roles\":[\"ROLE_BUYER\"],\"companyId\":101,\"providerId\":null}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para consultar usuario", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para consultar usuario\",\"details\":\"El parámetro userId=abc no es un identificador numérico válido de usuario.\"}"))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT Bearer válido, o el token ha expirado o tiene una firma incorrecta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Se requiere autenticación para acceder a FuelBridge", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El manejador de seguridad actual usa sendError; el cuerpo lo determina Spring.", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticación para acceder a FuelBridge\",\"details\":\"Envíe un JWT vigente emitido por FuelBridge en Authorization: Bearer <token> para consultar usuario.\"}"))),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Acceso a FuelBridge no permitido", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El rechazo actual lo gestiona Spring Security o CORS y puede no incluir cuerpo.", value = "{\"code\":\"FORBIDDEN\",\"message\":\"Acceso a FuelBridge no permitido\",\"details\":\"La política de acceso o el origen CORS no permite consultar usuario.\"}"))),
        @ApiResponse(responseCode = "404", description = "No se encontró el recurso solicitado o una dependencia identificada por la operación.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "errorResourceReferencia", summary = "Usuario no encontrado", description = "Ejemplo de referencia con la estructura ErrorResource (code, message, details). El controlador actual puede devolver 404 sin cuerpo.", value = "{\"code\":\"USER_NOT_FOUND\",\"message\":\"Usuario no encontrado\",\"details\":\"No existe usuario con identificador 1101.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: consultar usuario", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar usuario\",\"details\":\"Ocurrió un error al procesar los datos de usuario en FuelBridge.\"}")))
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserResource> getUserById(@PathVariable Long userId) {
        var result = userQueryService.handle(new GetUserByIdQuery(userId));
        return result.map(user -> new ResponseEntity<>(
                        UserResourceFromEntityAssembler.toResourceFromEntity(user), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
