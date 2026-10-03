-- ============================================================================
-- GATEWAY STUDIO - Complete Database Schema
-- ============================================================================
-- This is a reference file showing the complete schema.
-- The actual migrations are split into V1 and V2 for Flyway compatibility.
-- ============================================================================

-- ============================================================================
-- TABLES
-- ============================================================================

-- Gateway Routes Table (original schema + enhancements)
-- Stores dynamic route configurations with rate limiting and payload controls
CREATE TABLE IF NOT EXISTS gateway_routes (
                                              id VARCHAR(100) PRIMARY KEY,
    tenant_id VARCHAR(20) NOT NULL DEFAULT 'TNT001',
    route_name VARCHAR(255) NOT NULL,
    public_path VARCHAR(255) NOT NULL,
    target_uri VARCHAR(512) NOT NULL,
    strip_prefix INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT true,
    rate_limit_requests INT NOT NULL DEFAULT 100,
    rate_limit_window_seconds INT NOT NULL DEFAULT 60,
    max_payload_size_mb INT NOT NULL DEFAULT 10,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Index for tenant-based queries
CREATE INDEX idx_gateway_routes_tenant
    ON gateway_routes(tenant_id, enabled);

-- Index for rate limit queries
CREATE INDEX idx_gateway_routes_rate_limit
    ON gateway_routes(rate_limit_requests, rate_limit_window_seconds);

-- ============================================================================
-- COLUMN DEFINITIONS
-- ============================================================================
-- id: Unique route identifier (Primary Key)
-- tenant_id: Multi-tenant isolation (default: TNT001)
-- route_name: Human-readable route name
-- public_path: Gateway's public path pattern (supports wildcards)
-- target_uri: Backend service URI to route to
-- strip_prefix: Number of path segments to strip (0-10)
-- enabled: Route activation flag
-- rate_limit_requests: Max requests per window (default: 100)
-- rate_limit_window_seconds: Rate limit window in seconds (default: 60)
-- max_payload_size_mb: Max request body size in MB (default: 10)
-- created_at: Timestamp when route was created
-- updated_at: Timestamp of last update

-- ============================================================================
-- EXAMPLE DATA
-- ============================================================================
-- INSERT INTO gateway_routes (
--     id, tenant_id, route_name, public_path, target_uri,
--     strip_prefix, enabled, rate_limit_requests,
--     rate_limit_window_seconds, max_payload_size_mb
-- ) VALUES (
--     'users-api', 'TNT001', 'Users Microservice', '/users/**',
--     'http://users-service:3000', 1, true, 100, 60, 10
-- );

-- ============================================================================
-- END OF SCHEMA
-- ============================================================================

