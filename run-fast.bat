@echo off
echo ===================================================
echo   FAST RESTART: Rebuilding Case Engine API Only
echo ===================================================
cd /d "C:\Users\om\Desktop\NEW WKS AOP K\wks-platform\apps\java"

echo [1/3] Building c7-plugins library...
call mvn compile package -pl libraries/c7-plugins -DskipTests

echo [2/3] Incremental Java Build (Case Engine API)...
call mvn compile package -pl services/case-engine-rest-api -am -DskipTests

echo [3/3] Restarting Case Engine REST API Container Only...
cd /d "C:\Users\om\Desktop\NEW WKS AOP K\wks-platform"
docker-compose -f docker-compose.yaml -f docker-compose.camunda7.yaml -f docker-compose.event-hub.camunda7.yaml up -d --no-deps --build case-engine-rest-api

echo ===================================================
echo   FAST RESTART COMPLETE! (Time taken ~15-20 sec)
echo ===================================================
pause
