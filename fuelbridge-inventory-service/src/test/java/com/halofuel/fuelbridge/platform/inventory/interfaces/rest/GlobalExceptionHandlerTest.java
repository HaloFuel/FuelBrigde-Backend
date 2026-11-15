package com.halofuel.fuelbridge.platform.inventory.interfaces.rest;

import com.halofuel.fuelbridge.platform.shared.interfaces.rest.resources.ErrorResource;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void returns409WhenSpringDetectsConcurrentModification() {
        var exception = new OptimisticLockingFailureException(
                "Fuel product version is outdated"
        );

        var response = handler.handleOptimisticLocking(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        var body = assertInstanceOf(
                ErrorResource.class, response.getBody()
        );

        assertEquals("FUELPRODUCT_CONFLICT", body.code());
        assertNotNull(body.message());
        assertTrue(body.details().contains("Please retry"));
    }

    @Test
    void returns409WhenJpaDetectsOptimisticLockConflict() {
        var exception = new OptimisticLockException(
                "Concurrent stock update"
        );

        var response = handler.handleOptimisticLocking(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        var body = assertInstanceOf(
                ErrorResource.class, response.getBody()
        );

        assertEquals("FUELPRODUCT_CONFLICT", body.code());
    }
}
