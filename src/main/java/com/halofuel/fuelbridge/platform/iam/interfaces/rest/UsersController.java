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

import com.halofuel.fuelbridge.platform.iam.application.commandservices.UserCommandService;
import com.halofuel.fuelbridge.platform.iam.application.queryservices.UserQueryService;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.UpdateUserProfileResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.UserProfileResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.UpdateUserProfileCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.UserProfileResourceFromEntityAssembler;
import com.halofuel.fuelbridge.platform.shared.interfaces.rest.resources.ErrorResource;
import com.halofuel.fuelbridge.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final UserCommandService userCommandService;

    public UsersController(UserQueryService userQueryService, UserCommandService userCommandService) {
        this.userQueryService = userQueryService;
        this.userCommandService = userCommandService;
    }

    @Operation(
        summary = "Listar usuarios",
        description = "Devuelve los usuarios registrados con sus roles y vínculos empresariales; excluye contraseñas y tokens.",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.UserResource.class)), examples = @ExampleObject(name = "respuesta", value = "[{\"id\":1101,\"username\":\"compras.costasur\",\"roles\":[\"ROLE_BUYER\"],\"companyId\":101,\"providerId\":null}]"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
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
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado: 401 sin cuerpo de respuesta; el despacho a /error vuelve a pasar por la seguridad actual.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado: sin cuerpo de respuesta.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: consultar usuario", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: consultar usuario\",\"details\":\"Ocurrió un error al procesar los datos de usuario en FuelBridge.\"}")))
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserResource> getUserById(@PathVariable Long userId) {
        var result = userQueryService.handle(new GetUserByIdQuery(userId));
        return result.map(user -> new ResponseEntity<>(
                        UserResourceFromEntityAssembler.toResourceFromEntity(user), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(
        summary = "Editar perfil de usuario",
        description = "Actualiza el nombre completo y el teléfono del usuario. Solo el propio usuario autenticado puede editar su perfil. fullName es obligatorio; un phone vacío lo elimina.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "userId", in = ParameterIn.PATH, required = true, description = "Identificador del usuario cuyo perfil se edita.", example = "1101")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos editables del perfil.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateUserProfileResource.class),
                examples = @ExampleObject(name = "principal", value = "{\"fullName\":\"Rosa Medina Paredes\",\"phone\":\"+51987654321\"}")))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil actualizado.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":1101,\"username\":\"compras.costasur\",\"fullName\":\"Rosa Medina Paredes\",\"phone\":\"+51987654321\"}"))),
        @ApiResponse(responseCode = "400", description = "Datos de perfil inválidos (nombre vacío o demasiado largo, teléfono demasiado largo).", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado.", content = @Content),
        @ApiResponse(responseCode = "403", description = "El usuario autenticado intenta editar el perfil de otro usuario.", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "El usuario no existe.", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{userId}/profile")
    public ResponseEntity<?> updateUserProfile(@PathVariable Long userId,
                                               @RequestBody UpdateUserProfileResource resource,
                                               Authentication authentication) {
        // Only the owner may edit a profile. A missing user falls through to the command
        // service so it answers 404.
        var target = userQueryService.handle(new GetUserByIdQuery(userId));
        if (target.isPresent() && !target.get().getUsername().equals(authentication.getName())) {
            return new ResponseEntity<>(
                    new ErrorResource("USER_FORBIDDEN", "Access denied",
                            "Only the profile owner can edit this profile"),
                    HttpStatus.FORBIDDEN);
        }
        var command = UpdateUserProfileCommandFromResourceAssembler.toCommandFromResource(userId, resource);
        var result = userCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                UserProfileResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK);
    }
}
