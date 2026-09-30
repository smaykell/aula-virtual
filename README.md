# Aula Virtual — API

Backend REST del aula virtual. El frontend vive en otro repositorio y consume esta
API sobre HTTP.

| | |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Build | Gradle (wrapper 9.7.1) |
| Base de datos | PostgreSQL 18 |
| Migraciones | Flyway |
| Documentación | OpenAPI / Swagger UI |
| Autenticación | JWT (HS256), sin estado |

## Puesta en marcha

### 1. Levantar la base de datos y el almacenamiento

El entorno local vive en Docker (`compose.yaml`): PostgreSQL 18 y un MinIO que hace de
S3 para el material de los cursos.

```bash
docker compose up -d
```

Deja listos:

| Servicio | Dirección | Credenciales |
|---|---|---|
| PostgreSQL, bases `aula_virtual` y `aula_virtual_test` | `localhost:5432` | `aula_virtual` / `aula_virtual` |
| MinIO, API S3 con el bucket `aula-virtual` | `http://localhost:9000` | `aula_virtual` / `aula_virtual` |
| Consola de MinIO | http://localhost:9001 | las mismas |

Las credenciales son genéricas a propósito y coinciden con los valores por defecto de
`application.yml`, así que no hay nada que exportar. La base de pruebas la crea
`docker/postgres/init` y el bucket, con su regla de caducidad para subidas sin confirmar,
el servicio de un solo uso `minio-setup`; los dos solo actúan la primera vez, sobre
volúmenes vacíos. `docker compose down -v` borra los volúmenes y deja empezar de cero.

Si en la máquina hay un PostgreSQL instalado de forma nativa escuchando en el 5432, hay
que pararlo: Docker publica el puerto igualmente, pero las conexiones a `localhost` las
atiende el nativo y la aplicación acabaría en otra base. En Windows, desde una consola de
administrador: `Stop-Service postgresql-x64-16` (o el nombre de tu versión).

La imagen de MinIO sale de `quay.io` con un tag fijo: la edición comunitaria dejó de
publicarse en Docker Hub. Para desarrollo basta; para producción se usa un S3 de verdad.

### 2. Configurar el entorno

En local se puede saltar: con las bases recién creadas, todos los valores por defecto
sirven. No hay archivo de configuración fuera de `src/main/resources`; lo ajustable son
variables del entorno del proceso.

| Variable | Propiedad | Por defecto |
|---|---|---|
| `DB_URL` | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/aula_virtual` |
| `DB_USER` | `spring.datasource.username` | `aula_virtual` |
| `DB_PASSWORD` | `spring.datasource.password` | `aula_virtual` |
| `DB_POOL_SIZE` | `spring.datasource.hikari.maximum-pool-size` | `10` |
| `SERVER_PORT` | `server.port` | `8080` |
| `CORS_ALLOWED_ORIGINS` | `app.cors.allowed-origins` | los tres `localhost` habituales del frontend |
| `COURSE_INVITATION_BASE_URL` | `app.courses.invitation-base-url` | `http://localhost:5173/join` |
| `PASSWORD_RESET_URL` | `app.accounts.password-reset-url` | `http://localhost:5173/reset-password`; apunta al **frontend** |
| `PASSWORD_RESET_LIFETIME` | `app.accounts.password-reset-lifetime` | `PT30M` |
| `PASSWORD_RESET_COOLDOWN` | `app.accounts.password-reset-cooldown` | `PT2M` entre dos enlaces a la misma cuenta |
| `LOGIN_MAX_FAILURES` | `app.accounts.login-throttle.max-failures` | `5` contraseñas erróneas antes de bloquear |
| `LOGIN_FAILURE_WINDOW` | `app.accounts.login-throttle.window` | `PT15M` en los que se suman los fallos |
| `LOGIN_LOCKOUT` | `app.accounts.login-throttle.lockout` | `PT15M` de bloqueo |
| `NOTIFICATIONS_ENABLED` | `app.notifications.enabled` | `true`; en `false` no se consume la cola |
| `NOTIFICATIONS_POLL_INTERVAL` | `app.notifications.poll-interval` | `PT30S` |
| `NOTIFICATIONS_BATCH_SIZE` | `app.notifications.batch-size` | `50` |
| `NOTIFICATIONS_MAX_ATTEMPTS` | `app.notifications.max-attempts` | `5` |
| `NOTIFICATIONS_RETRY_DELAY` | `app.notifications.retry-delay` | `PT1M`, y se duplica en cada reintento |
| `NOTIFICATIONS_FROM` | `app.notifications.from` | `aula-virtual@localhost` |
| `SPRING_MAIL_HOST` | `spring.mail.host` | **sin declarar**: mientras falte, los correos solo se escriben en el log |
| `SPRING_MAIL_PORT` | `spring.mail.port` | `587` |
| `SPRING_MAIL_USERNAME` | `spring.mail.username` | vacío |
| `SPRING_MAIL_PASSWORD` | `spring.mail.password` | vacío |
| `SPRING_MAIL_AUTH` | `mail.smtp.auth` | `true` |
| `SPRING_MAIL_STARTTLS` | `mail.smtp.starttls.enable` | `true`; ponlo en `false` si usas el puerto 465 |
| `SPRING_MAIL_TIMEOUT` | los tres timeouts de `mail.smtp` | `5000` ms |
| `STORAGE_ENDPOINT` | `app.storage.endpoint` | `http://localhost:9000` (el MinIO de Docker); **definida y vacía** apunta a AWS S3 |
| `STORAGE_REGION` | `app.storage.region` | `us-east-1` |
| `STORAGE_BUCKET` | `app.storage.bucket` | `aula-virtual` |
| `STORAGE_ACCESS_KEY` | `app.storage.access-key` | `aula_virtual`, la del MinIO local; **definida y vacía** usa la cadena de credenciales de AWS (el rol IAM de la instancia) |
| `STORAGE_SECRET_KEY` | `app.storage.secret-key` | `aula_virtual`, la del MinIO local; vacía, igual que la anterior |
| `STORAGE_PATH_STYLE` | `app.storage.path-style` | `true`, lo que pide MinIO; en AWS, `false` |
| `STORAGE_UPLOAD_TTL` | `app.storage.upload-ttl` | `PT15M` de vida de un enlace de subida |
| `STORAGE_DOWNLOAD_TTL` | `app.storage.download-ttl` | `PT10M` de vida de un enlace de descarga |
| `JWT_SECRET` | `app.security.jwt.secret` | solo en `dev`; en cualquier otro perfil es obligatorio |
| `JWT_ISSUER` | `app.security.jwt.issuer` | `aula-virtual` |
| `JWT_EXPIRATION` | `app.security.jwt.expiration` | `PT8H` en `dev`, `PT1H` en el resto |

Una variable definida gana al valor por defecto. **Definida y vacía no es lo mismo que
ausente**: exportar `JWT_SECRET=` deja la propiedad en blanco, el valor de `dev` no se
aplica y `@NotBlank` corta el arranque. Por lo mismo `spring.mail.host` no se declara en
`application.yml`: si estuviera como `${SPRING_MAIL_HOST:}` quedaría definido y vacío, el
autoconfig de correo arrancaría contra un servidor en blanco y todos los envíos fallarían
en vez de irse al log.

El envío real se enciende **solo** con `SPRING_MAIL_HOST`. `NOTIFICATIONS_FROM` debe ser
la misma dirección con la que te autenticas, o el servidor rechazará el correo. Los
puertos habituales son `587` con STARTTLS (lo que viene por defecto) y `465`, que es SSL
directo y necesita `SPRING_MAIL_STARTTLS=false`.

Para exportarlas en una sesión:

```bash
export JWT_SECRET="$(openssl rand -base64 32)"
```

```powershell
$env:JWT_SECRET = 'la-clave-en-base64'
```

Solo valen para esa sesión: otra ventana, o el botón de arranque del IDE, no las ve.

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
java -jar build/libs/aula-virtual-*.jar --spring.profiles.active=prod
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
├── person/            la identidad: documento, nombres, fecha de nacimiento, sexo
├── user/              la cuenta: login, credenciales y /me
├── teacher/           el perfil de docente
├── administrator/     el perfil de administrador
├── student/           el perfil de estudiante
├── course/            el curso, sus unidades, su material y sus matrículas
├── assignment/        las tareas, sus entregas y las calificaciones
├── settings/          la configuración del centro: cómo se identifican los estudiantes
└── notification/      la cola de correos y quien la consume
```

Los módulos de dominio cuelgan del paquete raíz, al mismo nivel que las zonas
transversales: el paquete raíz ya es la aplicación y un `modules/` intermedio no
distinguía nada que el nombre del paquete no dijera ya.

Una **persona** (`persons`) puede tener varios **perfiles** —docente, estudiante,
administrador— y una sola **cuenta** (`users`). El documento de identidad es la clave
natural que permite reconocer que quien se está registrando como docente ya existe en
la base como estudiante: en ese caso se reutiliza su persona y su cuenta, y solo se le
añade el perfil nuevo. Cada perfil aporta lo suyo y su propio `active`, de modo que se
puede ser docente inactivo y estudiante activo a la vez.

La organización es por **vertical slice**: cada módulo de dominio agrupa su
entidad, repositorio, servicio, controlador y DTOs en un solo paquete. Ver
`CLAUDE.md` para las convenciones.

### Añadir un módulo

1. Crea el paquete `<name>/` bajo el paquete raíz (en inglés, como el resto del código).
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
duración en segundos, los **roles** y los permisos de la sesión. Los roles de una
persona son sus perfiles activos, así que quien es administrador y docente inicia
sesión una vez y lleva los dos. Las authorities viajan en el claim `roles`: cada rol
con prefijo (`ROLE_ADMIN`) y, junto a ellos, la unión de sus permisos
(`teachers:read`). Por eso `hasRole(...)` y `hasAuthority('teachers:create')`
funcionan sin traducción.

| Endpoint | Quién | Qué |
|---|---|---|
| `POST /api/auth/login` | público | devuelve el token y los permisos de la sesión |
| `POST /api/auth/password-reset` | público | recibe `{"identifier"}` (usuario o correo) y responde 202 siempre |
| `POST /api/auth/password-reset/$complete` | público | recibe `{"token", "newPassword"}` y devuelve 204 |
| `GET /api/me` | autenticado | mis datos, mis roles y mis permisos |
| `PUT /api/me` | autenticado | cambia mis datos de persona |
| `POST /api/me/$changePassword` | autenticado | pide la contraseña actual y devuelve 204 |
| `GET /api/persons/$byDocument` | `teachers:create` o `administrators:create` | busca una persona por documento y dice qué es ya |
| `GET /api/administrators` | `administrators:read` | listado paginado, filtro opcional `?active=` |
| `GET /api/administrators/{id}` | `administrators:read` | un administrador |
| `POST /api/administrators` | `administrators:create` | registra un administrador y devuelve 201 |
| `PUT /api/administrators/{id}` | `administrators:update` | cambia sus datos de persona |
| `POST /api/administrators/{id}/$enable` | `administrators:update` | reactiva el perfil |
| `POST /api/administrators/{id}/$disable` | `administrators:update` | desactiva el perfil |
| `POST /api/administrators/{id}/$changePassword` | `administrators:update` | cambia su contraseña y devuelve 204 |
| `GET /api/teachers` | `teachers:read` | listado paginado, filtro opcional `?active=` |
| `GET /api/teachers/{id}` | `teachers:read` | un docente |
| `POST /api/teachers` | `teachers:create` | registra un docente y devuelve 201 |
| `PUT /api/teachers/{id}` | `teachers:update` | cambia los datos de persona |
| `POST /api/teachers/{id}/$enable` | `teachers:update` | reactiva el perfil de docente |
| `POST /api/teachers/{id}/$disable` | `teachers:update` | desactiva el perfil de docente |
| `POST /api/teachers/{id}/$changePassword` | `teachers:update` | cambia la contraseña de su cuenta y devuelve 204 |
| `GET /api/students` | `students:read` | listado paginado, filtro opcional `?active=` |
| `GET /api/students/{id}` | `students:read` | un estudiante |
| `POST /api/students` | `students:create` | registra un estudiante y devuelve 201 |
| `PUT /api/students/{id}` | `students:update` | cambia sus datos de persona |
| `POST /api/students/{id}/$enable` | `students:update` | reactiva el perfil |
| `POST /api/students/{id}/$disable` | `students:update` | desactiva el perfil |
| `POST /api/students/{id}/$changePassword` | `students:update` | cambia su contraseña y devuelve 204 |
| `GET /api/settings` | `settings:read` | cómo se identifican los estudiantes y si el enlace admite inscripciones |
| `PUT /api/settings` | `settings:update` | solo el superadmin |
| `GET /api/invitations/{code}` | **pública** | nombre del curso y del docente, para la pantalla de inscripción |
| `POST /api/invitations/{code}/$register` | **pública** | el estudiante se da de alta y queda matriculado; devuelve 201 |
| `GET /api/courses` | `courses:read` | listado paginado, filtros opcionales `?teacherId=` y `?status=` |
| `GET /api/courses/{id}` | `courses:read` | un curso |
| `POST /api/courses` | `courses:create` | crea un curso y devuelve 201 |
| `PUT /api/courses/{id}` | `courses:update` | cambia sus datos y su docente titular |
| `POST /api/courses/{id}/$archive` | `courses:update` | archiva el curso |
| `POST /api/courses/{id}/$activate` | `courses:update` | devuelve el curso a activo |
| `GET /api/courses/{id}/units` | `courses:read` | las unidades del curso, en orden y con su material |
| `POST /api/courses/{id}/units` | `courses:update` | añade una unidad al final y devuelve 201 |
| `POST /api/courses/{id}/units/$reorder` | `courses:update` | reordena las unidades del curso |
| `GET /api/units/{id}` | `courses:read` | una unidad con su material |
| `PUT /api/units/{id}` | `courses:update` | cambia el título de la unidad |
| `DELETE /api/units/{id}` | `courses:update` | borra la unidad y su material, y devuelve 204 |
| `POST /api/units/{id}/materials` | `courses:update` | publica material en la unidad y devuelve 201 |
| `PUT /api/materials/{id}` | `courses:update` | reemplaza el material |
| `DELETE /api/materials/{id}` | `courses:update` | borra el material y devuelve 204 |
| `POST /api/courses/$join` | `enrollments:create` | el estudiante se inscribe con el código y devuelve 201 |
| `GET /api/me/enrollments` | autenticado, solo estudiantes | mis matrículas y el estado de cada una; quien no sea estudiante activo recibe `CRS_STUDENT_REQUIRED` |
| `GET /api/me/enrollments?code={code}` | autenticado, solo estudiantes | mi matrícula en el curso de esa invitación, o una lista vacía; la pantalla del enlace la usa para saber si ya estoy dentro antes de pedir entrar |
| `GET /api/courses/{id}/enrollments` | `enrollments:read` | el aula del curso, filtro opcional `?status=` |
| `POST /api/enrollments/{id}/$accept` | `enrollments:update` | acepta una solicitud pendiente |
| `POST /api/enrollments/{id}/$reject` | `enrollments:update` | rechaza una solicitud pendiente |
| `POST /api/enrollments/{id}/$withdraw` | `enrollments:update` | retira del curso a un estudiante matriculado |
| `GET /api/units/{id}/assignments` | `assignments:read` | las tareas de la unidad, por fecha límite |
| `POST /api/units/{id}/assignments` | `assignments:create` | publica una tarea y devuelve 201 |
| `POST /api/units/{id}/assignments/$upload` | `assignments:update` | firma la subida de un adjunto del docente |
| `GET /api/assignments/{id}` | `assignments:read` | una tarea |
| `PUT /api/assignments/{id}` | `assignments:update` | cambia la tarea |
| `DELETE /api/assignments/{id}` | `assignments:update` | borra la tarea y devuelve 204 |
| `POST /api/assignments/{id}/$submit` | `submissions:create` | el estudiante entrega, o reemplaza su entrega |
| `POST /api/assignments/{id}/$upload` | `submissions:create` | firma la subida de un adjunto de la entrega |
| `GET /api/attachments/{id}/$download` | `assignments:read` | enlace firmado para descargar un adjunto |
| `GET /api/assignments/{id}/submissions` | `assignments:read` | las entregas: todas para el docente, la suya para el estudiante |
| `GET /api/assignments/{id}/work` | `assignments:read` | cada estudiante activo con su entrega (o ninguna) y su nota; el estudiante recibe solo la suya |
| `PUT /api/assignments/{id}/grades/{studentId}` | `assignments:update` | califica al estudiante, haya entregado o no; la nota nace en borrador |
| `POST /api/assignments/{id}/grades/$return` | `assignments:update` | devuelve las notas de `{"studentIds"}` y cierra sus entregas |
| `GET /api/courses/{id}/grading-scheme` | `courses:read` | cómo se calcula la nota final del curso |
| `PUT /api/courses/{id}/grading-scheme` | `courses:update` | reemplaza el método, la aprobatoria y las categorías |
| `GET /api/courses/{id}/gradebook` | `courses:read` | el registro de notas: todo el curso al staff, su fila al estudiante |

Los verbos que no encajan en el CRUD van como sub-recurso con `$`
(`POST /api/teachers/{id}/$disable`). Así el sustantivo sigue siendo el recurso y no
hace falta inventar rutas como `/teachers/{id}/deactivation` ni meter un `PATCH` con
un cuerpo que solo lleva un booleano. El `$` es un carácter normal en una ruta: ni
Spring ni los navegadores lo tratan distinto, pero al leer el log se distingue de un
golpe una operación de un identificador.

Rutas públicas: `/auth/login`, `/actuator/health`, `/actuator/info`,
`/v3/api-docs/**`, `/swagger-ui/**`. Todo lo demás exige token.

### Roles y permisos

El catálogo vive en código: `security/Permission` enumera los permisos y
`security/Role` asigna a cada rol los suyos. Una persona tiene un rol por cada perfil
activo, y sus permisos son la unión de los de esos roles.

| Rol | Permisos | Administra a |
|---|---|---|
| `SUPER_ADMIN` | `administrators:`, `teachers:`, `students:`, `courses:`, `enrollments:read`, `enrollments:update` y `assignments:` | `ADMIN`, `TEACHER`, `STUDENT` |
| `ADMIN` | `teachers:`, `students:`, `courses:`, `enrollments:read`, `enrollments:update` y `assignments:` | `TEACHER`, `STUDENT` |
| `TEACHER` | `courses:`, `enrollments:read`, `enrollments:update` y `assignments:` | — |
| `STUDENT` | `courses:read`, `enrollments:create`, `assignments:read` y `submissions:create` | — |

`Role.manageableRoles()` es la única fuente de la última columna, y de ella salen
dos reglas: un admin no puede tocar a otro admin, y nadie —tampoco el superadmin—
crea un `SUPER_ADMIN` por API. El listado de `/api/administrators` devuelve solo los
roles que el solicitante administra, así que el superadmin no aparece para nadie.

No hay tabla de roles: el rol de una persona *es* tener el perfil correspondiente
activo. Cada módulo de persona publica un `ProfileProvider` (`security/ProfileProvider`)
que, si esa persona tiene su perfil activo, devuelve el rol que le concede junto al id de
ese perfil, y `PersonProfiles` los agrega. Así el módulo de cuentas no necesita conocer a
los de docentes o administradores, y no existe un sitio donde el rol guardado pueda
divergir del perfil real.

### El primer superadmin

`V2__create_users.sql` siembra el usuario `superadmin` con la contraseña
`Superadmin.2026`. Ese hash está versionado en el repositorio: **cambia la
contraseña en el primer arranque de cada entorno**, desde
`POST /api/me/$changePassword`.

Su persona y su perfil los crea `V6__migrate_accounts_to_persons.sql` con datos
marcados `Pendiente` y un documento `PEND...` que no pasa la validación de la
aplicación: es deliberado, obliga a corregirlos desde `PUT /api/me` antes de poder
volver a guardar. Nadie administra a un `SUPER_ADMIN`, así que `/api/me` es el único
camino para arreglarlos.

### Registro de docentes

`POST /api/teachers` recibe `{"person": {...}, "credentials": {...}}` y, en una sola
transacción, resuelve la persona por su documento, se asegura de que tenga cuenta y
crea el perfil de docente.

Resolver la persona es lo que evita duplicarla: si el documento ya existe se reutiliza
esa persona **con los datos que ya tenía** y su cuenta, y `credentials` sobra
—enviarlo responde `USR_ACCOUNT_ALREADY_EXISTS`—; si no existe, `credentials` es
obligatorio (`USR_CREDENTIALS_REQUIRED`). Por eso el front llama antes a
`GET /api/persons/$byDocument`, que además le dice qué es ya esa persona y le ahorra
pedir un usuario y una contraseña que no hacen falta.

Registrar dos veces al mismo docente responde `TCH_ALREADY_REGISTERED`. Si algo se
rechaza no queda ficha a medias.

`teachers.person_id` apunta a `persons.id`: el módulo guarda el identificador, no la
entidad `Person`, que vive en otro módulo.

### Activar y desactivar

Se desactiva el **perfil**, no la cuenta: `POST /api/teachers/{id}/$disable` apaga al
docente y deja intactos sus demás perfiles, así que quien además sea estudiante sigue
entrando como estudiante. La cuenta no tiene un interruptor propio ni le hace falta:
**sirve mientras quede algún perfil activo**, y quien se queda sin ninguno no pasa del
login (`USR_INACTIVE_ACCOUNT`).

El efecto es inmediato aunque el token siga vivo: cada operación recalcula los roles
del actor contra la base (`UserService.actor`), de modo que un token emitido antes de
la baja deja de servir en el acto.

Quién puede apagar a quién sale otra vez de `Role.manageableRoles()`: un admin no
desactiva a otro admin y nadie desactiva al superadmin. De ahí sale gratis que nadie
pueda desactivarse a sí mismo, porque ningún rol se administra a sí mismo.

### Cambiar la contraseña de un docente

`POST /api/teachers/{id}/$changePassword` recibe `{"password"}` y la guarda cifrada en
la cuenta del docente; responde 204 porque ningún dato visible del docente cambia. No
pide la contraseña actual: la cambia quien administra al docente, no él mismo. Las
reglas vuelven a salir de `Role.manageableRoles()`, así que un admin no puede cambiar
la contraseña de otro admin ni la del superadmin.

Una contraseña que no eligió la persona —la inicial al crear la cuenta, que suele ser su
documento, o la que le pone quien la administra— deja la cuenta con
`mustChangePassword`. El login y `GET /me` lo devuelven, y el front no deja pasar de la
pantalla de cambio hasta que la persona elige una propia por `/me/$changePassword` o por
el enlace de recuperación; la nueva no puede ser igual a la actual
(`USR_PASSWORD_UNCHANGED`). Lo aplica el front, no el backend: el token sigue sirviendo
para la API, porque quien tuviera la contraseña podría cambiarla igual. Las cuentas
anteriores a `V19` no quedaron marcadas.

### Cursos, unidades y material

El curso es un agregado: sus **unidades** (las semanas o temas en que se divide) y el
**material** de cada unidad no existen fuera de él. Por eso viven en un solo módulo,
`course/`, con un único catálogo de errores (`CRS`) y un único permiso de
escritura, `courses:update`, que cubre también unidades y material.

Dentro, el módulo se ordena por agregado: el curso en la raíz, las unidades y su
material en `unit/`, la matrícula en `enrollment/`. El catálogo de errores no se parte
—`course/exception/` los tiene todos— porque es una sola lista.

Quién puede tocar un curso lo decide **el docente titular**, no el rol suelto
(`CourseAccess`):

- quien administra docentes —admin y superadmin— alcanza cualquier curso;
- un docente alcanza los cursos en los que él es el titular, y ningún otro;
- todo lo demás responde `CRS_OUT_OF_REACH`.

Esa misma regla resuelve el listado (un docente solo ve los suyos, y pedir
`?teacherId=` de otro es un 403), la creación (el `teacherId` es opcional: si falta, el
titular es el propio docente que crea el curso, y un admin que no enseña tiene que
nombrarlo — `CRS_TEACHER_REQUIRED`) y el traspaso de un curso a otro docente, que solo
puede hacer quien alcanza a los dos. El titular siempre debe ser un perfil de docente
activo (`TCH_INACTIVE`).

Un curso no se borra: se archiva (`POST /api/courses/{id}/$archive`), y así no se pierde
el historial de quien pasó por él. Un curso archivado se sigue leyendo, pero rechaza
cualquier escritura —suya, de sus unidades o de su material— con `CRS_ARCHIVED` hasta
que se reactive.

Las unidades llevan un `position` dentro del curso. Una unidad nueva se añade al final;
el orden se cambia de una vez con `POST /api/courses/{id}/units/$reorder`, que recibe
todos los ids del curso en el orden deseado y reescribe las posiciones como 1..n. Si la
lista se deja alguna unidad fuera o repite una, responde `CRS_INVALID_UNIT_ORDER` y no
toca nada. La unicidad de `(course_id, position)` en la base es `DEFERRABLE INITIALLY
DEFERRED` justo por esto: el reordenado pasa por estados intermedios con posiciones
repetidas y solo tiene que cuadrar al hacer commit.

El material es de tipo `PDF`, `VIDEO`, `PPT`, `DOC` o `LINK`, y de ahí sale de dónde
viene: un `LINK` trae `externalUrl` (`CRS_MATERIAL_NEEDS_URL` si falta) y cualquier otro
trae `storageKey` (`CRS_MATERIAL_NEEDS_FILE`), nunca los dos. `publishedAt` permite
programar una publicación (si no viene, es ahora) y `visible` la controla a mano; el
estudiante solo ve, y solo descarga, lo que ya está publicado y visible.

**Los archivos no pasan por la API.** El navegador los sube y los descarga directamente
del almacenamiento (S3 o compatible) con enlaces firmados que reparte el backend, que es
quien decide la clave y quién tiene acceso. Subir un archivo son tres pasos:

1. `POST /api/units/{id}/materials/$upload` con `{"type", "fileName", "contentType",
   "size"}`. Responde `{"storageKey", "uploadUrl", "method": "PUT", "headers",
   "expiresAt"}`.
2. El navegador hace `PUT` del archivo a `uploadUrl` **con las cabeceras de `headers`**.
   Van firmadas: si el tamaño o el tipo no coinciden con lo declarado, el almacenamiento
   rechaza la subida. `content-length` lo pone el navegador solo.
3. `POST /api/units/{id}/materials` (o `PUT /api/materials/{id}`) con esa `storageKey`.
   Aquí el backend comprueba que la clave es de ese curso y nadie más la usa
   (`CRS_FOREIGN_FILE`), que el archivo llegó (`CRS_FILE_NOT_UPLOADED`) y que su tamaño y
   tipo son los del material; solo entonces lo da por suyo.

| Tipo | Archivos que admite | Tamaño máximo |
|---|---|---|
| `PDF` | `application/pdf` | 50 MB |
| `DOC` | Word (`.doc`, `.docx`) | 50 MB |
| `PPT` | PowerPoint (`.ppt`, `.pptx`) | 50 MB |
| `VIDEO` | `video/mp4`, `video/webm` | 500 MB |

Fuera de eso responde `CRS_FILE_TYPE_NOT_ALLOWED` o `CRS_FILE_TOO_LARGE`, ya en el paso 1.
Para vídeos largos conviene un `LINK` a una plataforma de vídeo: lo que más cuesta del
almacenamiento es cada descarga, no guardar el archivo.

Descargar es `GET /api/materials/{id}/$download`, que responde `{"url", "expiresAt"}` con
un enlace de pocos minutos. PDF y vídeo se abren en el navegador; Word y PowerPoint se
descargan. Un `LINK` no tiene archivo (`CRS_MATERIAL_WITHOUT_FILE`), y a un estudiante un
material aún no publicado le responde `CRS_MATERIAL_NOT_FOUND`, como si no existiera.

Una subida que nunca se confirma se borra sola al día siguiente (la marca la etiqueta
`status=pending`), y el archivo de un material borrado o reemplazado se borra en cuanto la
transacción hace commit.

**La invitación viaja de las dos formas.** Al crear el curso se genera un código libre
de ocho caracteres (sin `I`, `O`, `0` ni `1`: se dicta en voz alta y se teclea a mano) y
la respuesta lo devuelve junto al enlace que lo lleva dentro:

```json
"invitation": {
  "code": "ABCD2345",
  "url": "http://localhost:5173/join/ABCD2345"
}
```

El enlace **no se guarda**: el código es el dato y la URL se compone al responder, con
la base de `COURSE_INVITATION_BASE_URL` —que apunta al frontend, no a la API—, así que
cambiar de dominio no obliga a tocar ninguna fila. Las dos formas terminan en el mismo
sitio: quien pega el código a mano y quien abre el enlace acaban mandando el mismo
código.

La invitación solo se le enseña al **staff** del curso —su docente titular y los
administradores—: un estudiante que lee su curso recibe la misma ficha sin el bloque
`invitation`, para que repartir el acceso siga siendo decisión del docente.

### Cómo se identifican los estudiantes

`GET`/`PUT /api/settings` guardan una configuración global —una sola fila, garantizada
por la base— con dos cosas: de dónde sale el usuario de un estudiante nuevo
(`DOCUMENT_NUMBER`, `EMAIL` o `MANUAL`) y si el enlace de invitación admite
inscripciones. Con `DOCUMENT_NUMBER`, que es el valor por defecto, dar de alta a un
estudiante sin escribirle credenciales le deja **el número de documento como usuario y
como contraseña inicial**. Un administrador puede seguir tecleando otras si quiere: las
explícitas siempre ganan.

### Inscribirse por el enlace

El docente comparte la URL de la invitación y el estudiante se da de alta solo, sin
cuenta previa: documento, apellidos, nombres, fecha de nacimiento, sexo, correo y lugar
de trabajo. Queda matriculado según la política del curso y recibe un correo con su
usuario.

`selfRegistrationEnabled` **nace apagado**. Mientras lo esté, `$register` responde
`CRS_SELF_REGISTRATION_CLOSED`: el enlace no debe quedar abierto desde el día del
despliegue, porque el código de invitación no caduca ni se puede rotar, y quien lo tenga
podría dar de alta cuentas sin límite.

Solo acepta DNI y carné de extranjería; con pasaporte hay que pedir la cuenta al centro.
Si el documento o el correo ya están registrados responde `CRS_ACCOUNT_ALREADY_REGISTERED`
—el mismo código para los dos, para no convertir una ruta pública en un oráculo de quién
tiene cuenta aquí—. El front no lo muestra como un error: lleva al login con la vuelta
al enlace ya puesta, y desde ahí a recuperar la contraseña si hace falta.

### Intentos de login

`LOGIN_MAX_FAILURES` contraseñas erróneas dentro de `LOGIN_FAILURE_WINDOW` bloquean ese
usuario durante `LOGIN_LOCKOUT`: el login responde 429 `USR_TOO_MANY_LOGIN_ATTEMPTS`
aunque la contraseña sea la buena. Se cuenta por **usuario**, exista o no, para que el
bloqueo no delate quién tiene cuenta; entrar bien o cambiar la contraseña por el enlace
de recuperación borra el contador.

Contar por usuario y no por IP es a propósito: el usuario suele ser el documento y la
contraseña inicial también, así que lo que hay que frenar es probar muchas contraseñas
contra una cuenta, venga de donde venga. El precio es que un tercero puede bloquear una
cuenta ajena unos minutos; la recuperación por correo la desbloquea. El contador vive en
memoria: con más de una instancia cada una lleva el suyo y habría que moverlo a la base
o a Redis.

### Recuperar la contraseña

`POST /api/auth/password-reset` busca la cuenta por usuario y, si no, por correo, y
encola un correo con el usuario y un enlace de un solo uso a `PASSWORD_RESET_URL`
(`?token=...`). **Responde 202 siempre**, exista o no la cuenta, por la misma razón que
el código único del auto-registro. Sin correo en la persona no hay a dónde mandarlo y
no pasa nada: el centro sigue pudiendo cambiarla con `$changePassword`.

La tabla `password_resets` guarda el hash SHA-256 del token, nunca el token. El enlace
caduca a los `PASSWORD_RESET_LIFETIME`, sirve una vez, y usarlo cierra también los demás
enlaces abiertos de esa cuenta. Entre dos enlaces a la misma cuenta pasan al menos
`PASSWORD_RESET_COOLDOWN`: es lo que impide usar la ruta pública para inundar el correo
de alguien, a falta de una limitación de tasa general.

Dos límites asumidos: el cuerpo del correo, con el enlace dentro, se queda en
`notifications` hasta que caduca; y cambiar la contraseña no revoca los JWT ya emitidos,
que siguen valiendo hasta su expiración.

### Notificaciones

Inscribirse encola un correo; no se envía dentro de la petición. La cola es la tabla
`notifications`, escrita en la misma transacción que la matrícula, y un proceso
programado la consume cada `NOTIFICATIONS_POLL_INTERVAL`. Sin `SPRING_MAIL_HOST` los
correos solo se escriben en el log, que es lo que pasa en `dev`.

### Estudiantes y matrícula

La cuenta del estudiante la abre un administrador (`POST /api/students`), igual que la
del docente: misma persona, mismo documento como clave natural, mismo perfil con su
propio `active`. Quien ya existe como docente y se matricula como estudiante reutiliza
su persona y su cuenta.

**Inscribirse es cosa del estudiante**; a quién deja entrar lo decide el curso. Cada
curso lleva una `enrollmentPolicy`, que el docente elige al crearlo y puede cambiar
después:

| `enrollmentPolicy` | Qué pasa al usar el código |
|---|---|
| `AUTOMATIC` | la matrícula nace `ACTIVE`: el estudiante entra en el acto |
| `ON_REQUEST` | la matrícula nace `PENDING` y espera a que el docente la resuelva |

`POST /api/courses/$join` recibe `{"code"}` —el mismo código que lleva dentro el enlace
de invitación, así que da igual si lo tecleó o si abrió el enlace— y devuelve la
matrícula con su estado. El docente ve las solicitudes en
`GET /api/courses/{id}/enrollments?status=PENDING` y las resuelve con `$accept` o
`$reject`; `$withdraw` saca del curso a alguien ya matriculado.

Los cuatro estados son `PENDING`, `ACTIVE`, `REJECTED` y `WITHDRAWN`, y solo hay **una
fila por (curso, estudiante)**: a quien fue rechazado o se retiró y vuelve a pedir
entrar se le reutiliza la fila en vez de acumularle historial. Volver a pedirlo estando
dentro responde `CRS_ALREADY_ENROLLED`, y hacerlo con una solicitud viva,
`CRS_ENROLLMENT_PENDING`. Un curso archivado no admite matrículas nuevas ni permite
resolver las que tenga pendientes.

**Lo que ve un estudiante.** Su matrícula activa es lo que le abre el curso:

- `GET /api/courses` le devuelve los cursos en los que está `ACTIVE` —no los pendientes—,
  sin el bloque `invitation`;
- `GET /api/courses/{id}/units` le devuelve las unidades con **solo el material
  publicado**: aquí es donde por fin cuentan `visible` y `publishedAt`, que hasta ahora
  se guardaban sin filtrar nada. El docente sigue viendo todo, incluido lo programado
  para más adelante;
- `GET /api/me/enrollments` le devuelve sus matrículas con su estado, que es donde ve si
  su solicitud sigue pendiente o fue rechazada.

Todo lo demás del curso le está cerrado: escribir sigue siendo del titular y de quien
administra docentes.

### Tareas, entregas y calificaciones

Las tareas son el **primer módulo que no vive dentro de `course`**: `assignment`
tiene su propio catálogo (`ASG`) y le pregunta al de cursos lo que necesita saber, que es
poco —`UnitService.courseOf`, `CourseService.memberOf`, `CourseService.requireWritable` y
`CourseService.requireActiveStudent`—. `memberOf` es el que sostiene todo: responde **quién
eres en este curso** (staff o estudiante, y cuál), y de ahí sale sin repetir reglas que el
docente vea todas las entregas y el estudiante solo la suya.

La tarea cuelga de una unidad y lleva fecha límite, puntaje máximo, si admite entregas
tardías y, opcionalmente, la categoría del curso en la que cuenta. Entregar es
`POST /api/assignments/{id}/$submit` con un texto, adjuntos o las dos cosas, y el estado sale
solo de la fecha:

| Situación | Estado de la entrega |
|---|---|
| dentro de plazo | `SUBMITTED` |
| fuera de plazo y la tarea admite tardías | `LATE` |
| fuera de plazo y no las admite | se rechaza con `ASG_DEADLINE_PASSED` |

Hay **una entrega por (tarea, estudiante)**: volver a entregar reemplaza la que había, no
acumula intentos —hasta que se devuelve su nota, que es cuando se cierra
(`ASG_ALREADY_GRADED`)—. Una entrega sin archivo y sin texto no vale
(`ASG_EMPTY_SUBMISSION`). Una tarea que ya tiene entregas o notas no se borra
(`ASG_HAS_WORK`).

### Calificaciones y registro de notas

Funciona como en Google Classroom. El docente **califica a cualquier estudiante
matriculado**, haya entregado o no: `PUT /api/assignments/{id}/grades/{studentId}` con
`{"score", "feedback"}`. Una nota fuera de rango responde `GRB_SCORE_OUT_OF_RANGE` y
calificarse a uno mismo, `GRB_OWN_GRADE`. **La nota nace en borrador**: el estudiante no
la ve hasta que el docente la devuelve con `POST /api/assignments/{id}/grades/$return`
(`{"studentIds": [...]}`, varios a la vez). Mientras sea borrador, el alumno aún puede
reentregar.

Las notas viven en su propio módulo, `gradebook` (`GRB`), porque las pondrán también los
exámenes. Cada nota guarda el máximo con el que se calificó, así que cambiar el puntaje de
una tarea no altera las notas ya puestas.

**Cada curso define cómo se calcula su nota final** con `PUT /api/courses/{id}/grading-scheme`:

```json
{
  "method": "WEIGHTED",
  "passingScore": 13,
  "categories": [
    {"id": null, "name": "Tareas", "weight": 30},
    {"id": null, "name": "Exámenes", "weight": 50},
    {"id": null, "name": "Participación", "weight": 20}
  ]
}
```

- `WEIGHTED`: cada categoría promedia sus tareas por puntos y la final pondera esos
  promedios. Los pesos deben sumar 100 (`GRB_WEIGHTS_DO_NOT_ADD_UP`). Una categoría
  todavía sin notas cede su peso a las demás, y las tareas sin categoría no cuentan.
- `TOTAL_POINTS`: la suma de lo obtenido entre la suma de lo posible. Las categorías solo
  agrupan.

El PUT **reemplaza el esquema entero**: las categorías con `id` se conservan (y pueden
renombrarse), las que faltan se borran —sus tareas quedan sin categoría— y las que vienen
sin `id` se crean. El orden de la lista es el orden de las categorías. Un curso que nunca
definió su esquema usa total de puntos y aprueba con 13.

`GET /api/courses/{id}/gradebook` devuelve el esquema, las columnas (las tareas, por fecha
límite) y una fila por estudiante activo, en orden alfabético. Cada fila trae sus notas, el
promedio de cada categoría y la nota final, **siempre en escala 0–20**:

| Campo | Qué es |
|---|---|
| `finalGrade.score` | la nota con dos decimales (`13.20`) |
| `finalGrade.roundedScore` | el entero de acta, redondeando `.5` hacia arriba desde esos dos decimales |
| `finalGrade.passed` | si el entero llega a la aprobatoria del curso |

El estudiante recibe solo su fila y solo con las notas devueltas; el staff ve también los
borradores, así que la final que ve el docente puede adelantarse a la del alumno. Sin
ninguna nota, `finalGrade` es `null`.

### Adjuntos de tareas y entregas

El docente adjunta material a la tarea y el estudiante adjunta su trabajo a la entrega: hasta
**10 adjuntos**, cada uno un **archivo** (`FILE`) o un **enlace** (`LINK`, `http` o `https`).
Se aceptan PDF, Word, Excel, PowerPoint, OpenDocument, texto, imágenes JPG/PNG/WEBP y ZIP,
de hasta **50 MB** por archivo.

Un archivo se sube como el material: se pide la subida firmada
(`POST /api/units/{id}/assignments/$upload` el docente, `POST /api/assignments/{id}/$upload`
el estudiante, con `{"fileName", "contentType", "size"}`), se sube con `PUT` a la URL que
devuelve y se manda su `storageKey` dentro de `attachments` al guardar la tarea o la entrega:

```json
{
  "text": "Adjunto mi informe y el video de la práctica",
  "attachments": [
    {"kind": "FILE", "title": "Informe.pdf", "storageKey": "courses/…/submissions/…/informe.pdf"},
    {"kind": "LINK", "title": "Video de la práctica", "externalUrl": "https://youtu.be/…"}
  ]
}
```

La lista **reemplaza** los adjuntos: lo que no viene se borra (y su archivo, tras el commit),
un archivo que ya estaba se conserva por su `storageKey` y el orden de la lista es el orden
de los adjuntos. La clave la genera el backend bajo la carpeta del curso —y, en una entrega,
la del estudiante—, así que un archivo subido para otro curso u otra persona responde
`ASG_FOREIGN_FILE`. Los adjuntos de la tarea los descarga cualquiera del curso; los de una
entrega, el staff y el propio estudiante (`ASG_SUBMISSION_OUT_OF_REACH` para el resto).

## Almacenamiento en AWS

En local no hay que hacer nada: el MinIO de Docker ya trae el bucket y su regla. En AWS
hay que preparar cuatro cosas una vez:

1. **Un bucket privado**, con *Block Public Access* activado entero. Nadie lee del bucket
   sin un enlace firmado.
2. **CORS**, para que el navegador pueda subir y descargar desde el dominio del front:

   ```json
   [{
     "AllowedOrigins": ["https://aula.example.com"],
     "AllowedMethods": ["GET", "PUT"],
     "AllowedHeaders": ["content-type", "x-amz-tagging"],
     "ExposeHeaders": ["ETag"],
     "MaxAgeSeconds": 3600
   }]
   ```

3. **Una regla de ciclo de vida** que borre los objetos con la etiqueta
   `status=pending` a 1 día: son subidas que nunca se confirmaron.
4. **Un rol IAM para la instancia** (o, fuera de AWS, un usuario IAM solo para la
   aplicación), con esta política y nada más:

   ```json
   {
     "Version": "2012-10-17",
     "Statement": [{
       "Effect": "Allow",
       "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject",
                  "s3:PutObjectTagging", "s3:DeleteObjectTagging"],
       "Resource": "arn:aws:s3:::<bucket>/*"
     }, {
       "Effect": "Allow",
       "Action": "s3:ListBucket",
       "Resource": "arn:aws:s3:::<bucket>"
     }]
   }
   ```

   `s3:ListBucket` no se usa para listar nada: sin él, S3 responde 403 en vez de 404 al
   preguntar por un objeto que no existe, y confirmar un material cuyo archivo nunca se
   subió daría un 500 en lugar de `CRS_FILE_NOT_UPLOADED`.

Luego se exportan `STORAGE_ENDPOINT=` (vacío: apunta a AWS), `STORAGE_REGION`,
`STORAGE_BUCKET` y `STORAGE_PATH_STYLE=false`. Con un rol, `STORAGE_ACCESS_KEY=` y
`STORAGE_SECRET_KEY=` se dejan **definidas y vacías**: la aplicación toma entonces las
credenciales temporales del rol y no hay ninguna clave guardada. Con un usuario IAM se
ponen sus claves ahí. Cambiar a otro proveedor compatible con S3 (Cloudflare R2,
Backblaze B2) es cambiar esas variables, no el código.

El despliegue completo de una demo (EC2, RDS, S3, CloudFront y SES) está explicado paso a
paso en [`docs/deploy-aws`](docs/deploy-aws/README.md).

Cómo se numeran las versiones de este repo y del front, y cómo se publica una beta con
Git Flow: [`docs/versioning.md`](docs/versioning.md).

## Errores

Todos los errores comparten el mismo cuerpo:

```json
{
  "timestamp": "2026-09-18T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "GEN_VALIDATION_FAILED",
  "message": "La petición contiene campos inválidos",
  "path": "/api/teachers",
  "traceId": "a1b2c3d4",
  "errors": [
    { "field": "firstName", "message": "los nombres son obligatorios" }
  ]
}
```

`code` es el identificador estable del error y es lo que debe mirar el front para
decidir qué enseña; `message` es un texto en español pensado para leerse tal cual
si el front no tiene nada mejor que poner. El `traceId` se escribe también en el log
del servidor, así que localiza el error exacto sin exponer detalles internos. Los
errores del propio framework (404, 405, cuerpo ilegible) usan el mismo formato y
conservan su código: nunca se convierten en 500.

### El catálogo de errores

Cada error es una clase de excepción con nombre que extiende `ApiException` y trae su
código ya puesto. `ApiException` es abstracta, así que no hay forma de lanzar un error
anónimo: hay que darle nombre y, con el nombre, viene el código y el status.

```java
throw new UsernameTakenException();
throw new TeacherNotFoundException(teacherId);
```

Las excepciones viven en el paquete `exception/` de su módulo, al lado de `dto/`. Detrás
de cada una hay una constante de un `enum` que implementa `ErrorCode` y declara su status
HTTP y su mensaje; ese enum es el catálogo. Hay uno global y uno por módulo:

| Catálogo | Prefijo | Dónde |
|---|---|---|
| `CommonError` | `GEN` | `common/exception` |
| `PersonError` | `PRS` | `person/exception` |
| `UserError` | `USR` | `user/exception` |
| `TeacherError` | `TCH` | `teacher/exception` |
| `AdministratorError` | `ADM` | `administrator/exception` |
| `StudentError` | `STD` | `student/exception` |
| `CourseError` | `CRS` | `course/exception` |
| `AssignmentError` | `ASG` | `assignment/exception` |

El código no se escribe a mano: `ErrorCode.code()` lo compone como
`prefijo + "_" + nombre de la constante`, de modo que `UserError.USERNAME_TAKEN` es
`USR_USERNAME_TAKEN`. No hay contador que mantener y dos errores no pueden chocar sin
que choquen antes sus nombres dentro del mismo enum. `ErrorCatalogueTest` recorre por
classpath todas las implementaciones de `ErrorCode` —las de hoy y las que se añadan— y
falla si dos comparten código o si dos catálogos comparten prefijo.

Son dos piezas por error a propósito: el enum es la lista legible de todo lo que un
módulo puede responder, y lo que hace verificable que ningún código se repita; la clase es
lo que se lanza, y es la que hace que el `throw` se lea sin ir a buscar nada.

Los argumentos del constructor rellenan los `%s` del mensaje del catálogo, como el id en
`TeacherNotFoundException`. Un módulo puede lanzar la excepción de otro cuando el error es
de verdad del otro: registrar un docente con un usuario repetido responde
`USR_USERNAME_TAKEN` aunque la ruta sea `/api/teachers`, porque el conflicto es de la
cuenta. Por eso el front debe mirar el `code` y no la ruta.

Solo dos sitios escriben un `ApiError`, y ambos parten de un `ErrorCode`:
`GlobalExceptionHandler` para todo lo que pasa por el controlador, y `ApiErrorWriter`
para el 401 y el 403 que Spring Security responde antes de llegar al advice.

## Tests

```bash
./gradlew test
```

Los unitarios (`JwtServiceTest`, `RoleTest`, `PersonProfilesTest`, `PersonServiceTest`,
`UserServiceTest`, `AuthenticationServiceTest`, `TeacherServiceTest`,
`AdministratorServiceTest`) y las rodajas web con MockMvc y tokens reales
(`SecurityConfigTest`, `GlobalExceptionHandlerTest`, `AuthControllerTest`,
`MeControllerTest`, `PersonControllerTest`, `TeacherControllerTest`,
`AdministratorControllerTest`) corren siempre: no necesitan base de datos.

`AulaVirtualApplicationTests` levanta el contexto completo y necesita Postgres
(base `aula_virtual_test`). Si no hay base accesible, **se omite en lugar de
fallar**, para que el build funcione en una máquina recién clonada. Con
`docker compose up -d` vuelve a ejecutarse de verdad.

## Perfiles

| Perfil | Uso |
|---|---|
| `dev` | SQL en el log, actuator con metrics, secreto JWT de desarrollo. Lo activa `bootRun` |
| `test` | usado por los tests de integración |
| `prod` | logging mínimo, `flyway.clean` y `baseline-on-migrate` deshabilitados |
