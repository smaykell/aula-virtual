# Aula Virtual — API

Backend REST del aula virtual. El frontend vive en otro repositorio y consume esta
API sobre HTTP.

| | |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Build | Gradle (wrapper 9.7.1) |
| Base de datos | PostgreSQL 13+ |
| Migraciones | Flyway |
| Documentación | OpenAPI / Swagger UI |
| Autenticación | JWT (HS256), sin estado |

## Puesta en marcha

### 1. Crear la base de datos

```bash
psql -U postgres -c "CREATE DATABASE aula_virtual;"
psql -U postgres -c "CREATE DATABASE aula_virtual_test;"   # para los tests de integración
```

### 2. Configurar el entorno

```bash
cp .env.example .env
```

Ajusta al menos `DB_USER` y `DB_PASSWORD`. En el perfil `dev` el resto tiene valores
por defecto razonables para trabajar en local.

Las variables se leen del entorno del proceso, no del archivo `.env` directamente.
Para exportarlas en una sesión de bash:

```bash
set -a && source .env && set +a
```

### 3. Arrancar

```bash
./gradlew bootRun
```

La API queda en `http://localhost:8080/api`. `bootRun` activa el perfil `dev`; no hay
perfil por defecto, así que un `java -jar` sin `--spring.profiles.active` exige
`JWT_SECRET`.

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api/v3/api-docs |
| Health | http://localhost:8080/api/actuator/health |

## Comandos

```bash
./gradlew build          # compila y ejecuta los tests
./gradlew test           # solo tests
./gradlew bootRun        # arranca en perfil dev
./gradlew bootJar        # empaqueta el jar ejecutable
```

Para producción:

```bash
java -jar build/libs/aula-virtual-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Estructura

```
io.github.smaykell.aulavirtual
├── AulaVirtualApplication.java
├── config/            configuración transversal (OpenAPI, auditoría JPA, CORS, Clock)
├── security/          JWT: propiedades, servicio, filtro y cadena de seguridad
├── common/
│   ├── domain/        BaseEntity (id UUID + createdAt/updatedAt)
│   ├── dto/           ApiError, PageResponse
│   ├── web/           ApiErrorWriter
│   └── exception/     ApiException, ResourceNotFoundException, handler global
└── modules/
    └── user/          usuarios, login y catálogo de roles aplicado
```

La organización es por **vertical slice**: cada módulo de dominio agrupa su
entidad, repositorio, servicio, controlador y DTOs en un solo paquete. Ver
`CLAUDE.md` para las convenciones.

### Añadir un módulo

1. Crea el paquete `modules/<name>/` (en inglés, como el resto del código).
2. Añade la migración Flyway `V<n>__create_<name>.sql` en
   `src/main/resources/db/migration`.
3. La entidad extiende `BaseEntity`; los DTOs son `record` y nunca se exponen
   entidades directamente.
4. El controlador devuelve `PageResponse` para listados paginados y lanza
   `ResourceNotFoundException` cuando corresponda.

## Esquema de base de datos

Flyway es la única fuente de verdad: `spring.jpa.hibernate.ddl-auto` está en
`validate`, así que Hibernate nunca modifica el esquema y falla al arrancar si las
entidades no coinciden con las tablas.

Las migraciones ya aplicadas **no se editan**; los cambios van en una migración
nueva.

## Autenticación y autorización

La cadena de seguridad es *stateless*: cada petición se autentica con el token JWT
de la cabecera `Authorization: Bearer <token>`.

`POST /api/auth/login` recibe `{"username", "password"}` y devuelve el token, su
duración en segundos, el rol y los permisos de la sesión. Las authorities viajan en
el claim `roles`: el rol con prefijo (`ROLE_ADMIN`) y, junto a él, cada permiso de
ese rol (`users:read`). Por eso `hasRole(...)` y `hasAuthority('users:create')`
funcionan sin traducción.

| Endpoint | Quién | Qué |
|---|---|---|
| `POST /api/auth/login` | público | devuelve el token y los permisos de la sesión |
| `GET /api/users` | `users:read` | listado paginado, filtro opcional `?active=` |
| `POST /api/users` | `users:create` | crea un usuario y devuelve 201 |

Rutas públicas: `/auth/login`, `/actuator/health`, `/actuator/info`,
`/v3/api-docs/**`, `/swagger-ui/**`. Todo lo demás exige token.

### Roles y permisos

El catálogo vive en código: `security/Permission` enumera los permisos y
`security/Role` asigna a cada rol los suyos. Un usuario tiene exactamente un rol.

| Rol | Permisos | Administra a |
|---|---|---|
| `SUPER_ADMIN` | `users:read`, `users:create` | `ADMIN`, `TEACHER`, `STUDENT` |
| `ADMIN` | `users:read`, `users:create` | `TEACHER`, `STUDENT` |
| `TEACHER` | — | — |
| `STUDENT` | — | — |

`Role.manageableRoles()` es la única fuente de la última columna, y de ella salen
dos reglas: un admin no puede tocar a otro admin, y nadie —tampoco el superadmin—
crea un `SUPER_ADMIN` por API. El listado de `/api/users` devuelve solo los roles
que el solicitante administra, así que la cuenta del superadmin no aparece para
nadie.

### El primer superadmin

`V2__create_users.sql` siembra el usuario `superadmin` con la contraseña
`Superadmin.2026`. Ese hash está versionado en el repositorio: **cambia la
contraseña en el primer arranque de cada entorno**.

### Usuarios desactivados

`users.active` corta el acceso en dos puntos: el login responde 403 y cada
operación de `/api/users` vuelve a comprobarlo contra la base, de modo que un token
emitido antes de la desactivación deja de servir en el acto. Todavía no hay endpoint
que cambie ese campo.

## Errores

Todos los errores comparten el mismo cuerpo:

```json
{
  "timestamp": "2026-09-18T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "La peticion contiene campos invalidos",
  "path": "/api/courses",
  "traceId": "a1b2c3d4",
  "errors": [
    { "field": "name", "message": "no debe estar vacío" }
  ]
}
```

El `traceId` también se escribe en el log del servidor, así que sirve para
localizar el error exacto sin exponer detalles internos al cliente. Los errores del
propio framework (404, 405, cuerpo ilegible) usan el mismo formato y conservan su
código: nunca se convierten en 500.

## Tests

```bash
./gradlew test
```

`JwtServiceTest`, `RoleTest`, `UserServiceTest` y `AuthenticationServiceTest`
(unitarios) y `SecurityConfigTest`, `GlobalExceptionHandlerTest`, `UserControllerTest`
y `AuthControllerTest` (rodajas web con MockMvc y tokens reales) corren siempre: no
necesitan base de datos.

`AulaVirtualApplicationTests` levanta el contexto completo y necesita Postgres
(base `aula_virtual_test`). Si no hay base accesible, **se omite en lugar de
fallar**, para que el build funcione en una máquina recién clonada. Crea la base y
volverá a ejecutarse de verdad.

## Perfiles

| Perfil | Uso |
|---|---|
| `dev` | SQL en el log, actuator con metrics, secreto JWT de desarrollo. Lo activa `bootRun` |
| `test` | usado por los tests de integración |
| `prod` | logging mínimo, `flyway.clean` y `baseline-on-migrate` deshabilitados |
