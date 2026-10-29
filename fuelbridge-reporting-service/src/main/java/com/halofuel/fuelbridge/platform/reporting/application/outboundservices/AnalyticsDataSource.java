package com.halofuel.fuelbridge.platform.reporting.application.outboundservices;

import java.time.LocalDateTime;
import java.util.List;

/** Local read contracts; reporting does not import other bounded contexts. */
public interface AnalyticsDataSource {
    List<OrderSnapshot> allOrders();
    List<OrderSnapshot> ordersByCompany(Long companyId);
    List<OrderSnapshot> ordersByProvider(Long providerId);
    List<PaymentSnapshot> allPayments();
    List<PaymentSnapshot> paymentsByCompany(Long companyId);
    List<DeliverySnapshot> allDeliveries();

    record OrderSnapshot(Long id, String status) {}
    record PaymentSnapshot(Long orderId, Double amount, String status, LocalDateTime paidAt) {}
    record DeliverySnapshot(String status) {}
}
