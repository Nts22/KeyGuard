@echo off
echo ========================================
echo   KeyGuard - Windows Installer
echo ========================================
echo.

REM Verificar que Java 17+ esta instalado
java -version 2>nul
if errorlevel 1 (
    echo ERROR: Java no esta instalado o no esta en el PATH
    echo Por favor instala JDK 17 o superior
    pause
    exit /b 1
)

REM Limpiar y compilar
echo [1/4] Limpiando proyecto...
call mvn clean

echo [2/4] Compilando aplicacion...
call mvn package -DskipTests

if errorlevel 1 (
    echo ERROR: Fallo la compilacion
    pause
    exit /b 1
)

REM Crear runtime image con jlink
echo [3/4] Creando runtime image...
if exist target\jre rmdir /s /q target\jre

jlink --add-modules java.base,java.sql,java.desktop,java.naming,java.management,java.logging,java.xml,jdk.unsupported ^
      --output target\jre ^
      --strip-debug ^
      --no-man-pages ^
      --no-header-files ^
      --compress=2

REM Crear instalador
echo [4/4] Creando instalador...
jpackage ^
    --input target ^
    --name KeyGuard ^
    --main-jar password-manager-1.0.0.jar ^
    --main-class com.passmanager.KeyGuardApplication ^
    --type exe ^
    --dest target\installer ^
    --app-version 1.0.0 ^
    --vendor "KeyGuard Inc" ^
    --copyright "Copyright 2024" ^
    --description "Gestor de contrasenas seguro" ^
    --win-dir-chooser ^
    --win-shortcut ^
    --win-menu ^
    --win-menu-group "KeyGuard" ^
    --runtime-image target\jre ^
    --java-options "-Xmx256m"

if exist "src\main\resources\icons\icon.ico" (
    echo Agregando icono personalizado...
    jpackage ^
        --input target ^
        --name KeyGuard ^
        --main-jar password-manager-1.0.0.jar ^
        --main-class com.passmanager.KeyGuardApplication ^
        --type exe ^
        --dest target\installer ^
        --app-version 1.0.0 ^
        --vendor "KeyGuard Inc" ^
        --icon src\main\resources\icons\icon.ico ^
        --win-dir-chooser ^
        --win-shortcut ^
        --win-menu ^
        --runtime-image target\jre
)

echo.
echo ========================================
echo   Instalador creado exitosamente!
echo   Ubicacion: target\installer\
echo ========================================
pause
