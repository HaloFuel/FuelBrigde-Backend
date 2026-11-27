/*
 * FuelBridge - C4 Model (Sprint 3)
 * Estado: FuelBridge Platform (IAM y Ordering) + siete microservicios
 * (Notification, Catalog, Reporting, Equipment, Inventory, Fulfillment y Payment).
 * 7 de 9 Bounded Contexts extraídos (77,8 %).
 * Renderizar en https://structurizr.com/dsl
 */
workspace "FuelBridge" "Plataforma B2B de compraventa de combustible de HaloFuel" {

    model {
        requester = person "Client (Requester)" "Empresa solicitante que registra y sigue pedidos de combustible."
        provider = person "Provider" "Empresa proveedora que aprueba, despacha y entrega pedidos."

        emailService = softwareSystem "Email Service" "Envío de correos transaccionales (planificado)." "External"
        cloudStorage = softwareSystem "Cloud Storage" "Almacenamiento de vouchers de pago (planificado)." "External"
        pdfGenerator = softwareSystem "PDF Generator Service" "Generación de reportes en PDF (planificado)." "External"

        fuelBridge = softwareSystem "FuelBridge" "Conecta empresas solicitantes con proveedores de combustible." {
            landing = container "Landing Page" "Sitio estático de presentación del producto." "HTML, CSS, JavaScript" "Web"
            webApp = container "Web Application" "SPA para solicitantes y proveedores." "Angular" "Web"

            platform = container "FuelBridge Platform" "Monolito residual: IAM y Ordering; gateways de compatibilidad hacia los microservicios." "Spring Boot 4, Java 26"
            platformDb = container "fuelbridge_platform" "Usuarios, empresas, tokens, solicitudes y pedidos." "MySQL" "Database"

            paymentService = container "Payment Service" "Registro, completado, fallo y reembolso de pagos; solicita a Ordering marcar el pedido como pagado." "Spring Boot 4, Java 26"
            paymentDb = container "fuelbridge_payment" "Pagos." "MySQL" "Database"

            notificationService = container "Notification Service" "Notificaciones in-app por cambios de estado del pedido." "Spring Boot 4, Java 26"
            notificationDb = container "fuelbridge_notification" "Notificaciones." "MySQL" "Database"

            catalogService = container "Catalog Service" "Calificaciones de proveedores." "Spring Boot 4, Java 26"
            catalogDb = container "fuelbridge_catalog" "Calificaciones." "MySQL" "Database"

            reportingService = container "Reporting Service" "Indicadores de plataforma, proveedor y comprador (API Composition)." "Spring Boot 4, Java 26"

            equipmentService = container "Equipment Service" "Equipos del comprador, proveedor favorito y recepción de combustible." "Spring Boot 4, Java 26"
            equipmentDb = container "fuelbridge_equipment" "Equipos." "MySQL" "Database"

            inventoryService = container "Inventory Service" "Productos de combustible, reservas y liberación de stock." "Spring Boot 4, Java 26"
            inventoryDb = container "fuelbridge_inventory" "Productos y stock." "MySQL" "Database"

            fulfillmentService = container "Fulfillment Service" "Conductores, vehículos y entregas; orquesta la Saga de despacho." "Spring Boot 4, Java 26" {
                deliveriesController = component "DeliveriesController" "Programa, despacha, completa y consulta entregas." "Spring MVC REST Controller"
                driversController = component "DriversController" "CRUD de conductores con licencia única." "Spring MVC REST Controller"
                vehiclesController = component "VehiclesController" "CRUD de vehículos con placa única." "Spring MVC REST Controller"
                deliveryCommandService = component "DeliveryCommandServiceImpl" "Saga: valida, reserva stock, guarda localmente y solicita el despacho; compensa liberando el stock." "Application Service"
                deliveryQueryService = component "DeliveryQueryServiceImpl" "Consultas de entregas." "Application Service"
                fulfillmentRepositories = component "Delivery/Driver/Vehicle Repositories" "Adaptadores JPA del dominio." "Spring Data JPA"
                orderingClient = component "OrderingServiceFuelOrders" "Puerto FuelOrders: GET pedido, dispatch y receive; reenvía el JWT del usuario." "RestClient"
                inventoryClient = component "InventoryServiceStock" "Puerto InventoryStock: reserva y liberación de stock." "RestClient"
                equipmentClient = component "EquipmentServiceFuelReceipts" "Puerto EquipmentFuelReceipts: registra el combustible entregado." "RestClient"
                tokenIssuer = component "ServiceTokenIssuer" "JWT de servicio firmado con el secreto compartido (shared-kernel 1.1.0)." "fuelbridge-shared-kernel"
                securityFilter = component "BearerAuthorizationRequestFilter" "Valida localmente el JWT emitido por IAM." "Spring Security"
            }
            fulfillmentDb = container "fuelbridge_fulfillment" "Conductores, vehículos y entregas." "MySQL" "Database"
        }

        # Contexto
        requester -> fuelBridge "Registra pedidos, pagos y consulta su estado"
        provider -> fuelBridge "Gestiona pedidos, flota y entregas"
        fuelBridge -> emailService "Envía correos" "SMTP" "Planned"
        fuelBridge -> cloudStorage "Guarda vouchers" "HTTPS" "Planned"
        fuelBridge -> pdfGenerator "Genera reportes PDF" "HTTPS" "Planned"

        # Contenedores
        requester -> landing "Visita" "HTTPS"
        provider -> landing "Visita" "HTTPS"
        requester -> webApp "Usa" "HTTPS"
        provider -> webApp "Usa" "HTTPS"
        webApp -> platform "Consume la API REST" "JSON/HTTPS + JWT"

        platform -> platformDb "Lee y escribe" "JPA/JDBC"
        platform -> notificationService "Solicita notificaciones" "REST"
        platform -> catalogService "Gateway de calificaciones" "REST"
        platform -> reportingService "Gateway de indicadores" "REST"
        platform -> equipmentService "Gateway de equipos" "REST"
        platform -> inventoryService "Gateway de productos y consulta de precios (Ordering)" "REST"
        platform -> fulfillmentService "Gateway de entregas, conductores y vehículos" "REST"
        platform -> paymentService "Gateway de pagos" "REST"

        paymentService -> paymentDb "Lee y escribe" "JPA/JDBC"
        paymentService -> platform "Solicita mark-paid del pedido (Ordering)" "REST + JWT del usuario"

        notificationService -> notificationDb "Lee y escribe" "JPA/JDBC"
        notificationService -> platform "Resuelve usuarios (IAM)" "REST"
        catalogService -> catalogDb "Lee y escribe" "JPA/JDBC"
        catalogService -> platform "Comprueba empresas (IAM)" "REST"
        reportingService -> platform "Obtiene pedidos, pagos y entregas" "REST"
        equipmentService -> equipmentDb "Lee y escribe" "JPA/JDBC"
        inventoryService -> inventoryDb "Lee y escribe" "JPA/JDBC"
        fulfillmentService -> fulfillmentDb "Lee y escribe" "JPA/JDBC"
        fulfillmentService -> platform "Consulta pedidos y solicita dispatch/receive (Ordering)" "REST + JWT del usuario"
        fulfillmentService -> inventoryService "Reserva y libera stock" "REST + JWT de servicio"
        fulfillmentService -> equipmentService "Registra combustible recibido" "REST + JWT de servicio"

        # Componentes de Fulfillment Service
        platform -> deliveriesController "Reenvía /api/v1/deliveries" "REST"
        platform -> driversController "Reenvía /api/v1/drivers" "REST"
        platform -> vehiclesController "Reenvía /api/v1/vehicles" "REST"
        securityFilter -> deliveriesController "Autentica"
        deliveriesController -> deliveryCommandService "Usa"
        deliveriesController -> deliveryQueryService "Usa"
        driversController -> fulfillmentRepositories "Usa"
        vehiclesController -> fulfillmentRepositories "Usa"
        deliveryCommandService -> fulfillmentRepositories "Guarda entregas y estado de la flota"
        deliveryQueryService -> fulfillmentRepositories "Consulta"
        deliveryCommandService -> orderingClient "Consulta pedido, dispatch y receive"
        deliveryCommandService -> inventoryClient "Reserva / libera stock"
        deliveryCommandService -> equipmentClient "Registra recepción"
        inventoryClient -> tokenIssuer "Obtiene JWT de servicio"
        equipmentClient -> tokenIssuer "Obtiene JWT de servicio"
        fulfillmentRepositories -> fulfillmentDb "Lee y escribe" "JPA/JDBC"
        orderingClient -> platform "GET /fuel-orders/{id}, POST dispatch/receive" "REST"
        inventoryClient -> inventoryService "POST stock-reservations / stock-releases" "REST"
        equipmentClient -> equipmentService "POST fuel-receipts" "REST"
    }

    views {
        systemContext fuelBridge "Contexto" {
            include *
            autolayout lr
            title "FuelBridge - System Context (Sprint 3)"
        }

        container fuelBridge "Contenedores" {
            include *
            autolayout lr
            title "FuelBridge - Containers (Sprint 3)"
        }

        component fulfillmentService "FulfillmentService" {
            include *
            autolayout lr
            title "Fulfillment Service - Components (Sprint 3)"
        }

        styles {
            element "Person" {
                shape person
                background #08427b
                color #ffffff
            }
            element "Software System" {
                background #1168bd
                color #ffffff
            }
            element "External" {
                background #999999
                color #ffffff
            }
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Web" {
                shape WebBrowser
            }
            element "Database" {
                shape cylinder
                background #2e7d32
            }
            element "Component" {
                background #85bbf0
                color #000000
            }
            relationship "Planned" {
                style dashed
            }
        }
    }
}
