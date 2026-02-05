# Manual de Usuario - KeyGuard

**Gestor de Contraseñas Seguro (Aplicación de Escritorio)**
Versión: 1.1.0

---

## Tabla de Contenidos

1. Introducción
2. Requisitos del Sistema
3. Primer Inicio y Registro
4. Inicio de Sesión
5. Panel Principal (Bóveda)
6. Gestión de Contraseñas
7. Búsqueda y Organización
8. Verificación de Contraseñas
9. Temas Visuales
10. Auto-Lock y Cierre de Sesión
11. Copias de Seguridad (Backups)
12. Importación de Datos
13. Seguridad y Privacidad
14. Limitaciones Conocidas
15. Buenas Prácticas de Uso
16. Futuras Mejoras Planeadas

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

* Visualizar contraseñas almacenadas en una tabla con columnas que se ajustan automáticamente al ancho disponible
* Crear nuevas entradas mediante el botón **+** ubicado en la barra superior
* Editar o eliminar registros existentes usando los botones de acción por fila
* Filtrar entradas por categoría desde el sidebar
* Buscar por título, usuario o email en tiempo real

Las contraseñas se muestran ocultas por defecto y solo se revelan al hacer clic en **Ver**.

### Sidebar

El panel lateral organiza las funciones principales:

* **Categorías** — lista con scroll vertical independiente. Cuando el número de categorías supera el espacio disponible, aparece un scrollbar solo en esa sección, manteniendo las demás áreas siempre visibles. Permite crear, editar y eliminar categorías.
* **Seguridad** — acceso rápido a la verificación de contraseñas, exportación e importación de backups.
* **Tema** — selector desplegable para cambiar el tema visual de la aplicación (ver sección 9).
* **Cuenta** — opciones de cierre de sesión y salida de la aplicación.

El sidebar puede colapsar y expandir usando el botón ☰ ubicado en la esquina superior izquierda.

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

* Buscar por título, usuario o email usando la barra de búsqueda superior
* Filtrar por categoría seleccionando una del sidebar
* Ver el historial de cambios de cada contraseña

Las categorías se organizan en el sidebar con scroll vertical. Si hay muchas categorías, solo esa sección desplaza; las secciones de Seguridad, Tema y Cuenta permanecen fijas abajo.

Los resultados de búsqueda se muestran en tiempo real junto con un indicador de cantidad de coincidencias.

---

## 8. Verificación de Contraseñas

KeyGuard incluye un dashboard de seguridad que analiza todas las contraseñas almacenadas. Se accede desde **Seguridad → Verificar Contraseñas** en el sidebar.

El análisis se inicia automáticamente al abrir el diálogo y se presenta en tres pestañas:

### Brechas conocidas

Verifica cada contraseña contra la base de datos pública de Have I Been Pwned (HIBP). Por privacidad, solo se envían los primeros 5 caracteres del hash SHA-1 de la contraseña, nunca la contraseña en texto plano (protocolo k-anonymity).

Cada contraseña recibe un nivel de severidad:

| Nivel | Descripción |
| --- | --- |
| Segura | No encontrada en brechas conocidas |
| Riesgo Bajo | Apariciones limitadas |
| Riesgo Medio | Apariciones moderadas |
| Riesgo Alto | Apariciones frecuentes |
| Riesgo Crítico | Muy comprometida — cambiar inmediatamente |

### Duplicadas

Identifica entradas que comparten la misma contraseña y las agrupa visualmente por color. Las entradas del mismo grupo siempre se muestran juntas para facilitar la identificación.

### Débiles

Muestra las contraseñas clasificadas como **Muy débil** o **Débil**, junto con su longitud actual. Se recomienda cambiarlas por contraseñas más largas y complejas.

El resumen superior del dashboard muestra en tiempo real la cantidad total de contraseñas, las comprometidas, las duplicadas y las débiles.

---

## 9. Temas Visuales

KeyGuard soporta varios temas visuales que se pueden cambiar sin reiniciar la aplicación. El cambio se aplica instantáneamente a todas las pantallas y diálogos abiertos.

### Cambiar de tema

1. En el sidebar, desplázate hasta la sección **🎨 Tema**
2. Selecciona el tema deseado del menú desplegable

El tema seleccionado se guarda automáticamente en disco y se recupera en la próxima sesión.

### Temas disponibles

| Tema | Descripción |
| --- | --- |
| **Claro** | Tema por defecto con fondos blancos y azules suaves |
| **Océano** | Azules profundos con acentos en cian. Diseñado para reducir la fatiga visual en uso prolongado |

---

## 10. Auto-Lock y Cierre de Sesión

KeyGuard tiene dos mecanismos de protección por inactividad que se activan automáticamente:

* **Auto-Lock (2 minutos)** — la aplicación se bloquea y muestra la pantalla de desbloqueo. El usuario puede retomar la sesión ingresando su contraseña maestra sin perder los datos en pantalla.
* **Cierre de sesión por inactividad (3 minutos)** — si no hay actividad durante 3 minutos, la sesión se cierra completamente y el usuario debe autenticarse de nuevo.

Ambos timers se resetean automáticamente con cualquier actividad del ratón o teclado dentro de la aplicación. Es decir, mientras el usuario esté interactuando con la app, ninguno de los dos se dispara.

La aplicación también se bloquea inmediatamente al minimizar la ventana.

El usuario puede además:

* Bloquear manualmente pulsando el botón **🔒** en la barra superior
* Cerrar sesión desde **Cuenta → Cerrar Sesión** en el sidebar

---

## 11. Copias de Seguridad (Backups)

KeyGuard permite exportar las contraseñas a un archivo JSON cifrado.

Características del backup:

* No contiene contraseñas en texto plano
* No incluye la contraseña maestra
* Es portable entre dispositivos

El archivo solo puede descifrarse ingresando la contraseña maestra correcta.

---

## 12. Importación de Datos

El usuario puede importar manualmente archivos JSON previamente exportados.

Durante la importación:

* Se validan las estructuras
* Se mapean las entradas mediante identificadores únicos (UUID)
* No se sobrescriben datos sin confirmación
* Las categorías que no existen se crean automáticamente

---

## 13. Seguridad y Privacidad

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
| Acceso no autorizado | Auto-Lock + timeout de sesión |
| Contraseñas filtradas | Verificación HIBP con k-anonymity |

---

## 14. Limitaciones Conocidas

* No sincroniza automáticamente entre dispositivos
* No ofrece recuperación sin Recovery Key
* No cuenta con versión web (por ahora)
* El tema visual del popup de ComboBox no se adapta al tema seleccionado

---

## 15. Buenas Prácticas de Uso

* Utilizar una contraseña maestra robusta
* Guardar la Recovery Key en un lugar seguro
* Realizar copias de seguridad periódicas
* Cerrar sesión en equipos compartidos
* Ejecutar la verificación de contraseñas de forma periódica (cada 3-6 meses)
* Cambiar inmediatamente las contraseñas marcadas como **Riesgo Crítico**

---

## 16. Futuras Mejoras Planeadas

* Versión web compatible con Zero-Knowledge
* Importación directa desde la app de escritorio
* Autenticación de dos factores (2FA)
* Integración con TOTP
* Más temas visuales disponibles

---

**KeyGuard – Tu información, solo en tus manos.**
