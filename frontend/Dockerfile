# Stage 1: Node dependency cache (changes rarely)
FROM node:20-alpine AS npm-cache

WORKDIR /app

# Copy package files ONLY (layer caches when dependencies haven't changed)
COPY frontend/package.json frontend/package-lock.json ./

# Install dependencies (cached layer)
# Use npm install instead of npm ci for better compatibility
RUN npm install --legacy-peer-deps

# Stage 2: Build React app (reuses cached node_modules)
FROM node:20-alpine AS frontend-build

WORKDIR /app

# Copy cached node_modules from previous stage (instant if unchanged)
COPY --from=npm-cache /app/node_modules ./node_modules
COPY --from=npm-cache /app/package.json ./
COPY --from=npm-cache /app/package-lock.json ./

# Copy source code (rebuilt only when code changes)
COPY frontend/src ./src
COPY frontend/public ./public
COPY frontend/vite.config.js ./
COPY frontend/index.html ./

# Build optimized production bundle with better error visibility
RUN npm run build || (echo "Build failed!" && ls -la && exit 1)

# Stage 3: Serve with nginx (lightweight final image)
FROM nginx:alpine

# Copy nginx config for production
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

# Copy built React app
COPY --from=frontend-build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

