package com.halofuel.fuelbridge.platform.iam.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.halofuel.fuelbridge.platform.iam.application.commandservices.UserCommandService;
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

    public AuthenticationController(UserCommandService userCommandService) {
        this.userCommandService = userCommandService;
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
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: revisar identificadores, campos o valores indicados en el cuerpo o los parámetros.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "Uno o más roles solicitados no están registrados.", content = @Content),
        @ApiResponse(responseCode = "409", description = "La operación entra en conflicto con un registro existente o con la disponibilidad del recurso.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
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
        @ApiResponse(responseCode = "400", description = "La contraseña no coincide con la almacenada.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solicitud rechazada por la política de acceso o por un origen CORS no permitido.", content = @Content),
        @ApiResponse(responseCode = "404", description = "El nombre de usuario no está registrado.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error al procesar la solicitud o acceder a la persistencia; puede incluir una transición de estado no permitida.", content = @Content)
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
}
