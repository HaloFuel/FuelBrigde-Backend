package com.halofuel.fuelbridge.platform.reporting.interfaces.rest.resources;

import com.halofuel.fuelbridge.platform.reporting.domain.model.valueobjects.MonthlyAmount;
import java.util.List;

public record ProviderAnalyticsResource(Long providerId, long totalOrders, long confirmedOrders,
                                        long cancelledOrders, double totalRevenue,
                                        List<MonthlyAmount> monthlyRevenue) {
}
