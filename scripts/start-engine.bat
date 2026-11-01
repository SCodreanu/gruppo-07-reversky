@echo off
rem Avvia solo il motore di gioco in locale con Gradle (non serve Docker).
setlocal

pushd "%~dp0..\reversky-engine"
echo Avvio di reversky-engine su http://localhost:8081 ...
call gradlew.bat bootRun --args="--server.port=8081" %*
set EXIT_CODE=%ERRORLEVEL%
popd

exit /b %EXIT_CODE%
