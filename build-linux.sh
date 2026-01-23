#!/bin/bash

# ==========================================================
# CONFIGURACIÓN ROBUSTA (Spring Boot + JavaFX)
# ==========================================================
APP_NAME="PasswordManager"
APP_VERSION="1.0.0"
JAVA_HOME_CUSTOM="/opt/java/jdk-17.0.13+11"
MAIN_CLASS="org.springframework.boot.loader.launch.JarLauncher"
# Asegúrate de que esta ruta sea absoluta o correcta desde la raíz del proyecto
ICON_PATH="$(pwd)/src/main/resources/icons/icon.png"

export PATH="$JAVA_HOME_CUSTOM/bin:$PATH"
export JAVA_HOME="$JAVA_HOME_CUSTOM"

echo "========================================"
echo "  $APP_NAME - Generador de Instalador Ubuntu"
echo "========================================"

# 1. Compilación
echo "[1/4] Compilando Fat JAR con Maven..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then echo "❌ Falló la compilación"; exit 1; fi

JAR_NAME=$(ls target/*.jar | grep -v "original" | head -1 | xargs basename)

# 2. Carpeta de empaquetado limpia
echo "[2/4] Preparando entorno de empaquetado..."
rm -rf target/app-package
mkdir -p target/app-package
cp "target/$JAR_NAME" target/app-package/

# 3. Creación del Instalador .deb
echo "[3/4] Generando paquete .deb (Versión Full JRE)..."

# Ajuste: jpackage en Linux prefiere iconos .png para archivos .deb
ICON_OPTION=""
if [ -f "$ICON_PATH" ]; then
    ICON_OPTION="--icon $ICON_PATH"
else
    echo "⚠️ Advertencia: Icono no encontrado en $ICON_PATH, se usará el genérico."
fi

jpackage \
    --input target/app-package \
    --name "$APP_NAME" \
    --main-jar "$JAR_NAME" \
    --main-class "$MAIN_CLASS" \
    --type deb \
    --dest target/installer \
    --app-version "$APP_VERSION" \
    --vendor "Conastec" \
    --description "Gestor de contraseñas seguro" \
    --linux-shortcut \
    --linux-menu-group "Utility" \
    --java-options "-Dspring.beaninfo.ignore=true -Xmx512m --add-modules java.scripting" \
    $ICON_OPTION

if [ $? -eq 0 ]; then
    DEB_FILE=$(ls target/installer/*.deb)
    APP_LOWER=$(echo "$APP_NAME" | tr '[:upper:]' '[:lower:]')
    echo ""
    echo "========================================"
    echo "✅ ¡ÉXITO! Instalador creado con éxito."
    echo "========================================"
    echo "Para instalar o actualizar ejecuta:"
    echo "sudo dpkg -i $DEB_FILE"
    echo "========================================"
else
    echo "❌ ERROR: Falló la creación del instalador."
    exit 1
fi