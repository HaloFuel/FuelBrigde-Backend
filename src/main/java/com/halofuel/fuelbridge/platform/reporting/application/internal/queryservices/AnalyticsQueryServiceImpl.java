package com.halofuel.fuelbridge.platform.reporting.application.internal.queryservices;

import com.halofuel.fuelbridge.platform.reporting.application.queryservices.AnalyticsQueryService;
import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetBuyerAnalyticsQuery;
import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetPlatformSummaryQuery;
import com.halofuel.fuelbridge.platform.reporting.domain.model.queries.GetProviderAnalyticsQuery;
import com.halofuel.fuelbridge.platform.reporting.domain.model.valueobjects.BuyerAnalytics;
import com.halofuel.fuelbridge.platform.reporting.domain.model.valueobjects.MonthlyAmount;
import com.halofuel.fuelbridge.platform.reporting.domain.model.valueobjects.PlatformSummary;
import com.halofuel.fuelbridge.platform.reporting.domain.model.valueobjects.ProviderAnalytics;
import com.halofuel.fuelbridge.platform.fulfillment.application.queryservices.DeliveryQueryService;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.queries.GetAllDeliveriesQuery;
import com.halofuel.fuelbridge.platform.fulfillment.domain.model.valueobjects.DeliveryStatus;
import com.halofuel.fuelbridge.platform.ordering.application.queryservices.FuelOrderQueryService;
import com.halofuel.fuelbridge.platform.ordering.domain.model.queries.GetAllFuelOrdersQuery;
import com.halofuel.fuelbridge.platform.ordering.domain.model.queries.GetFuelOrdersByCompanyIdQuery;
import com.halofuel.fuelbridge.platform.ordering.domain.model.queries.GetFuelOrdersByProviderIdQuery;
import com.halofuel.fuelbridge.platform.ordering.domain.model.valueobjects.OrderStatus;
import com.halofuel.fuelbridge.platform.payment.application.queryservices.PaymentQueryService;
import com.halofuel.fuelbridge.platform.payment.domain.model.queries.GetAllPaymentsQuery;
import com.halofuel.fuelbridge.platform.payment.domain.model.queries.GetPaymentsByCompanyIdQuery;
import com.halofuel.fuelbridge.platform.payment.domain.model.valueobjects.PaymentStatus;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsQueryServiceImpl implements AnalyticsQueryService {

    private final FuelOrderQueryService fuelOrderQueryService;
    private final PaymentQueryService paymentQueryService;
    private final DeliveryQueryService deliveryQueryService;

    public AnalyticsQueryServiceImpl(FuelOrderQueryService fuelOrderQueryService,
                                     PaymentQueryService paymentQueryService,
                                     DeliveryQueryService deliveryQueryService) {
        this.fuelOrderQueryService = fuelOrderQueryService;
        this.paymentQueryService = paymentQueryService;
        this.deliveryQueryService = deliveryQueryService;
    }

    @Override
    public ProviderAnalytics handle(GetProviderAnalyticsQuery query) {
        var orders = fuelOrderQueryService.handle(new GetFuelOrdersByProviderIdQuery(query.providerId()));
        long totalOrders = orders.size();
        long confirmed = orders.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED || o.getStatus() == OrderStatus.DELIVERED).count();
        long cancelled = orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();
        var orderIds = orders.stream().map(order -> order.getId()).collect(Collectors.toSet());
        var completedPayments = paymentQueryService.handle(new GetAllPaymentsQuery()).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                .filter(payment -> orderIds.contains(payment.getOrderId()))
                .toList();
        double revenue = completedPayments.stream()
                .mapToDouble(payment -> payment.getAmount() != null ? payment.getAmount() : 0.0)
                .sum();
        return new ProviderAnalytics(query.providerId(), totalOrders, confirmed, cancelled, revenue,
                monthlyAmounts(completedPayments, payment -> payment.getPaidAt(),
                        payment -> payment.getAmount()));
    }

    @Override
    public BuyerAnalytics handle(GetBuyerAnalyticsQuery query) {
        var orders = fuelOrderQueryService.handle(new GetFuelOrdersByCompanyIdQuery(query.companyId()));
        var payments = paymentQueryService.handle(new GetPaymentsByCompanyIdQuery(query.companyId()));
        long totalOrders = orders.size();
        double totalSpent = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();
        long completedPayments = payments.stream().filter(p -> p.getStatus() == PaymentStatus.COMPLETED).count();
        long pendingPayments = payments.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).count();
        return new BuyerAnalytics(query.companyId(), totalOrders, totalSpent, completedPayments, pendingPayments,
                monthlyAmounts(payments.stream()
                                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED).toList(),
                        payment -> payment.getPaidAt(), payment -> payment.getAmount()));
    }

    @Override
    public PlatformSummary handle(GetPlatformSummaryQuery query) {
        var orders = fuelOrderQueryService.handle(new GetAllFuelOrdersQuery());
        var deliveries = deliveryQueryService.handle(new GetAllDeliveriesQuery());
        var payments = paymentQueryService.handle(new GetAllPaymentsQuery());
        long totalOrders = orders.size();
        long pendingOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long totalDeliveries = deliveries.size();
        long completedDeliveries = deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.DELIVERED).count();
        long totalPayments = payments.size();
        double totalRevenue = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
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
