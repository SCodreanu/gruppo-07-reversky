@echo off
rem Avvia tutto il backend con Docker Compose: database, engine e API.
rem Uso: scripts\start-backend.bat        (in primo piano, Ctrl+C per fermare)
rem      scripts\start-backend.bat -d     (in background)
setlocal

docker info >nul 2>&1
if errorlevel 1 (
    echo [ERRORE] Docker non e' avviato. Apri Docker Desktop e riprova.
    exit /b 1
)

pushd "%~dp0.."
echo Avvio di database, engine e API...
echo API su http://localhost:8080 - database su localhost:5432
docker compose up --build %*
set EXIT_CODE=%ERRORLEVEL%
popd

exit /b %EXIT_CODE%
