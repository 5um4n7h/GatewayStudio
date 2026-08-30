## 1. Description

GatewayStudio is a self-service API gateway platform being developed using **Java 25, Spring Boot, and Spring Cloud Gateway Server Web MVC**, designed to provide a centralized and controlled entry point for backend APIs. The project focuses on simplifying API routing, request handling, service integration, and gateway-level policies while providing a foundation for operational visibility and future self-service API management.

## 2. Current State

The project currently has a working **Spring Cloud Gateway Server Web MVC** implementation running on the Servlet stack with **Java Virtual Threads**.  
A public `/product/{id}` endpoint is routed to an internal `/catalog/product/{id}` API through path rewriting.  
**WireMock** is embedded in the project to simulate upstream services and test gateway behavior locally.  
A global request-ID filter generates and propagates `X-Request-ID` and stores the identifier in MDC for request correlation and logging.  
The gateway proxy uses Spring `RestClient` with the **JDK HttpClient**, including configurable connection and upstream response timeouts.  
**Spring Boot Actuator** is also integrated as the foundation for health and operational monitoring.

## 3. Future Scope

The gateway will evolve toward configurable **service and route management** through a self-service interface.  
Authentication and authorization mechanisms will be introduced for API consumers.  
Gateway-level traffic policies such as rate limiting and resilience controls will be added.  
Persistent configuration and controlled route publishing will be introduced as the platform matures.  
Operational visibility will be expanded with gateway metrics, request analytics, and usage information.  
The platform will eventually support production-oriented deployment and scaling capabilities.