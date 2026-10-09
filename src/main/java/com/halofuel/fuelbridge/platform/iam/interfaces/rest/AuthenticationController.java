package com.halofuel.fuelbridge.platform.iam.interfaces.rest;

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


    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody SignUpResource resource) {
        var command = SignUpCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = userCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                UserResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.CREATED);
    }

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
