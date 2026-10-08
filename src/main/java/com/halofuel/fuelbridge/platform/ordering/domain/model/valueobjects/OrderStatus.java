package com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    DISPATCHED,
    PENDING_PAYMENT,
    PAID,
    IN_PROGRESS,
    DELIVERED,
    CANCELLED
}
