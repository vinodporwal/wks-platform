#!/bin/bash
echo "==================================================="
echo "  FAST RESTART: Rebuilding Case Engine API Only"
echo "==================================================="

# 1. Build c7-plugins library
cd "$(dirname "$0")"/../../apps/java
mvn compile package -pl libraries/c7-plugins -DskipTests

# 2. Incremental build of case-engine-rest-api only (no clean, no tests)
mvn compile package -pl services/case-engine-rest-api -am -DskipTests

# 3. Rebuild and restart ONLY the case-engine-rest-api container
cd ../..
docker-compose -f docker-compose.yaml -f docker-compose.camunda7.yaml -f docker-compose.event-hub.camunda7.yaml up -d --no-deps --build case-engine-rest-api

echo "==================================================="
echo "  FAST RESTART COMPLETE! (~15-20 seconds)"
echo "==================================================="
