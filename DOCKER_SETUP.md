# Gateway Studio - API Gateway Middleware

A production-ready API gateway middleware built with Spring Boot, React, and PostgreSQL. Routes user requests to multiple microservices with built-in authentication, rate limiting, and analytics.

## Architecture

```
┌─────────────────────────────────────────┐
│  UI Layer (React + Nginx)               │
│  Port: 3000                             │
└─────────────────────────────────────────┘
           ↓ (Backend API calls)
┌─────────────────────────────────────────┐
│  API Gateway (Spring Boot)              │
│  Port: 8080                             │
│  - Auth/JWT Validation                  │
│  - Rate Limiting                        │
│  - Request Routing                      │
│  - Analytics/Logging                    │
└─────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────┐
│  PostgreSQL Database                    │
│  Port: 5432                             │
└─────────────────────────────────────────┘
```

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Git

### Run Locally (Docker)

```bash
# Clone the repository
git clone <repo-url>
cd GatewayStudio

# Start all 3 containers
docker compose up --build

# First build takes ~2-3 minutes
# Subsequent builds use cache: ~30 seconds
```

Then open:
- **Frontend UI**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **Database**: localhost:5432 (admin/admin123)

### Stop Containers
```bash
docker compose down

# Remove volumes (reset database)
docker compose down -v
```

## Production Build Optimization

The project uses **multi-stage Docker builds with layer caching** to optimize build times:

### Backend (Java/Maven)
- **Stage 1**: Downloads Maven dependencies (cached separately)
- **Stage 2**: Builds application JAR (reuses cached dependencies)
- **Stage 3**: Runtime only (minimal image with JRE)

**Cache benefits**:
- Edit `pom.xml` → dependencies re-download (~1 min)
- Edit `.java` files → rebuild only source (~20 sec)
- No changes → instant (~1 sec)

### Frontend (React/Node)
- **Stage 1**: Installs npm dependencies (cached separately)
- **Stage 2**: Builds React app (reuses cached node_modules)
- **Stage 3**: Nginx serving static assets

**Cache benefits**:
- Edit `package.json` → re-install npm (~40 sec)
- Edit `.jsx` files → rebuild only app (~10 sec)
- No changes → instant (~1 sec)

### Reduce Build Context
The `.dockerignore` file excludes unnecessary files (node_modules, logs, git) from Docker context, speeding up builds.

## Configuration

### Environment Variables

Frontend (`.env` in project root):
```
VITE_BACKEND_URL=http://localhost:8080          # Dev (Vite proxy)
VITE_BACKEND_URL=http://gateway-studio-backend:8080  # Docker
```

Backend (environment in `docker-compose.yml`):
```
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/gateway_studio
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=admin123
BACKEND_URL=http://gateway-studio-backend:8080
DEPLOYMENT=docker
```

### Database Configuration

Default credentials in `docker-compose.yml`:
- **User**: admin
- **Password**: admin123
- **Database**: gateway_studio

To change credentials, update both:
1. `docker-compose.yml` (service environment)
2. `backend/src/main/resources/application.yaml` (Spring properties)

## Project Structure

```
GatewayStudio/
├── backend/                    # Spring Boot API Gateway
│   ├── src/
│   │   ├── main/java/com/gatewaystudio/
│   │   │   ├── GatewayStudioApplication.java
│   │   │   ├── config/         # Spring configuration
│   │   │   ├── controller/     # REST endpoints
│   │   │   ├── entity/         # JPA entities
│   │   │   ├── repository/     # Data access
│   │   │   └── service/        # Business logic
│   │   └── resources/
│   │       ├── application.yaml        # Spring config
│   │       ├── logback-spring.xml      # Logging config
│   │       └── db/migration/           # Flyway migrations
│   └── pom.xml                 # Maven dependencies
│
├── frontend/                   # React UI
│   ├── src/
│   │   ├── App.jsx
│   │   ├── App.css
│   │   ├── main.jsx
│   │   └── components/
│   ├── public/
│   ├── Dockerfile              # Multi-stage React build
│   ├── nginx.conf              # Nginx configuration
│   ├── vite.config.js          # Vite bundler config
│   ├── package.json
│   └── index.html
│
├── Dockerfile                  # Backend build (multi-stage)
├── docker-compose.yml          # Orchestrates all 3 services
├── .dockerignore               # Excludes files from Docker build
└── README.md
```

## Key Technologies

- **Backend**: Spring Boot 3.4.3, Java 21, Spring Cloud Gateway MVC
- **Frontend**: React 19, Vite 8, Nginx (production serving)
- **Database**: PostgreSQL 15
- **Containerization**: Docker & Docker Compose
- **Build Caching**: Multi-stage Docker builds with layer optimization

## API Endpoints

### Admin Routes Management
```
POST   /api/v1/admin/routes          # Create new route
GET    /api/v1/admin/routes          # List all routes
PUT    /api/v1/admin/routes/{id}     # Update route
DELETE /api/v1/admin/routes/{id}     # Delete route
```

### Gateway Proxy
```
GET/POST/PUT/DELETE /{service}/**    # Route requests to upstream services
```

### Health & Diagnostics
```
GET /actuator/health               # Service health status
```

## Performance Tips

1. **Fast rebuilds**: Layer caching means code-only changes rebuild in ~10-20 seconds
2. **No reinstall**: npm/Maven dependencies cached, only reinstall when package files change
3. **Production-ready**: Nginx serves optimized React bundles with gzip compression
4. **Persistent data**: PostgreSQL uses named volume (`postgres_data`), survives container restarts
5. **Health checks**: All services have health checks; containers auto-restart on failure

## Logging

Logs are written to:
- **Console**: Real-time output via `docker compose logs -f`
- **Files**: Mounted to `./logs/` directory on host (persists after container stops)

View logs:
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f gateway-studio-backend
docker compose logs -f gateway-studio-frontend
docker compose logs -f postgres
```

## Troubleshooting

### Container fails to start
```bash
# Check logs
docker compose logs -f gateway-studio-backend

# Verify database is ready
docker exec postgres pg_isready -U admin
```

### Port already in use
```bash
# Change ports in docker-compose.yml:
# - "3000:80"  → "3001:80"  (frontend)
# - "8080:8080" → "8081:8080" (backend)
# - "5432:5432" → "5433:5432" (database)
```

### Clear everything and start fresh
```bash
docker compose down -v
docker compose up --build
```

## Development Workflow

### Local Development (without Docker)
```bash
# Terminal 1: Backend
cd backend
./mvnw spring-boot:run

# Terminal 2: Frontend
cd frontend
npm install
npm run dev
```

Then open http://localhost:5173 (Vite dev server with proxy to localhost:8080)

### Docker Development
```bash
# Single command, everything runs
docker compose up --build

# For rapid iteration: edit and save, Docker rebuilds automatically
```

## Resume Value

This project demonstrates:
- ✅ **API Gateway patterns** (routing, auth, rate limiting, analytics)
- ✅ **Full-stack development** (Java backend + React frontend)
- ✅ **Docker mastery** (multi-stage builds, layer caching, Compose orchestration)
- ✅ **Production-ready code** (health checks, logging, configuration management)
- ✅ **Database design** (PostgreSQL schema, migrations, JPA)
- ✅ **DevOps skills** (container networking, environment-driven config)

## License

MIT

