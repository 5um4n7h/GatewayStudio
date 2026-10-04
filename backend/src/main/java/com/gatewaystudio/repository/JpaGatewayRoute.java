package com.gatewaystudio.repository;

import com.gatewaystudio.entity.GatewayRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface JpaGatewayRoute extends JpaRepository<GatewayRoute, String> {

    List<GatewayRoute> findByTenantId(String tenantId);

    List<GatewayRoute> findByTenantIdAndId(String tenantId, String routeId);

    @Query("SELECT DISTINCT g.tenantId FROM GatewayRoute g ORDER BY g.tenantId")
    List<String> findDistinctTenantIds();

    @Modifying
    @Transactional
    @Query("UPDATE GatewayRoute g SET g.enabled = :enabled WHERE g.tenantId = :tenantId")
    int updateEnabledByTenant(@Param("tenantId") String tenantId, @Param("enabled") boolean enabled);
}