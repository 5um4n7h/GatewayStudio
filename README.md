# GatewayStudio

> Modern API Gateway platform with dynamic tenant-aware routing, request tracing, policy enforcement, mock upstream simulation, and a self-service admin console.

## What is GatewayStudio?

GatewayStudio is a self-service API gateway built with Java 21, Spring Boot 3.4.3, and Spring Cloud Gateway Server MVC. It provides a flexible routing layer for exposing public-facing API paths and forwarding requests to internal upstream services or embedded mock backends.

The platform combines:
- dynamic route configuration
- tenant-aware route registration
- request correlation and audit logging
- rate limiting and payload-size enforcement
- upstream proxying with JDK HttpClient
- embedded WireMock mock services
- a React admin interface with Azure AD authentication



### Current Request Flow

```mermaid
flowchart LR
  C[Client Request] --> G[GatewayStudio]
  G --> F[GlobalRequestIdFilter]
  F --> R[Route Matching]
  R --> D[DynamicGatewayRouteSupplier]
  D --> RL[RateLimitFilter]
  D --> PS[PayloadSizeFilter]
  RL --> P[Proxy to Upstream]
  PS --> P
  P --> U[Upstream API / WireMock]
  F -.-> ID[X-Request-ID + MDC]
  G -.-> A[Audit Log + Local JSON Log]
  ```

## Key Capabilities Implemented

- Dynamic Routing & Path Forwarding
  - Public-facing routes are matched and forwarded to configured upstream targets.
  - Route resolution is tenant-aware and supports lazy route loading per tenant.

- Tenant-Aware Route Management
  - Routes are persisted in PostgreSQL and exposed through admin APIs.
  - Administrators can list, create, delete, enable/disable, and bulk-update routes by tenant.

- Request Correlation & Audit Logging
  - A global request filter generates a UUID-based X-Request-ID.
  - The request ID is added to MDC and response headers for end-to-end request tracing.
  - HTTP request metadata is logged to a local JSON audit log for observability and debugging.

- Gateway Policy Enforcement
  - Per-route rate limiting prevents abuse and protects upstream dependencies.
  - Payload size validation blocks oversized requests based on route configuration.
  - Disabled routes are explicitly rejected with a 503 response.

- Embedded Upstream Mocking
  - WireMock is started automatically for local testing and mock service simulation.
  - This supports integration testing, response simulation, and fault injection without external dependencies.

- Self-Service Admin Console
  - A React frontend allows route management, tenant switching, and operational visibility.
  - Azure AD authentication is integrated for secure access to admin workflows.

- Proxying & Upstream Integration
  - Request forwarding uses the Java HTTP client and Spring Cloud Gateway MVC proxying.
  - Upstream services can be internal APIs, external services, or local mock endpoints.

## 🚀 Roadmap & What's Next

GatewayStudio is evolving from a core proxy engine into a full-featured API management platform.

- OAuth2/JWT authentication and authorization
- advanced traffic shaping and resiliency policies
- multi-tenant policy and service catalogs
- persistent governance workflows
- more advanced analytics dashboards
- production-grade deployment and autoscaling
- stronger cloud-native security and observability
