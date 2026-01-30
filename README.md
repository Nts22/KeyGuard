# KeyGuard

Aplicación de escritorio segura para la gestión de contraseñas, desarrollada con Java 17, Spring Boot 3.2 y JavaFX 17.

## Características

### Seguridad

- **Protección con contraseña maestra** - Autenticación segura usando PBKDF2-SHA256 con 100,000 iteraciones
- **Cifrado AES-256-GCM** - Todas las contraseñas se almacenan cifradas con cifrado de grado militar
- **Recovery Key (Clave de Recuperación)** - Sistema de recuperación de cuenta si olvidas tu contraseña maestra
- **Verificación de contraseñas filtradas** - Integración con Have I Been Pwned para detectar contraseñas comprometidas
- **Auto-logout por inactividad** - Cierre automático de sesión después de 3 minutos de inactividad
- **Rate Limiting** - Protección contra ataques de fuerza bruta (5 intentos fallidos → bloqueo de 15 minutos)
- **Auto-limpieza del portapapeles** - Las contraseñas copiadas se eliminan automáticamente después de 30 segundos
- **Permisos restrictivos en base de datos** - Archivos de BD con permisos 700/600 en sistemas Unix/Linux

### Gestión de Contraseñas

- **Gestión completa de contraseñas** - Crear, editar, eliminar y visualizar entradas
- **Organización por categorías** - Categorías predefinidas y personalizadas
- **Búsqueda y filtrado** - Búsqueda en tiempo real por título o categoría
- **Paginación** - Vista de tabla optimizada con 20 contraseñas por página
- **Generador de contraseñas** - Generación segura con opciones configurables
- **Historial de contraseñas** - Guarda automáticamente las últimas 10 versiones de cada contraseña
- **Validación de seguridad** - Verificación automática contra base de datos de 12+ mil millones de contraseñas filtradas
- **Auditoría masiva** - Verificación manual de todas las contraseñas guardadas
- **Export/Import cifrado** - Backup y restauración con cifrado AES-256-GCM
- **Campos personalizados** - Añade campos adicionales a cada entrada (preguntas de seguridad, códigos de recuperación, etc.)
- **Copia al portapapeles** - Copia rápida de contraseñas con un clic y auto-limpieza
- **Visualización de contraseñas** - Toggle para mostrar/ocultar contraseñas en formularios

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

## Verificación de contraseñas filtradas

KeyGuard incluye integración con **Have I Been Pwned (HIBP)** para detectar contraseñas que han sido comprometidas en brechas de seguridad conocidas.

### ¿Por qué es importante?

Miles de millones de contraseñas han sido filtradas en brechas de seguridad de grandes empresas:
- LinkedIn: 165 millones de contraseñas
- Yahoo: 3 mil millones de cuentas
- Facebook: 533 millones de registros
- Y muchas más...

Los atacantes usan estas contraseñas en ataques de "credential stuffing" para intentar acceder a otras cuentas. Si tu contraseña fue filtrada en LinkedIn, un atacante podría intentar usarla en tu banco, email, redes sociales, etc.

### ¿Cómo funciona?

KeyGuard utiliza la API gratuita de Have I Been Pwned, que contiene más de **12 mil millones de contraseñas comprometidas** de brechas reales.

**Importante**: Tu contraseña NUNCA se envía al servidor. KeyGuard usa el método **k-anonymity**:

1. ✅ Convierte tu contraseña a hash SHA-1 localmente
2. ✅ Solo envía los primeros 5 caracteres del hash al servidor
3. ✅ Recibe ~500-800 hashes que empiezan con esos 5 caracteres
4. ✅ Compara localmente para detectar coincidencias

**Ejemplo**:
```
Contraseña: "micontrasena123"
SHA-1: "482C811DA5D5B4BC6D497FFA98491E38"
Se envía: "482C8" (solo 5 caracteres)
HIBP devuelve: ~500 hashes que empiezan con "482C8"
KeyGuard verifica localmente: ¿está "11DA5D5B4BC6D497FFA98491E38" en la lista?
```

Esto garantiza que **nadie** (ni siquiera HIBP) puede saber qué contraseña específica estás verificando.

### Dos formas de verificar

#### 1. Verificación Automática (al crear/editar)

Cada vez que creas o editas una contraseña, KeyGuard la verifica automáticamente contra la base de datos de HIBP.

- ✅ Si la contraseña es segura → se guarda normalmente
- ⚠️ Si la contraseña fue comprometida → se muestra una advertencia con detalles:
  - Número de veces que apareció en brechas
  - Nivel de riesgo (Bajo, Medio, Alto, Crítico)
  - Opción de continuar o cancelar

**Nota**: Si la API no está disponible, se muestra una advertencia pero se permite continuar (no bloqueamos al usuario).

#### 2. Verificación Manual (auditoría completa)

Puedes verificar todas tus contraseñas guardadas a la vez:

1. Haz clic en **"🔍 Verificar Contraseñas"** en el menú lateral
2. KeyGuard verificará automáticamente todas tus contraseñas
3. Verás un reporte completo con:
   - Total de contraseñas verificadas
   - Cuántas son seguras ✅
   - Cuántas están comprometidas ⚠️
   - Tabla detallada con nivel de riesgo y recomendaciones

**Cuándo usar la verificación manual**:
- Después de noticias de grandes brechas de seguridad
- Periódicamente (cada 3-6 meses como auditoría)
- Primera vez que usas KeyGuard (auditar contraseñas existentes)
- Cuando sospechas que alguna cuenta fue comprometida

### Niveles de severidad

| Nivel | Criterio | Recomendación |
|-------|----------|---------------|
| ✅ Segura | No encontrada en brechas | Mantener contraseña |
| ℹ️ Riesgo Bajo | < 10 apariciones | Considerar cambio |
| ⚠️ Riesgo Medio | 10-99 apariciones | Cambiar pronto |
| ⚠️ Riesgo Alto | 100-999 apariciones | Cambiar urgentemente |
| ⛔ Riesgo Crítico | 1000+ apariciones | CAMBIAR INMEDIATAMENTE |

### Privacidad y seguridad

- ✅ **Tu contraseña nunca sale de tu computadora completa**
- ✅ **Solo se envían 5 caracteres del hash SHA-1**
- ✅ **Imposible que HIBP sepa qué contraseña verificas**
- ✅ **API gratuita, sin límites de uso razonable**
- ✅ **No requiere API key ni autenticación**

### Referencias

- **API de HIBP**: https://haveibeenpwned.com/API/v3#PwnedPasswords
- **Método k-anonymity**: https://www.troyhunt.com/ive-just-launched-pwned-passwords-version-2/
- **Creador**: Troy Hunt (Microsoft Regional Director, MVP)

## Exportar e importar contraseñas

KeyGuard incluye funcionalidad completa de backup y restauración con cifrado de grado militar.

### ¿Por qué necesitas backups?

- **Protección contra pérdida de datos**: Disco duro dañado, borrado accidental, ransomware
- **Migración entre dispositivos**: Cambio de computadora, múltiples dispositivos
- **Compartir contraseñas de forma segura**: Contraseñas familiares, equipo de trabajo
- **Cumplimiento de políticas**: Backups periódicos requeridos por seguridad corporativa

### Seguridad del backup

**Enfoque híbrido**:
- **Campos visibles**: título, usuario, email, URL, notas, categoría (texto claro)
- **Contraseñas cifradas**: Solo las contraseñas están cifradas con AES-256-GCM
- **Ventajas**: Puedes auditar el backup sin descifrar, pero las contraseñas están protegidas

**Cifrado AES-256-GCM**:
- El mismo cifrado de grado militar que usa la base de datos interna
- Imposible leer las contraseñas sin la contraseña de backup
- Protegido contra manipulación (GCM detecta alteraciones)

**Contraseña de backup**:
- Puede ser la misma que tu contraseña maestra
- O puede ser diferente (mayor seguridad)
- Derivada con PBKDF2-SHA256 (100,000 iteraciones)
- Nunca se almacena, solo se usa para cifrar/descifrar

**Salt e IV únicos POR ENTRADA**:
- Cada contraseña tiene su propio salt e IV aleatorios
- Máxima seguridad: incluso contraseñas duplicadas se cifran diferente
- Protege contra ataques de diccionario precomputados

### Exportar contraseñas

1. Haz clic en **"💾 Exportar Contraseñas"** en el menú lateral
2. Ingresa una contraseña de backup (mínimo 8 caracteres)
3. Confirma la contraseña
4. Selecciona dónde guardar el archivo (sugiere nombre con fecha)
5. ✅ Se crea un archivo JSON cifrado con todas tus contraseñas

**Formato del archivo** (Híbrido: campos visibles + contraseñas cifradas):
```json
{
  "version": "1.0",
  "exportDate": "2024-01-26T10:30:00",
  "entryCount": 2,
  "appVersion": "1.0.0",
  "entries": [
    {
      "title": "Facebook",
      "username": "pablo@email.com",
      "email": "pablo@email.com",
      "url": "https://facebook.com",
      "notes": "Mi cuenta personal",
      "categoryName": "Redes Sociales",
      "customFields": [],
      "encryptedPassword": "a8f3d9e2b1c4f5...",
      "salt": "wqAoW+coa1Zpj8+iUA...",
      "iv": "0dW5kJg8f3a1b2c..."
    },
    {
      "title": "Gmail",
      "username": "pablo@gmail.com",
      "email": "pablo@gmail.com",
      "url": "https://gmail.com",
      "notes": "Correo principal",
      "categoryName": "Correo",
      "customFields": [],
      "encryptedPassword": "c9d2e1f3a4b5c6...",
      "salt": "xrBpX+dpa2Aqk9+jVB...",
      "iv": "1eX6lKh9g4b2c3d..."
    }
  ]
}
```

**📋 Campos VISIBLES** (texto claro):
- `title`, `username`, `email`, `url`, `notes`, `categoryName`, `customFields`
- Puedes ver qué contraseñas tienes sin descifrar el archivo
- Fácil de auditar y buscar entradas específicas

**🔒 Campos CIFRADOS** (AES-256-GCM):
- `encryptedPassword`: La contraseña real cifrada
- `salt`: Salt único para esta entrada (16 bytes, Base64)
- `iv`: IV único para esta entrada (12 bytes, Base64)
- Cada contraseña tiene su propio salt/IV para máxima seguridad

**Nombre sugerido**: `keyguard-backup-2024-01-26.json`

**¿Dónde guardar el backup?**:
- ✅ USB externo (almacenamiento físico seguro)
- ✅ Nube cifrada (Dropbox, Google Drive, OneDrive)
- ✅ Servidor personal/NAS
- ✅ Gestor de contraseñas secundario
- ❌ NO en el mismo disco que KeyGuard (pierde sentido el backup)

### Importar contraseñas

1. Haz clic en **"📥 Importar Contraseñas"** en el menú lateral
2. Selecciona el archivo de backup (.json)
3. Ingresa la contraseña de backup
4. [Opcional] Haz clic en **"Validar"** para verificar antes de importar
5. Elige el modo de importación:
   - **Agregar** (recomendado): Mantiene contraseñas actuales, solo agrega nuevas
   - **Reemplazar**: ELIMINA todo y restaura desde backup
6. Haz clic en **"Importar"**
7. ✅ Verás un resumen: importadas, omitidas (duplicadas), errores

### Dos modos de importación

#### Modo Agregar (replaceExisting = false)
- ✅ **Más seguro**: Mantiene todas las contraseñas actuales
- ✅ Solo agrega las que no existan (comparación por título)
- ✅ No pierdes datos existentes
- ✅ **Recomendado** para la mayoría de usuarios

**Ejemplo**:
```
Contraseñas actuales: Facebook, Gmail, Netflix
Backup contiene: Facebook (diferente), Twitter, Spotify

Resultado:
✅ Facebook: Se omite (ya existe)
✅ Twitter: Se importa (nueva)
✅ Spotify: Se importa (nueva)

Total actuales: Gmail, Facebook, Netflix (sin cambios)
Total importadas: Twitter, Spotify (agregadas)
```

#### Modo Reemplazar (replaceExisting = true)
- ⚠️ **PELIGROSO**: ELIMINA todas las contraseñas actuales primero
- ⚠️ Luego importa todo del backup
- ✅ Útil para: Restauración completa, migración a nuevo dispositivo
- ⚠️ **Requiere confirmación adicional**

**Ejemplo**:
```
Contraseñas actuales: Facebook, Gmail, Netflix
Backup contiene: Twitter, Spotify

Resultado:
❌ Se eliminan: Facebook, Gmail, Netflix
✅ Se importan: Twitter, Spotify

Total: Twitter, Spotify (solo las del backup)
```

### Validación previa

Antes de importar, puedes validar el backup:
- Verifica que la contraseña es correcta
- Muestra información sin modificar datos:
  - Versión del backup
  - Fecha de exportación
  - Número de contraseñas
  - Versión de KeyGuard usada

Esto te ayuda a:
- Confirmar que tienes el archivo correcto
- Evitar errores de "contraseña incorrecta" después de eliminar datos
- Ver qué contiene el backup antes de decidir

### Detección de duplicados

Al importar en modo "Agregar", KeyGuard compara por **título**:
- Si una contraseña con el mismo título ya existe → se omite
- Si el título es diferente → se importa como nueva entrada
- **Nota**: Si tienes dos "Facebook" con usuarios diferentes, solo se mantiene la actual

### Buenas prácticas

**Frecuencia de backups**:
- 📅 **Semanal**: Si agregas contraseñas frecuentemente
- 📅 **Mensual**: Uso normal
- 📅 **Después de cambios importantes**: Actualización masiva de contraseñas

**Almacenamiento**:
- 🔒 Usa contraseña fuerte y diferente para cada backup
- 🔒 Guarda copias en al menos 2 ubicaciones diferentes
- 🔒 Prueba restaurar periódicamente para verificar que funciona
- 🔒 Considera cifrar la carpeta completa donde guardas los backups (VeraCrypt, BitLocker)

**Rotación de backups**:
- Mantén backups de los últimos 3-6 meses
- Elimina backups antiguos de forma segura (triturador de archivos)

### Casos de uso

**Caso 1: Backup periódico de seguridad**
```
1. Crear backup mensual: keyguard-backup-2024-01.json
2. Guardar en: Google Drive / carpeta cifrada
3. Probar restauración en máquina virtual
4. Eliminar backup del mes anterior
```

**Caso 2: Migración a nuevo dispositivo**
```
1. En dispositivo antiguo: Exportar todo
2. Copiar archivo a USB/nube
3. En dispositivo nuevo: Instalar KeyGuard
4. Importar con modo "Reemplazar"
```

**Caso 3: Compartir contraseñas familiares**
```
1. Exportar solo las contraseñas que quieres compartir manualmente
   (editar el JSON cifrado no es posible, exporta todo)
2. Compartir archivo + contraseña de forma segura (en persona/Signal)
3. Familia importa con modo "Agregar"
```

**Caso 4: Recuperación de desastre**
```
1. Disco duro falla
2. Instalar KeyGuard en nuevo disco
3. Recuperar backup de la nube
4. Importar con modo "Reemplazar"
5. ✅ Todas las contraseñas restauradas
```

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

### Verificación de Contraseñas Filtradas (HIBP)

- **API**: Have I Been Pwned v3 (https://api.pwnedpasswords.com/)
- **Método**: k-anonymity para proteger privacidad
- **Protocolo**:
  1. Cálculo local de SHA-1 hash de la contraseña
  2. Envío de solo los primeros 5 caracteres del hash (prefix)
  3. Recepción de ~500-800 hashes con el mismo prefix
  4. Comparación local del suffix completo
- **Cliente HTTP**: Java 11+ HttpClient con timeout de 10 segundos
- **Rate limiting**: Delay de 100ms entre requests en verificación masiva
- **Manejo de errores**: Degradación elegante si la API no está disponible
- **Costo**: Completamente gratuito, sin API key requerida
- **Database**: 12+ mil millones de contraseñas de brechas reales

**¿Por qué SHA-1?**: Aunque SHA-1 está deprecado para almacenamiento de contraseñas, es perfectamente seguro para este caso de uso porque:
- No se usa para autenticación
- Solo se usa para búsqueda en una base de datos pública
- La API de HIBP requiere SHA-1 específicamente
- El hash nunca se almacena, solo se calcula en memoria

### Auto-logout por Inactividad

- **Timeout**: 3 minutos de inactividad
- **Eventos monitoreados**: Movimiento del ratón, clicks, teclas, scroll
- **Implementación**: Timer con reseteo en cada evento de usuario
- **Notificación**: Alert antes de cerrar sesión automáticamente

### Export/Import con Cifrado

- **Cifrado**: AES-256-GCM (idéntico al de la base de datos interna)
- **Derivación de clave**: PBKDF2-SHA256 con 100,000 iteraciones
- **Salt**: 16 bytes (128 bits) generado aleatoriamente por backup
- **IV**: 12 bytes (96 bits) generado aleatoriamente por backup
- **Tag de autenticación GCM**: 128 bits (detecta manipulación)
- **Formato**: JSON con metadata legible + datos cifrados en Base64
- **Librería**: Gson 2.10.1 para serialización JSON
- **Sin IDs internos**: Backups portables entre instalaciones
- **Categorías automáticas**: Crea categorías faltantes al importar
- **Detección de duplicados**: Comparación por título en modo agregar
- **Validación previa**: Verifica contraseña sin modificar datos

**¿Por qué cifrar si ya está cifrado en la BD?**
- Permite almacenar en nube (Dropbox, Drive, etc.)
- Protege si el archivo cae en manos incorrectas
- Contraseña de backup puede ser diferente (compartir con familia)
- Compatible con principio zero-knowledge

**Proceso de exportación**:
1. Descifrar todas las contraseñas de la BD
2. Convertir a formato portable (sin IDs, con nombres de categorías)
3. Serializar a JSON
4. Generar salt e IV aleatorios
5. Derivar clave desde contraseña de backup
6. Cifrar JSON con AES-256-GCM
7. Codificar a Base64 y guardar con metadata

**Proceso de importación**:
1. Leer y parsear JSON del archivo
2. Decodificar Base64
3. Derivar clave desde contraseña de backup
4. Descifrar datos (si falla → contraseña incorrecta)
5. Crear categorías faltantes
6. Verificar duplicados (si modo agregar)
7. Insertar en BD con cifrado normal

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
