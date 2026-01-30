# Manual de Usuario - KeyGuard

**Gestor de Contraseñas Seguro (Aplicación de Escritorio)**
Versión: 1.0.0

---

## Tabla de Contenidos

1. Introducción
2. Requisitos del Sistema
3. Primer Inicio y Registro
4. Inicio de Sesión
5. Panel Principal (Bóveda)
6. Gestión de Contraseñas
7. Búsqueda y Organización
8. Auto-Lock y Cierre de Sesión
9. Copias de Seguridad (Backups)
10. Importación de Datos
11. Seguridad y Privacidad
12. Limitaciones Conocidas
13. Buenas Prácticas de Uso
14. Futuras Mejoras Planeadas

---

## 1. Introducción

KeyGuard es una aplicación de escritorio desarrollada para el almacenamiento y gestión segura de contraseñas. Está diseñada bajo un enfoque **Zero-Knowledge**, lo que significa que solo el usuario tiene acceso a su información confidencial.

La aplicación funciona completamente de manera local y no depende de servidores externos para el almacenamiento o procesamiento de datos sensibles.

---

## 2. Requisitos del Sistema

* Sistema Operativo: Windows / Linux
* Java: JDK 8 o superior
* Espacio en disco: mínimo 100 MB

---

## 3. Primer Inicio y Registro

En el primer uso, el usuario debe:

1. Crear una **contraseña maestra**
2. Confirmar la contraseña
3. Generar y guardar una **Recovery Key**

⚠️ La Recovery Key se muestra una sola vez y debe almacenarse en un lugar seguro.

---

## 4. Inicio de Sesión

Para acceder a la aplicación:

* Ingrese su contraseña maestra
* Presione el botón **Iniciar Sesión**

La contraseña maestra no se guarda ni se transmite. Se utiliza únicamente para derivar la clave criptográfica que desbloquea la bóveda.

---

## 5. Panel Principal (Bóveda)

Una vez autenticado, el usuario accede a la bóveda donde puede:

* Visualizar contraseñas almacenadas
* Crear nuevas entradas
* Editar o eliminar registros existentes

Las contraseñas se muestran ocultas por defecto.

---

## 6. Gestión de Contraseñas

Cada entrada de contraseña puede contener:

* Título
* Usuario o correo
* Contraseña (cifrada)
* URL
* Categoría
* Notas adicionales

Las contraseñas se cifran automáticamente al momento de guardarse.

---

## 7. Búsqueda y Organización

KeyGuard permite:

* Buscar por título o usuario
* Filtrar por categoría
* Ordenar entradas

Los metadatos visibles facilitan estas operaciones sin necesidad de descifrar información sensible.

---

## 8. Auto-Lock y Cierre de Sesión

La aplicación se bloquea automáticamente tras un período de inactividad.

El usuario puede:

* Configurar el tiempo de auto-lock
* Cerrar sesión manualmente

Esto evita accesos no autorizados en caso de descuidos.

---

## 9. Copias de Seguridad (Backups)

KeyGuard permite exportar las contraseñas a un archivo JSON cifrado.

Características del backup:

* No contiene contraseñas en texto plano
* No incluye la contraseña maestra
* Es portable entre dispositivos

El archivo solo puede descifrarse ingresando la contraseña maestra correcta.

---

## 10. Importación de Datos

El usuario puede importar manualmente archivos JSON previamente exportados.

Durante la importación:

* Se validan las estructuras
* Se mapean las entradas mediante identificadores únicos (UUID)
* No se sobrescriben datos sin confirmación

---

## 11. Seguridad y Privacidad

KeyGuard ha sido diseñado bajo un enfoque **Zero-Knowledge**, lo que significa que solo el usuario puede acceder a sus contraseñas.

### Protección de la información sensible

* Las contraseñas se cifran con **AES-256-GCM**
* La clave de cifrado se deriva mediante **PBKDF2 con SHA-256**
* Se utilizan **100,000 iteraciones** y un **salt único por usuario**
* Cada contraseña cuenta con un **IV único**

La clave criptográfica nunca se almacena.

### Contraseña maestra

* No se guarda en texto plano
* No se envía a servidores
* No se incluye en backups

Si se pierde y no se cuenta con la Recovery Key, las contraseñas no pueden recuperarse.

### Recovery Key

* Permite recuperar el acceso en caso de olvido
* No descifra contraseñas directamente
* Se usa únicamente para re-cifrar la clave de acceso

### Metadatos visibles

Algunos campos no sensibles se almacenan en texto claro para facilitar búsquedas y organización. Las contraseñas siempre permanecen cifradas.

### Amenazas mitigadas

| Amenaza              | Mitigación           |
| -------------------- | -------------------- |
| Robo del archivo     | Cifrado AES-256-GCM  |
| Fuerza bruta         | PBKDF2 + iteraciones |
| Manipulación         | Autenticación GCM    |
| Acceso no autorizado | Auto-Lock            |

---

## 12. Limitaciones Conocidas

* No sincroniza automáticamente entre dispositivos
* No ofrece recuperación sin Recovery Key
* No cuenta con versión web (por ahora)

---

## 13. Buenas Prácticas de Uso

* Utilizar una contraseña maestra robusta
* Guardar la Recovery Key en un lugar seguro
* Realizar copias de seguridad periódicas
* Cerrar sesión en equipos compartidos

---

## 14. Futuras Mejoras Planeadas

* Versión web compatible con Zero-Knowledge
* Importación directa desde la app de escritorio
* Autenticación de dos factores (2FA)
* Integración con TOTP

---

**KeyGuard – Tu información, solo en tus manos.**
