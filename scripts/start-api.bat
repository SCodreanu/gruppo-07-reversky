@echo off
rem Avvia solo l'API in locale con Gradle (utile per il debug).
rem Spring Boot avvia da solo il database con reversky-api\compose.yaml, quindi serve Docker.
rem Per le sfide serve anche l'engine: avvialo con scripts\start-engine.bat.
setlocal

docker info >nul 2>&1
if errorlevel 1 (
    echo [ERRORE] Docker non e' avviato. Apri Docker Desktop e riprova.
    exit /b 1
)

pushd "%~dp0..\reversky-api"
echo Avvio di reversky-api su http://localhost:8080 ...
call gradlew.bat bootRun %*
set EXIT_CODE=%ERRORLEVEL%
popd

exit /b %EXIT_CODE%
