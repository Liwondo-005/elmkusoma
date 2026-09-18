#!/bin/bash
echo "ELMKUSOMA Health Check"
echo "======================"

services=("core:8080" "realtime:8081" "workers:8082" "media:8083")

for service in "${services[@]}"; do
  name=$(echo $service | cut -d: -f1)
  port=$(echo $service | cut -d: -f2)
  if curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
    echo "✅ $name (port $port) — UP"
  else
    echo "❌ $name (port $port) — DOWN"
  fi
done
