package com.gatewaystudio.repository;

import com.gatewaystudio.entity.GatewayRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaGatewayRoute extends JpaRepository<GatewayRoute, String> {
    List<GatewayRoute> findByEnabledTrue();
    List<GatewayRoute> findByTenantId(String tenantId);
}