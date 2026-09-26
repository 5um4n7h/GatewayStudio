#!/bin/bash
# Quick Start Guide for Gateway Studio

echo "=========================================="
echo "Gateway Studio - Docker Quick Start"
echo "=========================================="
echo ""

# Check if Docker is running
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed or not in PATH"
    exit 1
fi

echo "✅ Docker found"
echo ""

# Check docker-compose
if ! command -v docker-compose &> /dev/null; then
    echo "⚠️  docker-compose not found, trying 'docker compose'"
    docker compose version
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

echo ""
echo "📦 Building and starting containers..."
echo "   (First build: 2-3 minutes | Subsequent: 30 seconds)"
echo ""

cd "$(dirname "$0")"

# Build and start
$COMPOSE_CMD up --build

echo ""
echo "=========================================="
echo "✅ Gateway Studio is running!"
echo "=========================================="
echo ""
echo "Open your browser:"
echo "  • Frontend UI: http://localhost:3000"
echo "  • Backend API: http://localhost:8080"
echo ""
echo "To view logs:"
echo "  $COMPOSE_CMD logs -f"
echo ""
echo "To stop:"
echo "  Press Ctrl+C (graceful shutdown)"
echo "  Or in another terminal:"
echo "  $COMPOSE_CMD down"
echo ""

