
package com.halofuel.fuelbridge.platform.inventory.interfaces.rest;

import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@RestControllerAdvice
@NullMarked
public class GlobalExceptionHandler {

    private static final String MESSAGES_BASENAME = "messages";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {

        var fieldErrors = ex.getBindingResult().getFieldErrors();

        var validationPrefix = resolveMessageOrDefault(
                "validation.field.prefix",
                "Field"
        );

        var errorDetails = fieldErrors.isEmpty()
                ? resolveMessageOrDefault(
                        "validation.request.failed",
                        "Request validation failed"
                )
                : fieldErrors.stream()
                        .map(error -> "%s %s: %s".formatted(
                                validationPrefix,
                                error.getField(),
                                error.getDefaultMessage()
                        ))
                        .reduce((a, b) -> a + "; " + b)
                        .orElse(resolveMessageOrDefault(
                                "validation.request.failed",
                                "Request validation failed"
                        ));

        var applicationError = ApplicationError.validationError(
                "request-body",
                errorDetails
        );

        return ErrorResponseAssembler
                .toErrorResponseFromApplicationError(applicationError);
    }

    // Maneja JSON invalido y tipos de datos incorrectos.
    // Ejemplo: {"newStock": "abc"} -> HTTP 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {

        var applicationError = ApplicationError.validationError(
                "request-body",
                "El cuerpo de la solicitud contiene valores invalidos o tipos de datos incorrectos."
        );

        return ErrorResponseAssembler
                .toErrorResponseFromApplicationError(applicationError);
    }

    // Maneja argumentos invalidos.
    // Ejemplo: stock negativo -> HTTP 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        var applicationError = ApplicationError.validationError(
                resolveMessageOrDefault(
                        "validation.request.argument",
                        "request-argument"
                ),
                ex.getMessage() != null
                        ? ex.getMessage()
                        : resolveMessageOrDefault(
                                "validation.request.failed",
                                "Request validation failed"
                        )
        );

        return ErrorResponseAssembler
                .toErrorResponseFromApplicationError(applicationError);
    }

    // Maneja errores inesperados en tiempo de ejecucion.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(
            RuntimeException ex) {

        var applicationError = ApplicationError.unexpected(
                resolveMessageOrDefault(
                        "error.unexpected.context",
                        "global-exception-handler"
                ),
                ex.getMessage() != null
                        ? ex.getMessage()
                        : "An unexpected error occurred"
        );

        return ErrorResponseAssembler
                .toErrorResponseFromApplicationError(applicationError);
    }

    // Maneja cualquier otra excepcion no controlada.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex) {

        var applicationError = ApplicationError.unexpected(
                resolveMessageOrDefault(
                        "error.unexpected.context",
                        "global-exception-handler"
                ),
                ex.getMessage() != null
                        ? ex.getMessage()
                        : "An unexpected error occurred"
        );

        return ErrorResponseAssembler
                .toErrorResponseFromApplicationError(applicationError);
    }

    private String resolveMessageOrDefault(
            String key,
            String defaultValue,
            Object... args) {

        try {
            var bundle = ResourceBundle.getBundle(
                    MESSAGES_BASENAME,
                    LocaleContextHolder.getLocale()
            );

            if (!bundle.containsKey(key)) {
                return defaultValue;
            }

            return MessageFormat.format(
                    bundle.getString(key),
                    args
            );

        } catch (MissingResourceException ex) {
            return defaultValue;
        }
    }
}
