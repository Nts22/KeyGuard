# Password Manager

Aplicación de escritorio segura para la gestión de contraseñas, desarrollada con Java 17, Spring Boot 3.2 y JavaFX 17.

## Características

- **Protección con contraseña maestra** - Autenticación segura usando PBKDF2-SHA256 con 100,000 iteraciones
- **Cifrado AES-256-GCM** - Todas las contraseñas se almacenan cifradas
- **Gestión completa de contraseñas** - Crear, editar, eliminar y visualizar entradas
- **Organización por categorías** - Categorías predefinidas y personalizadas
- **Búsqueda y filtrado** - Búsqueda en tiempo real por título o categoría
- **Generador de contraseñas** - Generación segura con opciones configurables
- **Campos personalizados** - Añade campos adicionales a cada entrada (preguntas de seguridad, códigos de recuperación, etc.)
- **Copia al portapapeles** - Copia rápida de contraseñas con un clic

## Requisitos previos

- Java 17 o superior
- Maven 3.6 o superior

## Instalación

1. Clona o descarga el proyecto:
```bash
git clone <url-del-repositorio>
cd password-manager
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

## Primer uso

1. Al iniciar por primera vez, se mostrará la pantalla de configuración
2. Crea tu contraseña maestra (mínimo 8 caracteres)
3. Confirma la contraseña maestra
4. Las categorías predeterminadas se crearán automáticamente

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

- **Hash de contraseña maestra**: PBKDF2-SHA256 con 100,000 iteraciones
- **Cifrado de datos**: AES-256-GCM con IV aleatorio por cifrado
- **Salt**: 16 bytes (128 bits)
- **IV**: 12 bytes (96 bits)
- **Tag de autenticación**: 128 bits

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
