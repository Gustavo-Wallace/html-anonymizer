@echo off
setlocal

cd /d "%~dp0.."

echo.
echo ========================================
echo  HTML Anonymizer - build do JAR
echo ========================================
echo.

where mvn >nul 2>nul
if errorlevel 1 (
    echo ERRO: Maven nao foi encontrado no PATH.
    echo Instale o Maven ou adicione o comando mvn ao PATH antes de continuar.
    exit /b 1
)

echo Executando: mvn clean package
call mvn clean package
if errorlevel 1 (
    echo.
    echo ERRO: Falha ao gerar o JAR.
    exit /b 1
)

echo.
echo JAR gerado com sucesso:
echo target\html-anonymizer-1.0.0.jar
echo.

endlocal
