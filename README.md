# ms_torneos_security

Microservicio de seguridad de **Arcanos**, la plataforma de torneos. Implementa las HU-SE-01 a 10 del Anexo 1 (Módulo de Seguridad): usuarios, perfiles, roles, permisos, registro, inicio de sesión con correo o con Google y GitHub, sesiones, recuperación de contraseña y verificación en dos pasos.

Spring Boot 4.1.1 · Java 17 · Spring Security (JWT y OAuth2) · JPA/Hibernate · MySQL · Lombok.

El front que lo usa es `frontend_torneos`.

## Requisitos

- Java 17. Maven no hace falta instalarlo: el proyecto trae `mvnw`.
- MySQL, con el usuario `root`.
- `ms_notificaciones` en `http://localhost:5000` para enviar los correos con códigos (verificación en dos pasos y recuperar contraseña). Sin él, el resto funciona; esas acciones responden 503.

## Configuración (una vez por PC)

### 1. Base de datos

En MySQL Workbench o en la consola de MySQL:

```sql
CREATE DATABASE torneos_security_db;
```

Las tablas las crea Hibernate al arrancar (`spring.jpa.hibernate.ddl-auto=update`).

### 2. Variables de entorno

`application.properties` no guarda ninguna clave: las toma de estas variables.

| Variable | Qué es | De dónde sale |
|---|---|---|
| `DB_PASSWORD` | Contraseña del usuario `root` de MySQL | La de tu MySQL |
| `JWT_SECRET` | Clave con que se firman los tokens de sesión; mínimo 32 bytes | Se genera en cada PC (ver abajo) |
| `ADMIN_EMAIL` | Correo del administrador inicial | El que quieran |
| `ADMIN_PASSWORD` | Contraseña del administrador inicial | La que quieran |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | App OAuth de Google | Google Cloud Console → APIs y servicios → Credenciales |
| `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` | App OAuth de GitHub | GitHub → Settings → Developer settings → OAuth Apps |
| `MICROSOFT_CLIENT_ID`, `MICROSOFT_CLIENT_SECRET` | Login con Microsoft, todavía sin implementar | Cualquier texto, por ejemplo `pendiente`. Deben existir: sin ellas el backend no arranca |

Para generar `JWT_SECRET` en PowerShell:

```powershell
$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Para guardar cada variable en tu usuario de Windows:

```powershell
setx JWT_SECRET "la-clave-generada"
setx DB_PASSWORD "tu-contraseña-de-mysql"
# ...una línea por cada variable de la tabla
```

`setx` no cambia las terminales que ya están abiertas: ciérralas, o reinicia VS Code, antes de arrancar el backend.

- Las credenciales de Google y GitHub pueden ser las mismas en todos los PCs del equipo. Compártanlas por privado, nunca en el repositorio.
- Si crean una app nueva, la URL de retorno es `http://localhost:8181/login/oauth2/code/google` (o `/github`). En GitHub, la página de inicio es `http://localhost:5173`.
- Si cambian `JWT_SECRET`, los tokens ya emitidos dejan de servir y todos deben iniciar sesión de nuevo.

## Cómo correrlo

```powershell
.\mvnw.cmd spring-boot:run
```

Queda en `http://localhost:8181`.

En el primer arranque, `DatabaseSeeder` deja lista la base:

- los 27 permisos del módulo de Administración: usuarios, perfiles, roles, permisos, asignación de roles y asignación de permisos;
- los 30 permisos del módulo de Gestión Deportiva, que protegen las rutas `/api/deportiva/...` de ms_torneos_deportiva: deportes, equipos, asignación de deportes, jugadores, contratos, habilidades, asignación de habilidades y perfil deportivo. Se siembran aquí porque los permisos se guardan y se validan en este servicio;
- el rol **Administrador**, con todos los permisos;
- el rol **Administrador deportivo**, solo con los 30 de Gestión Deportiva;
- el usuario `ADMIN_EMAIL`, con el rol Administrador.

En los arranques siguientes solo crea lo que falte. Por ejemplo, si al rol Administrador le quitaron alguno de sus permisos, se lo vuelve a dar; y si a un permiso le falta alguno de sus endpoints (porque el microservicio agregó una ruta, como `PATCH /api/deportiva/skills/{id}/disable`), se lo agrega sin tocar los roles. El Administrador deportivo es distinto: recibe sus permisos solo cuando se crea el rol, así que los cambios que se le hagan desde la matriz se respetan. Si el correo del administrador ya existe, conserva ese usuario y su contraseña.

### Si no arranca

| Mensaje | Causa |
|---|---|
| `Could not resolve placeholder '...'` | Falta esa variable de entorno, o la terminal se abrió antes del `setx` |
| `La clave JWT (JWT_SECRET) debe tener al menos 32 bytes` | La clave es muy corta |
| `Access denied for user 'root'` | `DB_PASSWORD` no coincide con la contraseña de MySQL |
| `Unknown database 'torneos_security_db'` | Falta crear la base (paso 1) |
| `Communications link failure` | MySQL no está encendido |
| `Port 8181 was already in use` | Ya hay otro backend corriendo |

## Pruebas

`.\mvnw.cmd test` arranca la aplicación completa, así que necesita MySQL y las variables de entorno, y escribe en `torneos_security_db`. Para generar el `.jar` sin correrlas:

```powershell
.\mvnw.cmd package -DskipTests
```

## Cómo funciona la seguridad

- **Sesión:** al iniciar sesión se guarda una sesión en la base y se entrega un JWT firmado con `JWT_SECRET`, que dura 30 minutos. Se envía en cada petición como `Authorization: Bearer <token>`. Cerrar sesión o desactivar al usuario lo deja sin valor aunque no haya vencido.
- **Rutas públicas:** registro, login, `2fa/verify`, recuperar contraseña y el login con Google o GitHub.
- **Rutas que solo piden sesión:** `/api/auth/me`, `/api/auth/me/profile`, `/api/auth/logout`, activar o desactivar el 2FA y la validación remota de permisos.
- **Validación remota de permisos:** otro microservicio reenvía el JWT que recibió y pregunta si ese usuario puede usar una URL y un método (`POST /api/internal/security/permissions-validation` con `{"url": "/torneos/tournaments/15", "method": "DELETE"}`). Responde 200 con `{"allowed": true}` o `{"allowed": false}`; el microservicio que pregunta convierte el `false` en 403, y si este servicio no responde, no ejecuta la operación. La URL se compara con las rutas de los permisos igual que las de este servicio (`/15` coincide con `/{id}`); para que no se confundan rutas iguales de dos microservicios, regístralas con un prefijo del servicio.
- **Todas las demás piden un permiso.** Cada permiso es un recurso más una operación (por ejemplo, `usuarios · LIST`) con las rutas que habilita. En cada petición se revisa si algún rol del usuario lo tiene; si no, responde 403. Los cambios en los permisos de un rol aplican de inmediato, sin volver a iniciar sesión.
- **Protección del administrador** (`AdminGuard`): nadie puede desactivarse, eliminarse ni quitarse el rol Administrador a sí mismo; siempre debe quedar un administrador activo; y el rol Administrador no se puede renombrar.
- **Duraciones**, en `application.properties`: sesión 30 minutos, código de dos pasos 5 minutos, código de recuperación 15 minutos.

## Rutas

| Base | Para qué | HU | Acceso |
|---|---|---|---|
| `/api/auth` | `register`, `login`, `logout`, `me` | SE-07, SE-08 | Pública o con sesión |
| `/api/auth/2fa` | `verify`, `enable`, `disable` | SE-10 | Pública o con sesión |
| `/api/auth/password` | `forgot`, `reset` | SE-09 | Pública |
| `/api/auth/me/profile` | Ver, crear y editar el perfil propio | SE-02 | Con sesión |
| `/oauth2/authorization/{google\|github}` | Iniciar sesión con un proveedor | SE-08 | Pública |
| `/api/users` | Usuarios: crear, listar, editar, eliminar, detalle, activar y desactivar | SE-01 | Permiso `usuarios` |
| `/api/users/{userId}/profile` | Perfil de cualquier usuario | SE-02 | Permiso `perfiles` |
| `/api/roles` | Roles | SE-03 | Permiso `roles` |
| `/api/user-roles` | Asignar y quitar roles a los usuarios | SE-04 | Permiso `asignacion-roles` |
| `/api/permissions` | Permisos y las rutas de cada uno | SE-05 | Permiso `permisos` |
| `/api/role-permissions` | Permisos de cada rol, la matriz y los permisos efectivos de un usuario | SE-06 | Permiso `asignacion-permisos` |
| `/api/internal/security/permissions-validation` | Otro microservicio pregunta si el usuario del token tiene permiso para una URL y un método | — | Con sesión |

## Errores

Los errores de la aplicación responden:

```json
{ "errorCase": "NOT_FOUND", "message": "Usuario no encontrado con id: 7" }
```

Los de validación responden un mensaje por campo, por ejemplo `{ "email": "..." }`.

| HTTP | `errorCase` | Cuándo |
|---|---|---|
| 400 | `INVALID_OPERATION`, `INVALID_BODY`, `INVALID_PARAMETER` | Una regla no se cumple, el JSON está mal formado o un parámetro no es válido (ej. `/api/users/abc`) |
| 401 | `UNAUTHORIZED` | Credenciales incorrectas, sin token, token vencido o sesión cerrada |
| 403 | `FORBIDDEN` | Ninguno de los roles del usuario tiene el permiso. También una ruta que no existe, porque ningún permiso la habilita |
| 404 | `NOT_FOUND` | El registro no existe |
| 405 | `METHOD_NOT_ALLOWED` | La ruta no acepta ese método |
| 409 | `ALREADY_EXISTS`, `DATA_CONFLICT` | Un dato repetido, o un registro que otro necesita |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | El cuerpo no se envió como JSON |
| 503 | `SERVICE_UNAVAILABLE` | `ms_notificaciones` no respondió al enviar un correo |
| 500 | `INTERNAL_ERROR` | Error no previsto; el detalle queda en el log del backend |
