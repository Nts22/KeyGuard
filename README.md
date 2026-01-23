# KeyGuard

Aplicación de escritorio segura para la gestión de contraseñas, desarrollada con Java 17, Spring Boot 3.2 y JavaFX 17.

## Características

### Seguridad

- **Protección con contraseña maestra** - Autenticación segura usando PBKDF2-SHA256 con 100,000 iteraciones
- **Cifrado AES-256-GCM** - Todas las contraseñas se almacenan cifradas con cifrado de grado militar
- **Recovery Key (Clave de Recuperación)** - Sistema de recuperación de cuenta si olvidas tu contraseña maestra
- **Rate Limiting** - Protección contra ataques de fuerza bruta (5 intentos fallidos → bloqueo de 15 minutos)
- **Auto-limpieza del portapapeles** - Las contraseñas copiadas se eliminan automáticamente después de 30 segundos
- **Permisos restrictivos en base de datos** - Archivos de BD con permisos 700/600 en sistemas Unix/Linux

### Gestión de Contraseñas

- **Gestión completa de contraseñas** - Crear, editar, eliminar y visualizar entradas
- **Organización por categorías** - Categorías predefinidas y personalizadas
- **Búsqueda y filtrado** - Búsqueda en tiempo real por título o categoría
- **Paginación** - Vista de tabla optimizada con 20 contraseñas por página
- **Generador de contraseñas** - Generación segura con opciones configurables
- **Campos personalizados** - Añade campos adicionales a cada entrada (preguntas de seguridad, códigos de recuperación, etc.)
- **Copia al portapapeles** - Copia rápida de contraseñas con un clic y auto-limpieza

## Requisitos previos

- Java 17 o superior
- Maven 3.6 o superior

## Instalación

1. Clona el proyecto desde GitHub:
```bash
git clone https://github.com/Nts22/KeyGuard.git
cd KeyGuard
```

2. Compila el proyecto:
```bash
mvn clean install
```

3. Ejecuta la aplicación:
```bash
mvn javafx:run
```

Alternativamente, puedes ejecutar el JAR generado:
```bash
mvn clean package
java -jar target/password-manager-1.0.0-SNAPSHOT.jar
```

### Build para Windows

Usa el script `build-windows.bat` para generar un ejecutable nativo:
```bash
build-windows.bat
```

### Build para Linux

Usa el script `build-linux.sh` para generar un ejecutable nativo:
```bash
chmod +x build-linux.sh
./build-linux.sh
```

## Primer uso

1. Al iniciar por primera vez, se mostrará la pantalla de registro
2. Crea tu contraseña maestra (mínimo 8 caracteres)
3. Confirma la contraseña maestra
4. **⚠️ IMPORTANTE**: Se generará una **Recovery Key** (clave de recuperación)
   - Esta clave tiene el formato: `XXXX-XXXX-XXXX-XXXX-XXXX-XXXX`
   - **Guárdala en un lugar seguro** (papel, USB, gestor de archivos)
   - Es tu ÚNICA forma de recuperar acceso si olvidas tu contraseña maestra
   - Solo se mostrará UNA VEZ
5. Las categorías predeterminadas se crearán automáticamente

## Recuperación de cuenta

Si olvidas tu contraseña maestra:

1. En la pantalla de login, haz clic en **"Olvidé mi contraseña"**
2. Selecciona tu usuario
3. Ingresa tu **Recovery Key**
4. Crea una nueva contraseña maestra
5. ✅ Recuperarás acceso a todas tus contraseñas guardadas

**Nota**: KeyGuard usa cifrado de conocimiento cero (zero-knowledge). Si pierdes TANTO tu contraseña maestra como tu Recovery Key, no hay forma de recuperar tus datos. Esta es una característica de seguridad, no un error.

## Estructura del proyecto

```
password-manager/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/passmanager/
│   │   │   ├── PasswordManagerApplication.java   # Punto de entrada
│   │   │   ├── SpringBootApp.java                # Configuración Spring Boot
│   │   │   ├── config/                           # Configuraciones
│   │   │   ├── controller/                       # Controladores JavaFX
│   │   │   ├── service/                          # Lógica de negocio
│   │   │   ├── model/
│   │   │   │   ├── entity/                       # Entidades JPA
│   │   │   │   └── dto/                          # Objetos de transferencia
│   │   │   ├── repository/                       # Repositorios Spring Data
│   │   │   ├── util/                             # Utilidades
│   │   │   └── exception/                        # Excepciones personalizadas
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── fxml/                             # Vistas JavaFX
│   │       └── css/                              # Estilos
│   └── test/                                     # Tests unitarios
└── target/
```

## Tecnologías utilizadas

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Java | 17 | Lenguaje principal |
| Spring Boot | 3.2.0 | Framework backend |
| JavaFX | 17.0.2 | Interfaz gráfica |
| SQLite | 3.42.0.0 | Base de datos local |
| Hibernate | 6.x | ORM |
| Lombok | - | Reducción de boilerplate |

## Seguridad

### Cifrado y Autenticación

- **Hash de contraseña maestra**: PBKDF2-SHA256 con 100,000 iteraciones
- **Cifrado de datos**: AES-256-GCM con IV aleatorio por cifrado
- **Salt**: 16 bytes (128 bits) generado aleatoriamente por usuario
- **IV**: 12 bytes (96 bits) generado aleatoriamente por entrada
- **Tag de autenticación GCM**: 128 bits

### Recovery Key (Clave de Recuperación)

- **Formato**: 24 caracteres alfanuméricos en 6 grupos (XXXX-XXXX-XXXX-XXXX-XXXX-XXXX)
- **Caracteres usados**: A-Z, 2-9 (excluyendo I, O, 0, 1 para evitar confusión)
- **Generación**: Usando `SecureRandom` con entropía criptográfica
- **Almacenamiento**:
  - Hash de la recovery key (PBKDF2-SHA256, 100,000 iteraciones)
  - Contraseña maestra cifrada con la recovery key (AES-256-GCM)
- **Uso**: Permite recuperar acceso si olvidas tu contraseña maestra

### Protección contra Ataques

- **Rate Limiting**:
  - Máximo 5 intentos de login fallidos
  - Bloqueo automático de 15 minutos
  - Contador de intentos restantes
- **Auto-limpieza del portapapeles**: Contraseñas copiadas se eliminan después de 30 segundos
- **Permisos de archivos**: Base de datos con permisos 600 (solo lectura/escritura del propietario) en Unix/Linux

## Almacenamiento

La base de datos SQLite se almacena automáticamente en:
```
~/.passmanager/passwords.db
```

## Categorías predeterminadas

- Redes Sociales
- Bancos
- Email
- Streaming
- Trabajo
- Otros

## Capturas de pantalla

*Pendiente de añadir*

## Licencia

Este proyecto está bajo la licencia MIT.
