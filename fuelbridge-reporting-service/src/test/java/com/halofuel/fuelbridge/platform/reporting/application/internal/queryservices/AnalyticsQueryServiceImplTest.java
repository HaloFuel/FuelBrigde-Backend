package com.halofuel.fuelbridge.platform.reporting.application.internal.queryservices;

import com.halofuel.fuelbridge.platform.reporting.application.outboundservices.AnalyticsDataSource;
import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetBuyerAnalyticsQuery;
import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetPlatformSummaryQuery;
import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetProviderAnalyticsQuery;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests of the analytics calculations using an in-memory data source instead of HTTP. */
class AnalyticsQueryServiceImplTest {

    private static final List<AnalyticsDataSource.OrderSnapshot> ORDERS = List.of(
            new AnalyticsDataSource.OrderSnapshot(1L, "CONFIRMED"),
            new AnalyticsDataSource.OrderSnapshot(2L, "DELIVERED"),
            new AnalyticsDataSource.OrderSnapshot(3L, "CANCELLED"),
            new AnalyticsDataSource.OrderSnapshot(4L, "PENDING"));

    private static final List<AnalyticsDataSource.PaymentSnapshot> PAYMENTS = List.of(
            new AnalyticsDataSource.PaymentSnapshot(1L, 1500.0, "COMPLETED", LocalDateTime.of(2026, 9, 10, 10, 0)),
            new AnalyticsDataSource.PaymentSnapshot(2L, 2500.0, "COMPLETED", LocalDateTime.of(2026, 10, 5, 9, 30)),
            new AnalyticsDataSource.PaymentSnapshot(4L, 800.0, "PENDING", null),
            new AnalyticsDataSource.PaymentSnapshot(9L, 999.0, "COMPLETED", LocalDateTime.of(2026, 10, 1, 8, 0)));

    private static final List<AnalyticsDataSource.DeliverySnapshot> DELIVERIES = List.of(
            new AnalyticsDataSource.DeliverySnapshot("DELIVERED"),
            new AnalyticsDataSource.DeliverySnapshot("DISPATCHED"));

    private static final AnalyticsDataSource DATA = new AnalyticsDataSource() {
        @Override public List<OrderSnapshot> allOrders() { return ORDERS; }
        @Override public List<OrderSnapshot> ordersByCompany(Long companyId) { return ORDERS; }
        @Override public List<OrderSnapshot> ordersByProvider(Long providerId) { return ORDERS; }
        @Override public List<PaymentSnapshot> allPayments() { return PAYMENTS; }
        @Override public List<PaymentSnapshot> paymentsByCompany(Long companyId) { return PAYMENTS.subList(0, 3); }
        @Override public List<DeliverySnapshot> allDeliveries() { return DELIVERIES; }
    };

    private final AnalyticsQueryServiceImpl service = new AnalyticsQueryServiceImpl(DATA);

    @Test
    void providerAnalyticsCountsOrdersAndOnlyRevenueFromItsCompletedPayments() {
        var analytics = service.handle(new GetProviderAnalyticsQuery(202L));

        assertEquals(4, analytics.totalOrders());
        assertEquals(2, analytics.confirmedOrders());
        assertEquals(1, analytics.cancelledOrders());
        assertEquals(4000.0, analytics.totalRevenue());
        assertEquals(2, analytics.monthlyRevenue().size());
        assertEquals("2026-09", analytics.monthlyRevenue().get(0).month());
    }

    @Test
    void buyerAnalyticsSeparatesCompletedAndPendingPayments() {
        var analytics = service.handle(new GetBuyerAnalyticsQuery(101L));

        assertEquals(4000.0, analytics.totalSpent());
        assertEquals(2, analytics.completedPayments());
        assertEquals(1, analytics.pendingPayments());
        assertEquals(2, analytics.monthlySpending().size());
    }

    @Test
    void platformSummaryAggregatesOrdersDeliveriesAndRevenue() {
        var summary = service.handle(new GetPlatformSummaryQuery());

        assertEquals(4, summary.totalOrders());
        assertEquals(1, summary.pendingOrders());
        assertEquals(2, summary.totalDeliveries());
        assertEquals(1, summary.completedDeliveries());
        assertEquals(4, summary.totalPayments());
        assertEquals(4999.0, summary.totalRevenue());
    }
}
