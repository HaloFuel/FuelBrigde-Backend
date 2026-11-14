package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.FuelBridgePlatformApplication;
import com.halofuel.fuelbridge.platform.ordering.domain.model.events.FuelOrderDispatchedEvent;

import io.cucumber.spring.CucumberContextConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@CucumberContextConfiguration
@SpringBootTest(classes = FuelBridgePlatformApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CucumberSpringConfiguration.EventTestConfiguration.class)
public class CucumberSpringConfiguration {

    private static final NotificationHttpStub NOTIFICATION_SERVICE = new NotificationHttpStub();

    @DynamicPropertySource
    static void notificationServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("notification.service.url", NOTIFICATION_SERVICE::baseUrl);
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class EventTestConfiguration {

        @Bean(destroyMethod = "close")
        public NotificationHttpStub notificationHttpStub() {
            return NOTIFICATION_SERVICE;
        }

        @Bean
        public DispatchEventRecorder dispatchEventRecorder() {
            return new DispatchEventRecorder();
        }
    }

    public static class DispatchEventRecorder {

        private final List<FuelOrderDispatchedEvent> events =
                new CopyOnWriteArrayList<>();

        @EventListener
        public void record(FuelOrderDispatchedEvent event) {
            events.add(event);
        }

        public void clear() {
            events.clear();
        }

        public boolean wasPublished(Long orderId, Long companyId) {
            return events.stream().anyMatch(event ->
                    orderId.equals(event.orderId())
                    && companyId.equals(event.companyId())
            );
        }
    }
}
