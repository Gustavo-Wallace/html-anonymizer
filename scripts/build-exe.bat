@echo off
setlocal

cd /d "%~dp0.."

set "APP_NAME=HTML Anonymizer"
set "APP_VERSION=1.0.0"
set "MAIN_JAR=html-anonymizer-1.0.0.jar"
set "MAIN_CLASS=br.com.estagio.anonymizer.Main"
set "DIST_DIR=dist"
set "TEMP_DIR=build\jpackage-temp"

echo.
echo ========================================
echo  HTML Anonymizer - build do EXE
echo ========================================
echo.

where mvn >nul 2>nul
if errorlevel 1 (
    echo ERRO: Maven nao foi encontrado no PATH.
    echo Instale o Maven ou adicione o comando mvn ao PATH antes de continuar.
    exit /b 1
)

where jpackage >nul 2>nul
if errorlevel 1 (
    echo ERRO: jpackage nao foi encontrado no PATH.
    echo Use um JDK que inclua jpackage e adicione o diretorio bin do JDK ao PATH.
    exit /b 1
)

echo Executando: mvn clean package
call mvn clean package
if errorlevel 1 (
    echo.
    echo ERRO: Falha ao gerar o JAR. O EXE nao sera criado.
    exit /b 1
)

if not exist "target\%MAIN_JAR%" (
    echo.
    echo ERRO: JAR esperado nao encontrado: target\%MAIN_JAR%
    exit /b 1
)

if exist "%TEMP_DIR%" (
    echo Removendo pasta temporaria anterior: %TEMP_DIR%
    rmdir /s /q "%TEMP_DIR%"
)

if exist "%DIST_DIR%" (
    echo Removendo saida anterior: %DIST_DIR%
    rmdir /s /q "%DIST_DIR%"
)

mkdir "%DIST_DIR%"

echo.
echo Executando jpackage...
jpackage ^
  --type exe ^
  --name "%APP_NAME%" ^
  --input target ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --dest "%DIST_DIR%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "Internal Tool" ^
  --win-menu ^
  --win-shortcut ^
  --win-menu-group "%APP_NAME%" ^
  --win-dir-chooser ^
  --temp "%TEMP_DIR%"

if errorlevel 1 (
    echo.
    echo ERRO: Falha ao gerar o executavel com jpackage.
    exit /b 1
)

echo.
echo Instalador gerado com sucesso em:
echo %CD%\%DIST_DIR%
echo.
echo Observacao:
echo O arquivo em dist e um instalador do Windows.
echo Depois de instalar, abra o programa pelo atalho da Area de Trabalho
echo ou pelo Menu Iniciar no grupo "%APP_NAME%".
echo O instalador nao abre o programa automaticamente ao finalizar.
echo.

endlocal
