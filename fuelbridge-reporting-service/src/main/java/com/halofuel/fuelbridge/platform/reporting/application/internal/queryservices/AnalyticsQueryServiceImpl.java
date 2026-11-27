package com.halofuel.fuelbridge.platform.reporting.application.internal.queryservices;

import com.halofuel.fuelbridge.platform.reporting.application.queryservices.AnalyticsQueryService;
import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetBuyerAnalyticsQuery;
import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetPlatformSummaryQuery;
import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetProviderAnalyticsQuery;
import com.halofuel.fuelbridge.platform.reporting.domain.model.valueobjects.BuyerAnalytics;
import com.halofuel.fuelbridge.platform.reporting.domain.model.valueobjects.MonthlyAmount;
import com.halofuel.fuelbridge.platform.reporting.domain.model.valueobjects.PlatformSummary;
import com.halofuel.fuelbridge.platform.reporting.domain.model.valueobjects.ProviderAnalytics;
import com.halofuel.fuelbridge.platform.reporting.application.outboundservices.AnalyticsDataSource;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsQueryServiceImpl implements AnalyticsQueryService {

    private final AnalyticsDataSource dataSource;

    public AnalyticsQueryServiceImpl(AnalyticsDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ProviderAnalytics handle(GetProviderAnalyticsQuery query) {
        var orders = dataSource.ordersByProvider(query.providerId());
        long totalOrders = orders.size();
        long confirmed = orders.stream().filter(o -> "CONFIRMED".equals(o.status()) || "DELIVERED".equals(o.status())).count();
        long cancelled = orders.stream().filter(o -> "CANCELLED".equals(o.status())).count();
        var orderIds = orders.stream().map(order -> order.id()).collect(Collectors.toSet());
        var completedPayments = dataSource.allPayments().stream()
                .filter(payment -> "COMPLETED".equals(payment.status()))
                .filter(payment -> orderIds.contains(payment.orderId()))
                .toList();
        double revenue = completedPayments.stream()
                .mapToDouble(payment -> payment.amount() != null ? payment.amount() : 0.0)
                .sum();
        return new ProviderAnalytics(query.providerId(), totalOrders, confirmed, cancelled, revenue,
                monthlyAmounts(completedPayments, payment -> payment.paidAt(),
                        payment -> payment.amount()));
    }

    @Override
    public BuyerAnalytics handle(GetBuyerAnalyticsQuery query) {
        var orders = dataSource.ordersByCompany(query.companyId());
        var payments = dataSource.paymentsByCompany(query.companyId());
        long totalOrders = orders.size();
        double totalSpent = payments.stream()
                .filter(p -> "COMPLETED".equals(p.status()))
                .mapToDouble(p -> p.amount() != null ? p.amount() : 0.0)
                .sum();
        long completedPayments = payments.stream().filter(p -> "COMPLETED".equals(p.status())).count();
        long pendingPayments = payments.stream().filter(p -> "PENDING".equals(p.status())).count();
        return new BuyerAnalytics(query.companyId(), totalOrders, totalSpent, completedPayments, pendingPayments,
                monthlyAmounts(payments.stream()
                                .filter(payment -> "COMPLETED".equals(payment.status())).toList(),
                        payment -> payment.paidAt(), payment -> payment.amount()));
    }

    @Override
    public PlatformSummary handle(GetPlatformSummaryQuery query) {
        var orders = dataSource.allOrders();
        var deliveries = dataSource.allDeliveries();
        var payments = dataSource.allPayments();
        long totalOrders = orders.size();
        long pendingOrders = orders.stream().filter(o -> "PENDING".equals(o.status())).count();
        long totalDeliveries = deliveries.size();
        long completedDeliveries = deliveries.stream().filter(d -> "DELIVERED".equals(d.status())).count();
        long totalPayments = payments.size();
        double totalRevenue = payments.stream()
                .filter(p -> "COMPLETED".equals(p.status()))
                .mapToDouble(p -> p.amount() != null ? p.amount() : 0.0)
                .sum();
        return new PlatformSummary(totalOrders, totalDeliveries, totalPayments,
                totalRevenue, pendingOrders, completedDeliveries);
    }

    private static <T> java.util.List<MonthlyAmount> monthlyAmounts(
            java.util.List<T> rows,
            Function<T, java.time.LocalDateTime> date,
            Function<T, Double> amount) {
        Map<YearMonth, Double> grouped = new TreeMap<>();
        rows.stream().filter(row -> date.apply(row) != null).forEach(row -> {
            var month = YearMonth.from(date.apply(row));
            grouped.merge(month, amount.apply(row) != null ? amount.apply(row) : 0.0, Double::sum);
        });
        return grouped.entrySet().stream()
                .map(entry -> new MonthlyAmount(entry.getKey().toString(),
                        entry.getKey().getMonthValue(), entry.getValue()))
                .toList();
    }
}
