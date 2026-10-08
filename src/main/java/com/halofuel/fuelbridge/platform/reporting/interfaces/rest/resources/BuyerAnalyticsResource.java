package com.halofuel.fuelbridge.platform.reporting.interfaces.rest.resources;

import com.halofuel.fuelbridge.platform.reporting.domain.model.valueobjects.MonthlyAmount;
import java.util.List;

public record BuyerAnalyticsResource(Long companyId, long totalOrders, double totalSpent,
                                     long completedPayments, long pendingPayments,
                                     List<MonthlyAmount> monthlySpending) {
}
