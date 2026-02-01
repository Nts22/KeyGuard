# Cómo Funciona el Cifrado v1.1 - KeyGuard

## Tabla de Contenidos

1. [Introducción](#introducción)
2. [Visión General](#visión-general)
3. [Componentes del Cifrado](#componentes-del-cifrado)
4. [Proceso de Exportación](#proceso-de-exportación)
5. [Proceso de Importación](#proceso-de-importación)
6. [Seguridad y Criptografía](#seguridad-y-criptografía)
7. [Comparación v1.0 vs v1.1](#comparación-v10-vs-v11)
8. [Preguntas Frecuentes](#preguntas-frecuentes)

---

## Introducción

KeyGuard v1.1 introduce un nuevo formato de backup optimizado que mantiene la máxima seguridad mientras mejora el rendimiento. Este documento explica en detalle cómo funciona el sistema de cifrado.

### ¿Qué Protege el Cifrado?

- ✅ **Contraseñas**: Todas las contraseñas están cifradas con AES-256-GCM
- ✅ **Integridad**: GCM detecta cualquier manipulación del archivo
- ✅ **Privacidad**: Solo el poseedor de la contraseña de backup puede descifrar

### ¿Qué NO Está Cifrado?

- 📋 **Metadata**: Títulos, usuarios, emails, URLs, notas, categorías
- 📋 **Estructura**: Formato JSON legible
- 📋 **Campos personalizados**: Visibles en texto claro

**Razón**: Balance entre seguridad y usabilidad. Puedes auditar qué contraseñas tienes sin necesidad de descifrar.

---

## Visión General

### Formato del Backup v1.1

```json
{
  "version": "1.1",
  "exportDate": "2024-01-30T10:30:00",
  "entryCount": 2,
  "appVersion": "1.0.0",
  "crypto": {
    "kdf": "PBKDF2-SHA256",
    "iterations": 100000,
    "salt": "ny1q7qN5rQMyjOBxpmew/A==",
    "cipher": "AES-256-GCM"
  },
  "entries": [
    {
      "id": "uuid-1234",
      "title": "Facebook",
      "username": "user@email.com",
      "email": "user@email.com",
      "url": "https://facebook.com",
      "notes": "Mi cuenta personal",
      "categoryName": "Redes Sociales",
      "customFields": [],
      "encryptedPassword": "wHQPOB8Zd4TfmmpupZ7POO+T+I6LnZxZmQ==",
      "iv": "5+8cffPirHjd5psP"
    }
  ]
}
```

### Flujo de Cifrado Simplificado

```
Usuario exporta con contraseña "miBackup123!"
              ↓
    Generar salt global
              ↓
    Derivar clave AES-256 (PBKDF2)
              ↓
    Para cada contraseña:
      - Generar IV único
      - Cifrar con AES-256-GCM
      - Guardar en JSON
```

---

## Componentes del Cifrado

### 1. Salt Global (`crypto.salt`)

**¿Qué es?**
```
Bytes aleatorios: [0x9F, 0x2D, 0x6A, 0xEE, 0xA3, 0x79, ...]
Codificado Base64: "ny1q7qN5rQMyjOBxpmew/A=="
Tamaño: 16 bytes (128 bits)
```

**¿Para qué sirve?**
- Convierte tu contraseña de backup en una clave AES-256 real
- Asegura que dos backups con la misma contraseña tengan claves diferentes
- Protege contra ataques de rainbow tables

**¿Por qué es global?**
- Una sola derivación de clave (más rápido)
- Formato estándar de la industria
- Suficiente seguridad con IVs únicos

**Generación** (en código):
```java
// BackupServiceImpl.java línea 100
byte[] globalSalt = generateRandomBytes(16);
String saltBase64 = Base64.getEncoder().encodeToString(globalSalt);
```

### 2. Clave Derivada (no guardada)

**¿Qué es?**
```
Resultado de: PBKDF2(contraseña, salt, 100000 iteraciones)
Tamaño: 32 bytes (256 bits)
Algoritmo: PBKDF2-SHA256
```

**¿Dónde está?**
- ❌ **NO se guarda** en el archivo JSON
- ✅ Se calcula cada vez al exportar/importar
- ✅ Solo existe en memoria durante el proceso

**Derivación** (en código):
```java
// BackupServiceImpl.java línea 409-421
KeySpec spec = new PBEKeySpec(
    contraseña.toCharArray(),
    salt,
    100_000,  // Iteraciones
    256       // Bits de clave
);
SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
byte[] keyBytes = factory.generateSecret(spec).getEncoded();
```

**¿Por qué 100,000 iteraciones?**
- Recomendación OWASP 2023
- ~100ms en hardware moderno (aceptable UX)
- Dificulta ataques de fuerza bruta

### 3. IV Único por Entrada (`entry.iv`)

**¿Qué es?**
```
Bytes aleatorios: [0xE7, 0xEF, 0x1C, 0x7D, 0xF3, ...]
Codificado Base64: "5+8cffPirHjd5psP"
Tamaño: 12 bytes (96 bits)
```

**¿Para qué sirve?**
- Garantiza que cifrar la misma contraseña dos veces produzca resultados diferentes
- Cada entrada tiene su propio IV aleatorio
- Protege contra análisis de patrones

**¿Por qué es importante?**
```
❌ Sin IV:
  Facebook con "pass123" → cifra a "ABC..."
  Gmail con "pass123"    → cifra a "ABC..." (¡IGUAL!)
  → Atacante sabe que tienen la misma contraseña

✅ Con IV único:
  Facebook con "pass123" + IV1 → cifra a "ABC..."
  Gmail con "pass123" + IV2    → cifra a "XYZ..." (¡DIFERENTE!)
  → No hay forma de detectar contraseñas iguales
```

**Generación** (en código):
```java
// BackupServiceImpl.java línea 119
byte[] entryIv = generateRandomBytes(12);
String ivBase64 = Base64.getEncoder().encodeToString(entryIv);
```

### 4. Contraseña Cifrada (`entry.encryptedPassword`)

**¿Qué es?**
```
Tu contraseña → Cifrada con AES-256-GCM → Base64
"MiPassword123!" → [bytes cifrados] → "wHQPOB8Zd4TfmmpupZ7POO+T+I6LnZxZmQ=="
```

**Proceso de cifrado**:
```java
// BackupServiceImpl.java línea 437-442
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
GCMParameterSpec spec = new GCMParameterSpec(128, iv);
cipher.init(Cipher.ENCRYPT_MODE, key, spec);
byte[] encrypted = cipher.doFinal(plaintext);
```

**Componentes del resultado cifrado**:
- Texto cifrado: Tu contraseña ininteligible
- Tag GCM (128 bits): Para autenticación e integridad

---

## Proceso de Exportación

### Paso 1: Usuario Ingresa Contraseña de Backup

```
┌─────────────────────────────────────┐
│ Exportar Contraseñas                │
├─────────────────────────────────────┤
│ Contraseña de backup:               │
│ [miBackup123!_________] [👁]        │
│                                     │
│ Confirmar:                          │
│ [miBackup123!_________] [👁]        │
│                                     │
│     [ Exportar ]  [ Cancelar ]      │
└─────────────────────────────────────┘
```

### Paso 2: Generar Salt Global

```java
// Código: BackupServiceImpl.java línea 100
byte[] globalSalt = new byte[16];
new SecureRandom().nextBytes(globalSalt);

// Resultado (ejemplo):
globalSalt = [0x9F, 0x2D, 0x6A, 0xEE, 0xA3, 0x79, 0xAD, 0x03,
              0x32, 0x8C, 0xE0, 0x71, 0xA6, 0x67, 0xB0, 0xFC]

// Convertir a Base64 para JSON:
String saltBase64 = Base64.getEncoder().encodeToString(globalSalt);
// saltBase64 = "ny1q7qN5rQMyjOBxpmew/A=="
```

**Diagrama**:
```
┌────────────────────────────────────────┐
│ SecureRandom                           │
│ (Entropía del sistema operativo)      │
└────────────────┬───────────────────────┘
                 ↓
         16 bytes aleatorios
         [0x9F, 0x2D, ...]
                 ↓
         Codificar Base64
                 ↓
     "ny1q7qN5rQMyjOBxpmew/A=="
                 ↓
         Guardar en crypto.salt
```

### Paso 3: Derivar Clave Global

```java
// Código: BackupServiceImpl.java línea 409-421
KeySpec spec = new PBEKeySpec(
    "miBackup123!".toCharArray(),  // Contraseña del usuario
    globalSalt,                     // Salt del paso 2
    100_000,                        // Iteraciones
    256                             // Longitud de clave en bits
);

SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
byte[] derivedKey = factory.generateSecret(spec).getEncoded();

// Resultado:
derivedKey = [32 bytes de clave AES-256]
```

**Diagrama del proceso PBKDF2**:
```
Entrada:
┌─────────────────────────────────────────────────┐
│ Contraseña: "miBackup123!"                      │
│ Salt: [0x9F, 0x2D, 0x6A, ...]                   │
│ Iteraciones: 100,000                            │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│ Iteración 1: HMAC-SHA256(contraseña + salt)    │
│ Iteración 2: HMAC-SHA256(resultado anterior)   │
│ Iteración 3: HMAC-SHA256(resultado anterior)   │
│ ...                                             │
│ Iteración 100,000: HMAC-SHA256(...)            │
└─────────────────────────────────────────────────┘
                    ↓
         XOR todos los resultados
                    ↓
Salida: 32 bytes de clave AES-256
[0xA7, 0x3F, 0x89, 0x2C, 0xDE, ...]
```

**¿Por qué tantas iteraciones?**
```
Sin PBKDF2 (derivación simple):
  Probar 1 millón de contraseñas = 1 segundo

Con PBKDF2 (100,000 iteraciones):
  Probar 1 millón de contraseñas = 27 horas

Resultado: 100,000x más lento para atacantes
```

### Paso 4: Cifrar Cada Contraseña

Para cada entrada de contraseña:

```java
// Código: BackupServiceImpl.java línea 119-125

// 4.1 Generar IV único
byte[] entryIv = new byte[12];
new SecureRandom().nextBytes(entryIv);

// 4.2 Cifrar
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
GCMParameterSpec spec = new GCMParameterSpec(128, entryIv);
cipher.init(Cipher.ENCRYPT_MODE, derivedKey, entryIv);
byte[] encrypted = cipher.doFinal("MiPasswordFB!".getBytes());

// 4.3 Codificar resultados
String ivBase64 = Base64.getEncoder().encodeToString(entryIv);
String encryptedBase64 = Base64.getEncoder().encodeToString(encrypted);
```

**Diagrama del cifrado AES-GCM**:
```
Entrada 1: Facebook
┌─────────────────────────────────────────────┐
│ Contraseña original: "MiPasswordFB!"       │
│ Clave AES-256: [32 bytes derivados]        │
│ IV único: [12 bytes aleatorios]            │
└─────────────────────────────────────────────┘
                    ↓
         ┌──────────────────────┐
         │   AES-256-GCM        │
         │  - Cifrado           │
         │  - Autenticación     │
         └──────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│ Texto cifrado: [17 bytes]                   │
│ Tag GCM: [16 bytes]                         │
│ Total: 33 bytes                             │
└─────────────────────────────────────────────┘
                    ↓
         Codificar Base64
                    ↓
"wHQPOB8Zd4TfmmpupZ7POO+T+I6LnZxZmQ=="
```

### Paso 5: Ensamblar JSON

```java
// Código: BackupServiceImpl.java línea 151-169
BackupDTO backup = BackupDTO.builder()
    .version("1.1")
    .exportDate(LocalDateTime.now())
    .entryCount(entries.size())
    .appVersion("1.0.0")
    .crypto(CryptoMetadata.builder()
        .kdf("PBKDF2-SHA256")
        .iterations(100_000)
        .salt(saltBase64)
        .cipher("AES-256-GCM")
        .build())
    .entries(backupEntries)
    .build();

// Guardar en archivo
gson.toJson(backup, outputFile);
```

**JSON Final**:
```json
{
  "version": "1.1",
  "exportDate": "2024-01-30T10:30:00",
  "entryCount": 1,
  "appVersion": "1.0.0",
  "crypto": {
    "kdf": "PBKDF2-SHA256",
    "iterations": 100000,
    "salt": "ny1q7qN5rQMyjOBxpmew/A==",
    "cipher": "AES-256-GCM"
  },
  "entries": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "title": "Facebook",
      "username": "pablo@email.com",
      "email": "pablo@email.com",
      "url": "https://facebook.com",
      "notes": "Mi cuenta personal",
      "categoryName": "Redes Sociales",
      "customFields": [],
      "encryptedPassword": "wHQPOB8Zd4TfmmpupZ7POO+T+I6LnZxZmQ==",
      "iv": "5+8cffPirHjd5psP"
    }
  ]
}
```

---

## Proceso de Importación

### Paso 1: Usuario Selecciona Archivo y Contraseña

```
┌─────────────────────────────────────┐
│ Importar Contraseñas                │
├─────────────────────────────────────┤
│ Archivo:                            │
│ [backup-2024-01-30.json] [Browse]  │
│                                     │
│ Contraseña de backup:               │
│ [miBackup123!_________] [👁]        │
│                                     │
│     [ Importar ]  [ Cancelar ]      │
└─────────────────────────────────────┘
```

### Paso 2: Leer y Parsear JSON

```java
// Código: BackupServiceImpl.java línea 197-201
BackupDTO backup;
try (FileReader reader = new FileReader(inputFile)) {
    backup = gson.fromJson(reader, BackupDTO.class);
}

// Validar
if (backup == null || backup.getEntries() == null) {
    throw new BackupException("Archivo inválido");
}
```

### Paso 3: Extraer Salt y Derivar Clave

```java
// Código: BackupServiceImpl.java línea 262-263
String saltBase64 = backup.getCrypto().getSalt();
byte[] globalSalt = Base64.getDecoder().decode(saltBase64);

SecretKey key = deriveKey("miBackup123!", globalSalt);
```

**Diagrama**:
```
Leer del JSON:
"crypto.salt" = "ny1q7qN5rQMyjOBxpmew/A=="
        ↓
Decodificar Base64
        ↓
Salt bytes: [0x9F, 0x2D, 0x6A, ...]
        ↓
Usuario ingresa: "miBackup123!"
        ↓
PBKDF2-SHA256 con 100,000 iteraciones
        ↓
Clave AES-256: [32 bytes]

SI la contraseña es correcta:
  ✅ Clave IDÉNTICA a la usada al exportar

SI la contraseña es incorrecta:
  ❌ Clave DIFERENTE
  → Descifrado fallará con "Tag mismatch"
```

### Paso 4: Descifrar Cada Contraseña

Para cada entrada:

```java
// Código: BackupServiceImpl.java línea 283-289

// 4.1 Leer datos cifrados
String ivBase64 = entry.getIv();
String encryptedBase64 = entry.getEncryptedPassword();

// 4.2 Decodificar Base64
byte[] entryIv = Base64.getDecoder().decode(ivBase64);
byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);

// 4.3 Descifrar
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
GCMParameterSpec spec = new GCMParameterSpec(128, entryIv);
cipher.init(Cipher.DECRYPT_MODE, key, spec);
byte[] decrypted = cipher.doFinal(encrypted);

// 4.4 Convertir a String
String password = new String(decrypted, UTF_8);
```

**Diagrama del descifrado**:
```
Entrada 1: Facebook
┌─────────────────────────────────────────────┐
│ encryptedPassword (Base64):                 │
│ "wHQPOB8Zd4TfmmpupZ7POO+T+I6LnZxZmQ=="     │
└─────────────────────────────────────────────┘
                    ↓
         Decodificar Base64
                    ↓
┌─────────────────────────────────────────────┐
│ Bytes cifrados: [33 bytes]                  │
│ - Texto cifrado: 17 bytes                   │
│ - Tag GCM: 16 bytes                         │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│ iv (Base64): "5+8cffPirHjd5psP"            │
│ → [12 bytes]                                │
│                                             │
│ Clave: [32 bytes derivados]                 │
└─────────────────────────────────────────────┘
                    ↓
         ┌──────────────────────┐
         │   AES-256-GCM        │
         │  1. Verificar Tag    │
         │  2. Descifrar        │
         └──────────────────────┘
                    ↓
         SI tag es válido:
                    ↓
┌─────────────────────────────────────────────┐
│ Contraseña original: "MiPasswordFB!"       │
└─────────────────────────────────────────────┘

         SI tag NO es válido:
                    ↓
         ❌ Exception: "Tag mismatch"
```

### Paso 5: Crear Entradas en la BD

```java
// Código: BackupServiceImpl.java línea 270-282
PasswordEntryDTO newEntry = PasswordEntryDTO.builder()
    .title(backupEntry.getTitle())
    .username(backupEntry.getUsername())
    .email(backupEntry.getEmail())
    .password(decryptedPassword)  // ← La contraseña descifrada
    .url(backupEntry.getUrl())
    .notes(backupEntry.getNotes())
    .categoryId(categoryId)
    .customFields(backupEntry.getCustomFields())
    .build();

passwordEntryService.create(newEntry);
```

**Nota**: Al guardar en la BD, KeyGuard usa su propio cifrado (con la contraseña maestra del usuario actual).

---

## Seguridad y Criptografía

### Algoritmos Utilizados

| Componente | Algoritmo | Parámetros | Seguridad |
|------------|-----------|------------|-----------|
| **Derivación de clave** | PBKDF2-SHA256 | 100,000 iteraciones | ⭐⭐⭐⭐⭐ |
| **Cifrado** | AES-256-GCM | Clave 256 bits, IV 96 bits | ⭐⭐⭐⭐⭐ |
| **Salt** | SecureRandom | 128 bits | ⭐⭐⭐⭐⭐ |
| **IV** | SecureRandom | 96 bits | ⭐⭐⭐⭐⭐ |

### Fortalezas del Sistema

#### 1. Derivación de Clave Robusta (PBKDF2)

**¿Qué hace?**
```
Contraseña débil: "password"
      ↓
100,000 iteraciones de hashing
      ↓
Clave fuerte: [32 bytes impredecibles]
```

**Protección contra fuerza bruta**:
```
Hardware de consumo (2024):
- Sin PBKDF2: ~1 billón de intentos/segundo
- Con PBKDF2 (100k iter): ~10,000 intentos/segundo
- Reducción: 100 millones de veces más lento

Hardware GPU high-end:
- Sin PBKDF2: ~100 billones de intentos/segundo
- Con PBKDF2: ~1 millón de intentos/segundo
- Reducción: 100 millones de veces más lento

Contraseña de 12 caracteres mixtos:
- Combinaciones: 62^12 = 3.2 × 10^21
- Tiempo con PBKDF2: 10 mil millones de años
```

#### 2. Salt Aleatorio

**¿Qué protege?**
```
❌ Sin Salt:
  Usuario A: "password123" → clave: 0xABC...
  Usuario B: "password123" → clave: 0xABC... (IGUAL)
  → Atacante puede usar tablas precomputadas (rainbow tables)

✅ Con Salt:
  Usuario A: "password123" + salt1 → clave: 0xABC...
  Usuario B: "password123" + salt2 → clave: 0xXYZ... (DIFERENTE)
  → Atacante debe computar específicamente para cada salt
```

**Números**:
```
Salt de 128 bits:
- Combinaciones posibles: 2^128 = 3.4 × 10^38
- Imposible precomputar tablas para todos los salts
```

#### 3. IV Único por Entrada

**¿Qué protege?**
```
❌ Sin IV único:
  Facebook: "pass123" → cifra a "ABC..."
  Gmail:    "pass123" → cifra a "ABC..." (IGUAL)
  → Atacante detecta contraseñas duplicadas
  → Análisis de frecuencia posible

✅ Con IV único:
  Facebook: "pass123" + IV1 → cifra a "ABC..."
  Gmail:    "pass123" + IV2 → cifra a "XYZ..." (DIFERENTE)
  → Imposible detectar patrones
  → Cada cifrado es único
```

#### 4. AES-256-GCM

**Cifrado (AES-256)**:
```
Tamaño de clave: 256 bits
Combinaciones: 2^256 = 1.15 × 10^77
Universos estimados: 10^23

Para romper por fuerza bruta:
- Supercomputadora más rápida (2024): 1 exaflop
- Tiempo necesario: 3.7 × 10^51 años
- Edad del universo: 1.4 × 10^10 años
- Resultado: IMPOSIBLE
```

**Autenticación (GCM)**:
```
Tag GCM: 128 bits
Detecta:
  - Modificación de texto cifrado
  - Cambio de IV
  - Alteración de datos asociados
  - Ataques de bit-flipping

Si alguien modifica 1 bit:
  → Tag no coincide
  → Descifrado falla inmediatamente
  → No se revela información
```

### Vectores de Ataque y Defensas

#### 1. Ataque de Fuerza Bruta

**Ataque**: Probar todas las contraseñas posibles

**Defensa**:
- PBKDF2 con 100,000 iteraciones (lento)
- Contraseña mínima de 8 caracteres
- Recomendación de 12+ caracteres

**Tiempo para romper**:
```
Contraseña de 8 caracteres (solo minúsculas):
26^8 = 2 × 10^11 combinaciones
Con PBKDF2: ~600 años (GPU high-end)

Contraseña de 12 caracteres (mixta):
62^12 = 3 × 10^21 combinaciones
Con PBKDF2: 10 mil millones de años
```

#### 2. Ataque de Rainbow Tables

**Ataque**: Usar tablas precomputadas de contraseñas

**Defensa**:
- Salt aleatorio de 128 bits
- Cada backup tiene salt diferente

**Efectividad**: ❌ INEFECTIVO
```
Tamaño de una rainbow table típica: 10 GB
Número de salts posibles: 2^128 = 3 × 10^38

Espacio necesario para todas las rainbow tables:
10 GB × 3 × 10^38 = 3 × 10^39 GB
= 3 × 10^27 exabytes

Para referencia:
- Toda la información digital del mundo: ~64 zettabytes = 6.4 × 10^13 GB
- Resultado: IMPOSIBLE almacenar
```

#### 3. Ataque de Manipulación de Archivo

**Ataque**: Modificar el archivo JSON cifrado

**Defensa**:
- GCM tag de autenticación
- Cualquier cambio invalida el tag

**Ejemplo**:
```json
Original:
"encryptedPassword": "wHQPOB8Zd4TfmmpupZ7POO+T+I6LnZxZmQ=="

Atacante modifica 1 bit:
"encryptedPassword": "wHQPOB8Zd5TfmmpupZ7POO+T+I6LnZxZmQ=="
                           ↑ cambiado

Al intentar importar:
→ GCM verifica tag
→ Tag no coincide
→ Exception: "Tag mismatch!"
→ Descifrado fallado
→ No se revela información
```

#### 4. Ataque de Diccionario

**Ataque**: Probar contraseñas comunes (password, 123456, etc.)

**Defensa**:
- PBKDF2 hace cada intento lento
- Salt único por backup

**Números**:
```
Diccionario común: 10 millones de contraseñas
Hardware GPU high-end: ~1 millón intentos/seg con PBKDF2

Tiempo: 10 segundos por backup

Pero:
- Cada backup tiene salt diferente
- Atacante debe intentar para cada archivo
- Si tienes 100 backups: 1,000 segundos = 16 minutos
```

**Mitigación adicional**: Usar contraseñas largas (12+ caracteres) que no estén en diccionarios.

---

## Comparación v1.0 vs v1.1

### Tabla Comparativa

| Aspecto | v1.0 | v1.1 |
|---------|------|------|
| **Salt** | Uno por entrada | ✅ Uno global |
| **Derivación de clave** | Una por entrada | ✅ Una sola vez |
| **IV** | Único por entrada | ✅ Único por entrada |
| **UUID** | ❌ No | ✅ Sí |
| **Metadata crypto** | ❌ No | ✅ Objeto separado |
| **Seguridad** | Muy alta | ✅ Muy alta |
| **Rendimiento** | Lento | ✅ Rápido (15x) |
| **Formato** | Plano | ✅ Estructurado |

### Rendimiento

#### Exportar 100 Contraseñas

```
v1.0:
  - 100 derivaciones de clave (PBKDF2)
  - 100 × 100ms = 10 segundos
  - 100 salts generados
  - 100 IVs generados

v1.1:
  - 1 derivación de clave (PBKDF2)
  - 1 × 100ms = 0.1 segundos
  - 1 salt generado
  - 100 IVs generados

Mejora: 100x más rápido en derivación
```

#### Importar 100 Contraseñas

```
v1.0:
  - 100 derivaciones de clave
  - 100 × 100ms = 10 segundos
  - 100 descifraciones AES

v1.1:
  - 1 derivación de clave
  - 1 × 100ms = 0.1 segundos
  - 100 descifraciones AES

Mejora: 100x más rápido en derivación
```

### ¿Por Qué v1.1 Es Igual de Seguro?

**Pregunta**: Si v1.0 tiene un salt por entrada, ¿no es más seguro que v1.1 con salt global?

**Respuesta**: No, ambos son igualmente seguros. He aquí por qué:

#### Análisis de Seguridad

**v1.0 (Salt por entrada)**:
```
Facebook:
  Salt1 + "miBackup" → Clave1
  Clave1 + IV1 → Cifrado1

Gmail:
  Salt2 + "miBackup" → Clave2
  Clave2 + IV2 → Cifrado2

Seguridad:
- Cada entrada tiene clave diferente ✅
- Cada entrada tiene IV diferente ✅
- Total unicidad: Clave + IV ✅
```

**v1.1 (Salt global)**:
```
Todas las entradas:
  SaltGlobal + "miBackup" → ClaveGlobal

Facebook:
  ClaveGlobal + IV1 → Cifrado1

Gmail:
  ClaveGlobal + IV2 → Cifrado2

Seguridad:
- Todas las entradas usan misma clave ⚠️
- Cada entrada tiene IV diferente ✅
- Total unicidad: IV único garantiza seguridad ✅
```

**Conclusión**:
El IV único es suficiente para garantizar que cada cifrado sea único. La clave compartida NO reduce la seguridad porque:
1. El IV es aleatorio y único (96 bits)
2. AES-GCM con IV único produce cifrados completamente diferentes
3. Estándar de la industria (TLS, Signal, WhatsApp usan una clave por sesión)

---

## Preguntas Frecuentes

### ¿Es seguro el formato v1.1?

**Respuesta**: Sí, absolutamente. v1.1 usa:
- AES-256-GCM (estándar de la industria)
- PBKDF2 con 100,000 iteraciones (OWASP 2023)
- IV único por entrada (máxima seguridad)
- Salt aleatorio de 128 bits

Es el mismo nivel de seguridad usado por:
- Signal (mensajería)
- WhatsApp (end-to-end encryption)
- TLS 1.3 (HTTPS)

### ¿Por qué los títulos y usuarios no están cifrados?

**Respuesta**: Balance entre seguridad y usabilidad.

**Ventajas de datos visibles**:
- ✅ Puedes auditar qué contraseñas tienes sin descifrar
- ✅ Fácil buscar una entrada específica
- ✅ Detectar duplicados sin descifrar
- ✅ Verificar integridad del backup

**Datos protegidos**:
- 🔒 Solo las contraseñas están cifradas
- 🔒 Son los datos más sensibles
- 🔒 Imposible obtenerlas sin la contraseña de backup

### ¿Puedo usar la misma contraseña de backup que mi contraseña maestra?

**Respuesta**: Puedes, pero no es recomendado.

**Pros de usar la misma**:
- ✅ Solo una contraseña para recordar
- ✅ Más conveniente

**Pros de usar diferente**:
- ✅ Mayor seguridad (separación de concerns)
- ✅ Si compartes el backup, no revelas tu contraseña maestra
- ✅ Si alguien obtiene tu backup, no puede acceder a tu BD principal

**Recomendación**: Usa contraseñas diferentes para máxima seguridad.

### ¿Qué pasa si olvido la contraseña de backup?

**Respuesta**: No hay forma de recuperar los datos.

- ❌ No hay "contraseña de recuperación"
- ❌ No hay "backdoor"
- ❌ No hay forma de resetear

**Esto es una característica de seguridad**, no un bug. Zero-knowledge significa que NADIE (ni siquiera nosotros) puede acceder a tus datos sin la contraseña.

**Prevención**:
- ✅ Usa un gestor de contraseñas para la contraseña de backup
- ✅ Anota la contraseña en papel en lugar seguro
- ✅ Prueba restaurar antes de eliminar el backup antiguo

### ¿Puedo almacenar el backup en la nube?

**Respuesta**: Sí, es seguro.

El backup está cifrado con AES-256-GCM. Puedes almacenarlo en:
- ✅ Google Drive
- ✅ Dropbox
- ✅ OneDrive
- ✅ iCloud
- ✅ Servidor personal
- ✅ Email

Sin la contraseña de backup, el archivo es solo basura aleatoria.

### ¿Cada cuánto debo hacer backups?

**Recomendaciones**:

**Uso frecuente** (agregas contraseñas semanalmente):
- 📅 Backup semanal
- 📅 Rotar: mantener últimos 4 backups (1 mes)

**Uso normal** (agregas contraseñas ocasionalmente):
- 📅 Backup mensual
- 📅 Rotar: mantener últimos 6 backups (6 meses)

**Después de cambios importantes**:
- 📅 Backup inmediato
- Ejemplos: actualizar muchas contraseñas, agregar datos críticos

### ¿Puedo ver el contenido del backup sin importarlo?

**Respuesta**: Parcialmente, sí.

**Puedes ver** (texto claro):
- ✅ Títulos de entradas
- ✅ Usuarios
- ✅ Emails
- ✅ URLs
- ✅ Notas
- ✅ Categorías
- ✅ Metadata (fecha, versión, cantidad)

**NO puedes ver** (cifrado):
- ❌ Contraseñas
- ❌ Necesitas la contraseña de backup para descifrarlas

**Para ver**:
```bash
# Con herramienta jq (Linux/Mac):
cat backup.json | jq '.entries[] | {title, username, categoryName}'

# O simplemente abrir en editor de texto
```

### ¿Es compatible v1.1 con v1.0?

**Importar**:
- ❌ v1.1 NO puede importar backups v1.0 directamente
- ✅ Solución: Re-exportar backups antiguos con versión actual

**Exportar**:
- ✅ KeyGuard siempre exporta en el formato más reciente (v1.1)

**Migración de v1.0 a v1.1**:
1. Importar backup v1.0 en KeyGuard antiguo
2. Actualizar KeyGuard a última versión
3. Exportar nuevamente (genera formato v1.1)

---

## Referencias

### Estándares Utilizados

- **AES-256**: [FIPS 197](https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.197.pdf)
- **GCM**: [NIST SP 800-38D](https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf)
- **PBKDF2**: [RFC 8018](https://tools.ietf.org/html/rfc8018)
- **JSON**: [RFC 8259](https://tools.ietf.org/html/rfc8259)
- **Base64**: [RFC 4648](https://tools.ietf.org/html/rfc4648)

### Lectura Recomendada

- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [NIST Digital Identity Guidelines](https://pages.nist.gov/800-63-3/)
- [Cryptographic Right Answers](https://latacora.micro.blog/2018/04/03/cryptographic-right-answers.html)

---

**Versión del documento**: 1.0
**Última actualización**: 30 de enero de 2024
**Compatible con**: KeyGuard 1.0.0+
