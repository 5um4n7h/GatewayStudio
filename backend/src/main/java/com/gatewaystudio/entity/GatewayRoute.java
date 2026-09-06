package com.gatewaystudio.entity;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "gateway_routes")
public class GatewayRoute {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 6)
    private String tenantId;

    @Column(name = "route_name", nullable = false)
    private String routeName;

    @Column(name = "public_path", nullable = false)
    private String publicPath;

    @Column(name = "target_uri", nullable = false)
    private String targetUri;

    @Column(name = "strip_prefix")
    private int stripPrefix = 0;

    @Column(nullable = false)
    private boolean enabled = true;

}
