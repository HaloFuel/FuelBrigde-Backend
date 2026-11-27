package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.FuelBridgeOrderingServiceApplication;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderDispatchedEvent;

import io.cucumber.spring.CucumberContextConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Boots the ordering service for Cucumber scenarios. Inventory, Notification and
 * Identity &amp; Access are remote services, so each one is replaced by an in-memory
 * HTTP stand-in.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = FuelBridgeOrderingServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CucumberSpringConfiguration.RemoteServicesConfiguration.class)
public class CucumberSpringConfiguration {

    private static final InventoryHttpStub INVENTORY_SERVICE = new InventoryHttpStub();
    private static final NotificationHttpStub NOTIFICATION_SERVICE = new NotificationHttpStub();
    private static final IamHttpStub IAM_SERVICE = new IamHttpStub();

    @DynamicPropertySource
    static void remoteServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("inventory.service.url", INVENTORY_SERVICE::baseUrl);
        registry.add("notification.service.url", NOTIFICATION_SERVICE::baseUrl);
        registry.add("iam.service.url", IAM_SERVICE::baseUrl);
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class RemoteServicesConfiguration {

        @Bean(destroyMethod = "close")
        public InventoryHttpStub inventoryHttpStub() {
            return INVENTORY_SERVICE;
        }

        @Bean(destroyMethod = "close")
        public NotificationHttpStub notificationHttpStub() {
            return NOTIFICATION_SERVICE;
        }

        @Bean(destroyMethod = "close")
        public IamHttpStub iamHttpStub() {
            return IAM_SERVICE;
        }

        @Bean
        public DispatchEventRecorder dispatchEventRecorder() {
            return new DispatchEventRecorder();
        }
    }

    public static class DispatchEventRecorder {

        private final List<FuelOrderDispatchedEvent> events = new CopyOnWriteArrayList<>();

        @EventListener
        public void record(FuelOrderDispatchedEvent event) {
            events.add(event);
        }

        public void clear() {
            events.clear();
        }

        public boolean wasPublished(Long orderId, Long companyId) {
            return events.stream().anyMatch(event ->
                    orderId.equals(event.orderId()) && companyId.equals(event.companyId()));
        }
    }
}
