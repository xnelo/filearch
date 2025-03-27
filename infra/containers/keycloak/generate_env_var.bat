@ECHO OFF

SETLOCAL

CALL :setEnvVar KEYCLOAK_ADMIN admin

CALL :getUUID KEYCLOAK_ADMIN_PASSWORD
CALL :setEnvVar KEYCLOAK_ADMIN_PASSWORD %KEYCLOAK_ADMIN_PASSWORD%

CALL :getUUID FILEARCH_RESTAPI_SECRET
CALL :setEnvVar FILEARCH_RESTAPI_SECRET %FILEARCH_RESTAPI_SECRET%

ECHO "Please restart your command prompt for changes to take effect."
EXIT /B 0

REM Functions

:setEnvVar
setx %~1 %~2
echo Setting environment variable %~1 = %~2
EXIT /B 0

:getUUID
for /f "delims=" %%i in ('java uuid') do (
    set "%~1=%%i"
)
EXIT /B 0