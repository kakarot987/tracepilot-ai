
package com.tracepilot.incident.repository;

import com.tracepilot.incident.domain.ServiceInfo;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
@Profile("simulator")
public class InMemoryServiceInfoRepository
        implements ServiceInfoRepository {

    private static final Map<String, ServiceInfo> SERVICES =
            Map.of(
                    "payment-service",
                    new ServiceInfo(
                            "payment-service", "Payments", "simulation",
                            "Java 21 / Spring Boot",
                            "payments-db", "payment-provider"
                    ),

                    "order-service",
                    new ServiceInfo(
                            "order-service", "Orders", "simulation",
                            "Java 21 / Spring Boot",
                            "orders-db", null
                    ),

                    "checkout-service",
                    new ServiceInfo(
                            "checkout-service", "Checkout", "simulation",
                            "Java 21 / Spring Boot",
                            "checkout-db", "pricing-service"
                    ),

                    "api-gateway",
                    new ServiceInfo(
                            "api-gateway", "Platform", "simulation",
                            "Spring Cloud Gateway",
                            null, "internal-services"
                    ),

                    "inventory-service",
                    new ServiceInfo(
                            "inventory-service", "Inventory", "simulation",
                            "Java 21 / Spring Boot",
                            "inventory-db", null
                    )
            );

    @Override
    public Optional<ServiceInfo> findByServiceName(
            String serviceName) {

        return Optional.ofNullable(SERVICES.get(serviceName));
    }
}
