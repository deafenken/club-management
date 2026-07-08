#!/bin/bash
set -euo pipefail

# =====================================================
# Club Management System - ECS Deployment Script
# Usage: ./deploy.sh <ECS_IP> [ssh_user]
# Example: ./deploy.sh 47.96.123.45 root
# =====================================================

ECS_IP="${1:?Usage: ./deploy.sh <ECS_IP> [ssh_user]}"
SSH_USER="${2:-root}"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== Deploying to ECS at ${SSH_USER}@${ECS_IP} ==="

# Step 1: Create project directory on ECS
echo "--- Step 1: Creating project directory on ECS ---"
ssh "${SSH_USER}@${ECS_IP}" "mkdir -p /opt/club-management"

# Step 2: Upload project files (exclude node_modules, target, .git, dist)
echo "--- Step 2: Uploading project files via rsync ---"
rsync -avz --progress \
  --exclude 'frontend/node_modules' \
  --exclude 'frontend/dist' \
  --exclude 'backend/target' \
  --exclude '.git' \
  --exclude '.idea' \
  --exclude '*.bat' \
  --exclude 'start.ps1' \
  --exclude 'start-all.bat' \
  --exclude 'frontend-restart.bat' \
  "${PROJECT_DIR}/" \
  "${SSH_USER}@${ECS_IP}:/opt/club-management/"

# Step 3: Ensure .env exists on ECS
echo "--- Step 3: Checking .env file on ECS ---"
ssh "${SSH_USER}@${ECS_IP}" '
  cd /opt/club-management
  if [ ! -f .env ]; then
    echo ""
    echo "  ==============================================="
    echo "  ERROR: .env file not found!"
    echo "  Create /opt/club-management/.env first."
    echo "  Use .env.example as a template:"
    echo "    cp .env.example .env"
    echo "    vim .env   # fill in real values"
    echo "  ==============================================="
    echo ""
    exit 1
  fi
'

# Step 4: Pull base images and build
echo "--- Step 4: Pulling base images and building ---"
ssh "${SSH_USER}@${ECS_IP}" '
  cd /opt/club-management
  docker pull mysql:8.0
  docker pull nginx:alpine
  docker compose up -d --build
'

# Step 5: Show container status
echo ""
echo "=== Deployment complete. Checking container status... ==="
sleep 5
ssh "${SSH_USER}@${ECS_IP}" 'cd /opt/club-management && docker compose ps'

echo ""
echo "=== Health Check URLs ==="
echo "  Frontend : http://${ECS_IP}/"
echo "  Login API: http://${ECS_IP}/api/user/login"
echo ""
echo "  Default accounts (password: 123456):"
echo "    admin (ADMIN) / president01 (PRESIDENT)"
echo "    student01 (STUDENT) / teacher01 (TEACHER)"
