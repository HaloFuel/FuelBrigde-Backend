package com.halofuel.fuelbridge.platform.steps;

import com.halofuel.fuelbridge.platform.FuelBridgeFulfillmentServiceApplication;

import io.cucumber.spring.CucumberContextConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Boots the fulfillment service for Cucumber scenarios. Ordering, Inventory and Equipment
 * are remote services, so each one is replaced by an in-memory HTTP stand-in.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = FuelBridgeFulfillmentServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CucumberSpringConfiguration.RemoteServicesConfiguration.class)
public class CucumberSpringConfiguration {

    private static final OrderingHttpStub ORDERING_SERVICE = new OrderingHttpStub();
    private static final InventoryHttpStub INVENTORY_SERVICE = new InventoryHttpStub();
    private static final EquipmentHttpStub EQUIPMENT_SERVICE = new EquipmentHttpStub();

    @DynamicPropertySource
    static void remoteServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("ordering.service.url", ORDERING_SERVICE::baseUrl);
        registry.add("inventory.service.url", INVENTORY_SERVICE::baseUrl);
        registry.add("equipment.service.url", EQUIPMENT_SERVICE::baseUrl);
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class RemoteServicesConfiguration {

        @Bean(destroyMethod = "close")
        public OrderingHttpStub orderingHttpStub() {
            return ORDERING_SERVICE;
        }

        @Bean(destroyMethod = "close")
        public InventoryHttpStub inventoryHttpStub() {
            return INVENTORY_SERVICE;
        }

        @Bean(destroyMethod = "close")
        public EquipmentHttpStub equipmentHttpStub() {
            return EQUIPMENT_SERVICE;
        }
    }
}
