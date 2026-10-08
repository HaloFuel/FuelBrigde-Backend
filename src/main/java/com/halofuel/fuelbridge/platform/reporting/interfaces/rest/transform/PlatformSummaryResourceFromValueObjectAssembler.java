package com.halofuel.fuelbridge.platform.reporting.interfaces.rest.transform;

import com.halofuel.fuelbridge.platform.reporting.domain.model.valueobjects.PlatformSummary;
import com.halofuel.fuelbridge.platform.reporting.interfaces.rest.resources.PlatformSummaryResource;

public final class PlatformSummaryResourceFromValueObjectAssembler {

    private PlatformSummaryResourceFromValueObjectAssembler() {
    }

    public static PlatformSummaryResource toResourceFromValueObject(PlatformSummary summary) {
        return new PlatformSummaryResource(summary.totalOrders(), summary.totalDeliveries(),
                summary.totalPayments(), summary.totalRevenue(),
                summary.pendingOrders(), summary.completedDeliveries());
    }
}
