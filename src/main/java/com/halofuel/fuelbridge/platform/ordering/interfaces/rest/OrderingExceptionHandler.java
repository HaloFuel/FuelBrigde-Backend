package com.halofuel.fuelbridge.platform.ordering.interfaces.rest;

import com.halofuel.fuelbridge.platform.ordering.domain.model.exceptions.IllegalOrderStateTransitionException;
import com.halofuel.fuelbridge.platform.shared.application.result.ApplicationError;
import com.halofuel.fuelbridge.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the Ordering domain exceptions to HTTP. It has precedence over the shared
 * GlobalExceptionHandler, whose catch-all RuntimeException handler would otherwise answer 500.
 * It lives in Ordering so the shared package does not depend on a bounded context.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OrderingExceptionHandler {

    @ExceptionHandler(IllegalOrderStateTransitionException.class)
    public ResponseEntity<?> handleIllegalOrderStateTransition(IllegalOrderStateTransitionException ex) {
        // FUELORDER_CONFLICT is mapped to 409 by ErrorResponseAssembler.
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                ApplicationError.conflict("FuelOrder", ex.getMessage()));
    }
}
