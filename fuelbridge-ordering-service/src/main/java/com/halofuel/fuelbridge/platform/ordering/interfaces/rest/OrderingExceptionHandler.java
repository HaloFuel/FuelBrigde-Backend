package com.halofuel.fuelbridge.platform.ordering.interfaces.rest;

import com.halofuel.fuelbridge.platform.ordering.domain.model.exceptions.IllegalOrderStateTransitionException;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the Ordering exceptions to HTTP: an invalid state transition is a conflict (409),
 * and missing or invalid input (including an unknown fuel request or product) is a
 * validation error (400). Any other exception keeps the default 500 response.
 */
@RestControllerAdvice
public class OrderingExceptionHandler {

    @ExceptionHandler(IllegalOrderStateTransitionException.class)
    public ResponseEntity<?> handleIllegalOrderStateTransition(IllegalOrderStateTransitionException ex) {
        // FUELORDER_CONFLICT is mapped to 409 by ErrorResponseAssembler.
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                ApplicationError.conflict("FuelOrder", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                ApplicationError.validationError("request", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                ApplicationError.validationError("request-body",
                        "The request body contains invalid values or data types"));
    }
}
