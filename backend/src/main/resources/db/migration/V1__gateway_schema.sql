CREATE TABLE gateway_routes (
                                id VARCHAR(100) PRIMARY KEY,
                                tenant_id VARCHAR(20) NOT NULL DEFAULT 'TNT001',
                                route_name VARCHAR(255) NOT NULL,
                                public_path VARCHAR(255) NOT NULL,
                                target_uri VARCHAR(512) NOT NULL,
                                strip_prefix INT NOT NULL DEFAULT 0,
                                enabled BOOLEAN NOT NULL DEFAULT true,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gateway_routes_tenant ON gateway_routes(tenant_id, enabled);