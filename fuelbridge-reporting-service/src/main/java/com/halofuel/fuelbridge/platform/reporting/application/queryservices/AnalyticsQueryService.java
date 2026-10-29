package com.halofuel.fuelbridge.platform.reporting.application.queryservices;

import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetBuyerAnalyticsQuery;
import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetPlatformSummaryQuery;
import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetProviderAnalyticsQuery;
import com.halofuel.fuelbridge.platform.reporting.domain.model.valueobjects.BuyerAnalytics;
import com.halofuel.fuelbridge.platform.reporting.domain.model.valueobjects.PlatformSummary;
import com.halofuel.fuelbridge.platform.reporting.domain.model.valueobjects.ProviderAnalytics;

public interface AnalyticsQueryService {
    ProviderAnalytics handle(GetProviderAnalyticsQuery query);
    BuyerAnalytics handle(GetBuyerAnalyticsQuery query);
    PlatformSummary handle(GetPlatformSummaryQuery query);
}
