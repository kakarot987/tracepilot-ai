
package com.tracepilot.incident.repository;

import com.tracepilot.incident.domain.ServiceInfo;

import java.util.Optional;

public interface ServiceInfoRepository {
    Optional<ServiceInfo> findByServiceName(
            String serviceName
    );
}
