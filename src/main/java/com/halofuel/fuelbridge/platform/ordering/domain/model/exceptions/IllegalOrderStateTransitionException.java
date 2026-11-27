package com.halofuel.fuelbridge.platform.ordering.domain.model.exceptions;

/**
 * Thrown when a state transition is not allowed from the current state (for example cancelling
 * a delivered order). It is also used for fuel requests, whose states follow the same rules.
 *
 * <p>It extends {@link IllegalStateException} on purpose: callers that already translate
 * IllegalStateException into a conflict (the mark-paid and receive handlers) keep working, while
 * {@code OrderingExceptionHandler} maps it to 409 for every other path.
 */
public class IllegalOrderStateTransitionException extends IllegalStateException {

    private final String currentState;
    private final String targetState;

    public IllegalOrderStateTransitionException(Enum<?> currentState, Enum<?> targetState) {
        super("Cannot transition from %s to %s".formatted(currentState, targetState));
        this.currentState = String.valueOf(currentState);
        this.targetState = String.valueOf(targetState);
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getTargetState() {
        return targetState;
    }
}
