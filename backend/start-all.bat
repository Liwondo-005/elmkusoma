@echo off
echo ==========================================
echo   ELMKUSOMA - Starting Full Stack
echo ==========================================
echo.

echo [1/3] Checking PostgreSQL...
sc query postgresql-x64-18 | find "RUNNING" >nul
if %errorlevel% neq 0 (
    echo   Starting PostgreSQL...
    net start postgresql-x64-18
) else (
    echo   PostgreSQL already running.
)

echo [2/3] Checking Redis (Memurai)...
sc query Memurai | find "RUNNING" >nul
if %errorlevel% neq 0 (
    echo   Starting Memurai...
    net start Memurai
) else (
    echo   Memurai already running.
)

echo [3/3] Checking RabbitMQ...
sc query RabbitMQ | find "RUNNING" >nul
if %errorlevel% neq 0 (
    echo   Starting RabbitMQ...
    net start RabbitMQ
) else (
    echo   RabbitMQ already running.
)

echo.
echo ==========================================
echo   Starting Backend (Spring Boot)...
echo ==========================================
start "ELMKUSOMA Backend" /min cmd /c "set JAVA_HOME=C:\Program Files\Java\jdk-17.0.13+11 && set PATH=C:\Program Files\Java\jdk-17.0.13+11\bin;%%PATH%% && cd /d %~dp0elmkusoma-core && java -Dmaven.multiModuleProjectDirectory=%~dp0elmkusoma-core -classpath .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain spring-boot:run"

echo.
echo ==========================================
echo   Starting Frontend (Next.js)...
echo ==========================================
start "ELMKUSOMA Frontend" /min cmd /c "cd /d %~dp0..\frontend && pnpm dev"

echo.
echo ==========================================
echo   All services starting...
echo   Backend:  http://localhost:8080/api
echo   Frontend: http://localhost:3000
echo   Swagger:  http://localhost:8080/api/swagger-ui.html
echo   Database: localhost:5432
echo ==========================================
echo.
echo Press any key to check status...
pause >nul

echo.
echo Checking services...
timeout /t 50 /nobreak >nul
netstat -ano | findstr ":8080 :3000 :5432 :6379 :5672"
echo.
echo Done!
