# Pre-Docker Deployment Checklist

## Configuration Files ✅
- [x] `backend/src/main/resources/application.yaml` - Environment-based DB config
- [x] `frontend/vite.config.js` - Environment-based backend URL
- [x] `docker-compose.yml` - 3-container orchestration (Frontend, Backend, Postgres)
- [x] `Dockerfile` - Backend multi-stage build with caching
- [x] `frontend/Dockerfile` - Frontend multi-stage build with caching
- [x] `frontend/nginx.conf` - Nginx production serving + SPA routing
- [x] `.dockerignore` - Exclude unnecessary files from Docker context
- [x] `.env.example` - Environment variable template

## Application Setup ✅
- [x] Java 21 configured in `pom.xml`
- [x] Spring Boot 3.4.3 with proper dependencies
- [x] Spring Boot Maven plugin configured with mainClass
- [x] React 19 + Vite 8 with build optimizations
- [x] PostgreSQL 15 database schema migration support

## Docker Build Optimization ✅
- [x] Backend: Multi-stage Maven cache layer (pom.xml downloaded once)
- [x] Frontend: Multi-stage npm cache layer (dependencies installed once)
- [x] Backend: Source code rebuilt only when needed (~20 sec)
- [x] Frontend: React app rebuilt only when needed (~10 sec)
- [x] `.dockerignore` reduces Docker build context size

## Container Health & Reliability ✅
- [x] Frontend: Health check (HTTP on port 80)
- [x] Backend: Health check (Spring Actuator endpoint)
- [x] Database: Health check (pg_isready command)
- [x] All services: Auto-restart on failure
- [x] Database: Named volume for data persistence

## Networking & Service Discovery ✅
- [x] All 3 services on `gateway-network` bridge
- [x] Frontend can reach backend via `http://gateway-studio-backend:8080`
- [x] Backend can reach database via `http://postgres:5432`
- [x] Environment variables for all service URLs

## Logging & Monitoring ✅
- [x] Backend logs mounted to `./logs/` volume (persistent on host)
- [x] Console logging enabled (visible via `docker compose logs -f`)
- [x] Spring Actuator `/actuator/health` endpoint for monitoring
- [x] Structured logging configuration (Logback)

## Production Readiness ✅
- [x] Nginx gzip compression enabled
- [x] Static asset caching (1-year browser cache for .js, .css, etc.)
- [x] index.html cache control (no-cache for SPA routing)
- [x] SPA routing configured (404 → index.html)
- [x] No console.log in production (Terser minification)
- [x] Lightweight JRE runtime (not JDK) in final image

## Pre-Flight Checks

Before running `docker compose up`:

### 1. Verify Docker Installation
```bash
docker --version
docker compose version
```

### 2. Verify Ports Available
- Port 3000 (Frontend)
- Port 8080 (Backend)
- Port 5432 (Database)

```bash
# Linux/Mac
lsof -i :3000
lsof -i :8080
lsof -i :5432

# Windows PowerShell
Get-NetTCPConnection -LocalPort 3000, 8080, 5432 -ErrorAction SilentlyContinue
```

### 3. Verify File Structure
```bash
ls -la Dockerfile
ls -la frontend/Dockerfile
ls -la frontend/nginx.conf
ls -la docker-compose.yml
ls -la .dockerignore
```

### 4. Verify Backend Configuration
- Check `backend/src/main/resources/application.yaml`:
  - Database URL uses environment variables
  - Logging properly configured
  - Spring profiles set up

### 5. Verify Frontend Configuration
- Check `frontend/vite.config.js`:
  - Environment variable loading for `VITE_BACKEND_URL`
  - Build optimizations in place
  - Dev proxy configured for local testing

## Build Time Expectations

### First Run (all layers from scratch)
```
Frontend: ~1 min (npm install + Vite build)
Backend: ~1.5-2 min (Maven download + compile)
Database: ~10 sec (PostgreSQL init)
Total: 2-3 minutes
```

### Subsequent Runs (using cache)
- **No changes**: ~1 second (all cached)
- **Java code only**: ~20 seconds (recompile source)
- **React code only**: ~10 seconds (rebuild app)
- **pom.xml change**: ~1 minute (re-download Maven deps)
- **package.json change**: ~40 seconds (re-install npm deps)

## Startup Checklist

When containers start up:

1. **PostgreSQL** initializes (~5 sec)
2. **Backend** starts Spring Boot (~15-20 sec)
   - Connects to database
   - Loads configuration
   - Starts Tomcat on port 8080
   - Health check passes
3. **Frontend** serves Nginx (~2 sec)
   - Waits for backend healthy
   - Starts Nginx on port 80
   - Health check passes

**Total startup time: ~30-40 seconds**

## Troubleshooting

### Container won't start
- Check logs: `docker compose logs -f gateway-studio-backend`
- Verify database is ready: `docker exec postgres pg_isready -U admin`

### Port already in use
- Change ports in docker-compose.yml
- Or kill existing process: `docker compose down`

### Database connection error
- Verify `SPRING_DATASOURCE_URL` matches service name: `postgres`
- Check credentials match compose environment

### Frontend showing 502 Bad Gateway
- Verify backend is healthy: `docker compose ps`
- Check backend logs: `docker compose logs -f gateway-studio-backend`

### Slow build times
- First build is expected to be 2-3 minutes
- Check .dockerignore is excluding unnecessary files
- Subsequent builds should use cache

## Quick Commands

```bash
# Start everything
docker compose up --build

# View logs
docker compose logs -f

# Stop everything gracefully
docker compose down

# Remove database and start fresh
docker compose down -v
docker compose up --build

# SSH into containers
docker exec -it gateway-studio-backend bash
docker exec -it postgres psql -U admin -d gateway_studio
```

---

✅ All checks passed - Ready for Docker deployment!

