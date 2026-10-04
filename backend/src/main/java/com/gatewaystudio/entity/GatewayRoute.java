package com.gatewaystudio.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gateway_routes")
@Data
@Getter
@Setter
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

    @Column(name = "rate_limit_requests", nullable = false)
    private int rateLimitRequests = 3;

    @Column(name = "rate_limit_window_seconds", nullable = false)
    private int rateLimitWindowSeconds = 10;

    @Column(name = "max_payload_size_mb", nullable = false)
    private int maxPayloadSizeMb = 1;

}
