package com.halofuel.fuelbridge.platform.iam.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.halofuel.fuelbridge.platform.iam.application.commandservices.PasswordResetCommandService;
import com.halofuel.fuelbridge.platform.iam.application.commandservices.RevokedTokenCommandService;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.ForgotPasswordResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.PasswordResetConfirmationResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.PasswordResetTokenResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.ResetPasswordResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.RequestPasswordResetCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.ResetPasswordCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.iam.application.commandservices.UserCommandService;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.SignOutResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.RevokeTokenCommandFromAuthorizationHeaderAssembler;
import org.springframework.web.bind.annotation.RequestHeader;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.SignInResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.SignUpResource;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.SignUpCommandFromResourceAssembler;
import com.halofuel.fuelbridge.platform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import com.halofuel.fuelbridge.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Controlador REST para la gestión de autenticación y acceso (IAM).
 */

@RestController
@RequestMapping(value = "/api/v1/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthenticationController {

    private final UserCommandService userCommandService;
    private final RevokedTokenCommandService revokedTokenCommandService;
    private final PasswordResetCommandService passwordResetCommandService;

    public AuthenticationController(UserCommandService userCommandService,
                                    RevokedTokenCommandService revokedTokenCommandService,
                                    PasswordResetCommandService passwordResetCommandService) {
        this.userCommandService = userCommandService;
        this.revokedTokenCommandService = revokedTokenCommandService;
        this.passwordResetCommandService = passwordResetCommandService;
    }


    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @Operation(
        summary = "Registrar usuario",
        description = "Registra un usuario y sus vínculos con comprador o proveedor. roles admite ROLE_BUYER y ROLE_PROVIDER; si la lista está vacía usa el rol predeterminado. Devuelve el usuario sin contraseña.",
        security = {},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para registrar usuario.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.SignUpResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"username\":\"compras.costasur\",\"password\":\"CostaSur!Combustible2026\",\"roles\":[\"ROLE_BUYER\"],\"companyId\":101,\"providerId\":null}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registro creado correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.UserResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":1101,\"username\":\"compras.costasur\",\"roles\":[\"ROLE_BUYER\"],\"companyId\":101,\"providerId\":null}"))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Solicitud inválida para registrar usuario", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Solicitud inválida para registrar usuario\",\"details\":\"username debe ser un correo válido, por ejemplo operaciones@minerasur.pe; password y roles no pueden estar vacíos.\"}"))),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "Uno o más roles solicitados no están registrados.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Rol no encontrado", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"ROLE_NOT_FOUND\",\"message\":\"Rol no encontrado\",\"details\":\"No existe rol con identificador ROLE_BUYER.\"}"))),
        @ApiResponse(responseCode = "409", description = "La operación entra en conflicto con un registro existente o con la disponibilidad del recurso.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "El usuario ya está registrado", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"USER_CONFLICT\",\"message\":\"El usuario ya está registrado\",\"details\":\"Ya existe una cuenta con el usuario operaciones@minerasur.pe.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: registrar usuario", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: registrar usuario\",\"details\":\"Ocurrió un error al procesar los datos de usuario en FuelBridge.\"}")))
    })
    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody SignUpResource resource) {
        var command = SignUpCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = userCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                UserResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.CREATED);
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @Operation(
        summary = "Iniciar sesión",
        description = "Verifica usuario y contraseña y devuelve un JWT para autenticar llamadas al monolito y a los microservicios con el secreto compartido.",
        security = {},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Datos necesarios para iniciar sesión.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.SignInResource.class),
                examples = { @ExampleObject(name = "principal", value = "{\"username\":\"compras.costasur\",\"password\":\"CostaSur!Combustible2026\"}") }))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación completada correctamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.halofuel.fuelbridge.platform.iam.interfaces.rest.resources.AuthenticatedUserResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"id\":1101,\"username\":\"compras.costasur\",\"token\":\"eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjb21wcmFzLmNvc3Rhc3VyIn0.ZnVlbGJyaWRnZS1leGFtcGxlLXNpZ25hdHVyZQ\"}"))),
        @ApiResponse(responseCode = "400", description = "La contraseña no coincide con la almacenada.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Credenciales inválidas", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Credenciales inválidas\",\"details\":\"El usuario operaciones@minerasur.pe o la contraseña no son correctos.\"}"))),
        @ApiResponse(responseCode = "403", description = "Origen CORS rechazado: devuelve el texto literal Invalid CORS request sin cabecera Content-Type. Un rechazo de autorización de Spring Security puede no incluir cuerpo.", content = @Content(mediaType = "*/*", schema = @Schema(type = "string"), examples = @ExampleObject(name = "corsRechazado", description = "Texto literal devuelto por el procesador CORS; la respuesta no incluye Content-Type.", value = "Invalid CORS request"))),
        @ApiResponse(responseCode = "404", description = "El nombre de usuario no está registrado.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "Usuario no encontrado", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"USER_NOT_FOUND\",\"message\":\"Usuario no encontrado\",\"details\":\"No existe usuario con identificador operaciones@minerasur.pe.\"}"))),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "error", summary = "No se pudo completar la operación: iniciar sesión", description = "Ejemplo de error con la estructura ErrorResource (code, message, details).", value = "{\"code\":\"UNEXPECTED_ERROR\",\"message\":\"No se pudo completar la operación: iniciar sesión\",\"details\":\"Ocurrió un error al procesar los datos de usuario en FuelBridge.\"}")))
    })
    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody SignInResource resource) {
        var command = SignInCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = userCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                pair -> AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntity(
                        pair.getLeft(), pair.getRight()),
                HttpStatus.OK);
    }

    @Operation(
        summary = "Cerrar sesión",
        description = "Revoca el JWT enviado en el encabezado Authorization: guarda su hash SHA-256 en la lista de tokens revocados hasta que expire. Después, cualquier petición con ese token recibe 401.",
        security = { @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token revocado.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignOutResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"message\":\"Signed out\"}"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido, expirado o ya revocado: 401 sin cuerpo.", content = @Content)
    })
    @PostMapping("/sign-out")
    public ResponseEntity<?> signOut(@RequestHeader(value = "Authorization", required = false) String authorization) {
        var command = RevokeTokenCommandFromAuthorizationHeaderAssembler.toCommandFromHeader(authorization);
        var result = revokedTokenCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                revoked -> new SignOutResource("Signed out"),
                HttpStatus.OK);
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @Operation(
        summary = "Solicitar recuperación de contraseña",
        description = "Genera un token de restablecimiento (UUID, válido 1 hora, de un solo uso) para el usuario indicado por email o username (el username es el correo de la cuenta). Solo se guarda su hash. En esta versión el token se devuelve en la respuesta en lugar de enviarse por correo. Una nueva solicitud invalida los tokens anteriores del usuario.",
        security = {},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Email o username de la cuenta.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ForgotPasswordResource.class),
                examples = @ExampleObject(name = "principal", value = "{\"email\":\"compras.costasur\"}")))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token generado.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PasswordResetTokenResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"token\":\"3f6b0c52-8d1e-4a7b-9c5e-2b7a1d0f4e66\",\"expiresAt\":\"2026-11-14T22:00:00Z\"}"))),
        @ApiResponse(responseCode = "400", description = "No se indicó email ni username.", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "El usuario no existe.", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordResource resource) {
        var command = RequestPasswordResetCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = passwordResetCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                pair -> new PasswordResetTokenResource(pair.getRight(), pair.getLeft().getExpiresAt()),
                HttpStatus.OK);
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @Operation(
        summary = "Restablecer contraseña",
        description = "Cambia la contraseña con un token de restablecimiento válido (existente, no expirado y no usado) y lo marca como usado. La nueva contraseña debe tener entre 8 caracteres y 72 bytes.",
        security = {},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, description = "Token recibido y nueva contraseña.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResetPasswordResource.class),
                examples = @ExampleObject(name = "principal", value = "{\"token\":\"3f6b0c52-8d1e-4a7b-9c5e-2b7a1d0f4e66\",\"newPassword\":\"NuevaClave!2026\"}")))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contraseña actualizada.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PasswordResetConfirmationResource.class), examples = @ExampleObject(name = "respuesta", value = "{\"message\":\"Password updated\"}"))),
        @ApiResponse(responseCode = "400", description = "Token inexistente, expirado o ya usado, o contraseña inválida.", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordResource resource) {
        var command = ResetPasswordCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = passwordResetCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                ignored -> new PasswordResetConfirmationResource("Password updated"),
                HttpStatus.OK);
    }
}
