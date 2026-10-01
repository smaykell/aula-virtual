# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Comandos

```bash
docker compose up -d                               # Postgres 18 + MinIO (S3 local)
./gradlew build                                    # compila + tests
./gradlew test                                     # solo tests
./gradlew test --tests '*JwtServiceTest'           # una clase
./gradlew test --tests '*JwtServiceTest.rejects_an_expired_token'   # un test
./gradlew bootRun                                  # arranca en perfil dev, http://localhost:8080/api
./gradlew bootJar
```

En PowerShell (shell por defecto de este entorno) el wrapper es `.\gradlew.bat`.

`bootRun` activa el perfil `dev` desde `build.gradle`; **no hay perfil por defecto**. Un
`java -jar` sin `--spring.profiles.active` no arranca si falta `JWT_SECRET`, que es lo que
se quiere: el secreto de desarrollo nunca puede colarse en un despliegue real.

No hay `.env`: la configuración ajustable son variables del entorno del proceso, todas
con un valor por defecto para local. El catálogo está en el README.

## Ramas: Git Flow

Desde septiembre de 2026 el repo sigue **Git Flow**. Dos ramas permanentes:

- `main` — solo lo publicado. Cada commit de `main` es una versión y lleva su tag
  (`v1.2.0`). Nunca se trabaja ni se commitea directamente en ella.
- `develop` — la integración. Aquí confluye todo lo terminado para la próxima versión.

Y tres tipos de rama temporal, que se borran al cerrarse:

- `feature/<nombre>` — sale de `develop` y vuelve a `develop`. Todo trabajo nuevo, por
  pequeño que sea, empieza aquí; el nombre en inglés y en kebab-case
  (`feature/exam-module`).
- `release/<versión>` — sale de `develop` cuando lo que hay está listo para publicar.
  Solo admite ajustes de versión y correcciones; se fusiona en `main` (con tag) **y** de
  vuelta en `develop`.
- `hotfix/<versión>` — sale de `main` para arreglar algo publicado; se fusiona en `main`
  (con tag) **y** en `develop`, para que el arreglo no se pierda en la siguiente versión.

Las fusiones hacia `develop` y `main` son `--no-ff`, para que cada feature, release o
hotfix quede visible como una unidad en el historial. Si al empezar una tarea se está en
`develop` o `main`, lo primero es crear la rama que toque.

## Convenciones de código

**Inglés.** Clases, métodos, variables, paquetes de módulo, nombres de test y nombres de
tabla/columna se escriben en inglés: `Course`, `CourseService`, `CourseRepository`,
`findById`, `V2__create_courses.sql`. Los mensajes de error dirigidos al usuario final
siguen en español, porque los lee una persona y no un programador. Los tests usan
`snake_case` descriptivo en inglés (`rejects_an_expired_token`).

**Sin comentarios.** Nada de javadoc decorativo ni comentarios que repitan lo que hace la
línea siguiente. Si algo pide explicación, se arregla con un nombre mejor, un método más
pequeño o una constante con nombre. La única excepción es el *porqué* que no cabe en un
nombre — una decisión no obvia o una trampa de la librería — y ahí va una línea, no un
párrafo. Hoy quedan exactamente dos en todo `src/`; que sigan siendo pocas.

**Clean code, siempre.** Métodos cortos y con un solo nivel de abstracción, sin parámetros
booleanos, sin clases que hagan dos cosas, sin duplicación tolerada "por ahora".

## Versiones y trampas de Spring Boot 4 / Jackson 3

Spring Boot 4.1.1 (Spring Framework 7.0.9, Hibernate 7.4.5) sobre Java 21, con cambios de
paquete respecto a Boot 3 que rompen el autocompletado y las respuestas de memoria:

- Starter web: `spring-boot-starter-webmvc` (no `-web`); tests: `spring-boot-starter-webmvc-test`.
- `@WebMvcTest` vive en `org.springframework.boot.webmvc.test.autoconfigure`.
- `JacksonAutoConfiguration` vive en `org.springframework.boot.jackson.autoconfigure`.
- Jackson 3: el mapper es `tools.jackson.databind.json.JsonMapper` (se inyecta como bean),
  mientras que las anotaciones siguen en `com.fasterxml.jackson.annotation`.

Ante cualquier duda de API de Spring o de una librería, consultar context7 antes de
escribir código.

## Arquitectura

Paquete raíz `io.github.smaykell.aulavirtual`. De él cuelgan tres zonas transversales
(`config`, `security`, `common`) y, al mismo nivel, un paquete por módulo de dominio.
**No hay un `modules/` intermedio**: el paquete raíz ya es la aplicación y esa carpeta no
distinguía nada que el nombre del módulo no dijera ya.

**Persona, perfiles y cuenta son tres cosas distintas**, y esa separación es el eje de
todo lo demás:

- `person` — la identidad (`persons`): tipo y número de documento, nombres, apellidos,
  fecha de nacimiento y sexo. El documento es la **clave natural**: es lo único que
  permite saber que el docente que se está registrando ya existe como estudiante.
  `PersonService.resolveOrCreate` reutiliza a quien ya está, sin sobrescribir sus datos.
- `teacher`, `student`, `administrator` — los perfiles. Cada uno guarda `person_id`, su
  propio `active` y **sus atributos propios**; los que le sobren a otro perfil no le
  estorban. Una misma persona puede tener varios. Hoy `Teacher` y `Student` son casi
  idénticos, y **eso es transitorio**: cada uno va a recibir campos exclusivos, así que
  la duplicación es deliberada y **no se unifican** en una entidad ni en un servicio
  genérico de perfiles.
- `user` — la cuenta (`users`): username, contraseña, `person_id`. Una por persona,
  1:1. No guarda rol ni `active`.

Una persona sin perfiles no existe en la práctica: `resolveOrCreate` solo se llama
desde el alta de un perfil, dentro de la misma transacción.

**Vertical slice.** Cada módulo de dominio es un paquete autocontenido
(`course/`: entidad, repositorio, servicio, controlador, `dto/`). Un módulo solo
depende de `common` y `config`; si dos módulos necesitan hablarse lo hacen a través del
*service* del otro, nunca de su repositorio ni de sus entidades.

**Esa regla ya no es solo prosa**: `ModuleBoundariesTest` lee las fuentes de `src/main` y
falla si un módulo importa el repositorio o la entidad de otro. Va acompañado de tres
comprobaciones contra sí mismo —que el escaneo encuentra los 25 repositorios, las 25
entidades y que no confunde `BaseEntity` con una— porque un test que busca en ficheros
pasa igual de verde si deja de mirar donde debe. No arregla nada hoy: impide retroceder
mañana. Los sub-paquetes de un módulo son el mismo módulo, así que `course/enrollment`
puede usar `CourseRepository`.

**El esquema lo manda Flyway.** `spring.jpa.hibernate.ddl-auto=validate`, así que una
entidad nueva sin su migración correspondiente hace fallar el arranque (y el test de
contexto). Cada módulo aporta su `V<n>__create_<name>.sql` en
`src/main/resources/db/migration`. Las migraciones ya aplicadas no se editan jamás: los
cambios van en una migración nueva. `V1__init.sql` es solo comentarios con las
convenciones del esquema (snake_case, tablas en plural, `id UUID PK`,
`created_at`/`updated_at` `TIMESTAMPTZ NOT NULL`) y se deja intacto pese a la regla de "sin
comentarios", porque tocarlo rompería el checksum en las bases que ya lo aplicaron.
`baseline-on-migrate` solo está activo en `dev` y `test`: en prod debe fallar, no
baselinear en silencio.

**Entidades y DTOs.** Toda entidad extiende `BaseEntity` (UUID generado por Hibernate antes
del INSERT, auditoría vía `@EnableJpaAuditing`). `equals`/`hashCode` usan
`Hibernate.getClass()` y `getId()`, no el campo ni `getClass()`: si no, una entidad y su
proxy lazy de la misma fila no se reconocen entre sí dentro de un `HashSet`. Los DTOs son
`record` y las entidades nunca se exponen. Los listados paginados devuelven `PageResponse`
en vez de `Page` de Spring Data, para que el JSON del frontend no dependa de internos de
la librería.

**Contrato de error único.** Todas las respuestas de error son un `ApiError`, que genera su
propio `traceId`; ese `traceId` se escribe siempre en el log del servidor. Hay dos
emisores, y solo dos:

- `GlobalExceptionHandler` (`common/exception`) **extiende `ResponseEntityExceptionHandler`**.
  Eso es lo que impide que un `@ExceptionHandler(Exception.class)` se trague los 4xx del
  framework y los convierta en 500: los 404, 405, 400 por parámetro ausente, etc. pasan por
  `handleExceptionInternal`, que los traduce a `ApiError` conservando su status. **No añadir
  un `@ExceptionHandler` para tipos que la clase base ya cubre** — el resolver lanza
  "Ambiguous @ExceptionHandler"; hay que sobrescribir el método `handle*` correspondiente.
- `ApiErrorWriter` (`common/web`), que usan `RestAuthenticationEntryPoint` (401) y
  `RestAccessDeniedHandler` (403). Existe porque Spring Security responde en el filtro,
  antes de llegar al `@RestControllerAdvice`.

`GlobalExceptionHandler.rethrowForSecurityFilterChain` relanza `AccessDeniedException` y
`AuthenticationException` a propósito, para que las resuelva `ExceptionTranslationFilter`
y no el catch-all. Sin ese relanzamiento, el 403/401 lo produciría el advice y el
`accessDeniedHandler` sería código muerto.

**Catálogo de errores.** Cada error es **una clase de excepción con nombre** que extiende
`ApiException` y trae su código ya puesto:

```java
throw new UsernameTakenException();
throw new TeacherNotFoundException(teacherId);
```

`ApiException` es **abstracta** a propósito: no se puede lanzar un error anónimo con un
status y un literal sueltos, hay que darle nombre. Las excepciones viven en el paquete
`exception/` de su módulo, al lado de `dto/`.

Detrás de cada una hay una constante de un `enum` que implementa `ErrorCode` y declara su
status HTTP y su mensaje. Hay un catálogo global, `CommonError` (`GEN`), y uno por módulo
— `PersonError` (`PRS`), `UserError` (`USR`), `TeacherError` (`TCH`),
`AdministratorError` (`ADM`) —; un módulo nuevo trae el suyo con su propio prefijo. Son dos piezas por error a propósito: el enum es la lista legible de todo
lo que un módulo puede responder y lo que hace verificable la unicidad; la clase es lo que
se lanza y lo que hace que el `throw` se lea solo.

`ErrorCode.code()` compone el código como `prefijo + "_" + name()`, así que no hay números
que asignar ni riesgo de repetir uno. `ErrorCatalogueTest` escanea el classpath buscando
implementaciones de `ErrorCode` y falla si dos comparten código o si dos catálogos
comparten prefijo: al añadir un módulo no hay que registrarlo en ningún sitio, pero sí
darle un prefijo libre.

Los argumentos del constructor rellenan los `%s` del mensaje del catálogo. Un módulo puede
lanzar la excepción de otro cuando el error es del otro — registrar un docente con un
usuario repetido responde `USR_USERNAME_TAKEN` desde `/api/teachers` —, que es justo por
lo que el front mira el `code` y no la ruta.

El mensaje de una `ApiException` se envía tal cual al cliente: escribirlo para un usuario
final, sin detalles internos; los detalles van al log.

**Seguridad.** Cadena *stateless*, sin CSRF, sin usuarios en memoria. El login vive en
`user` (`POST /auth/login`) y llama a `jwtService.issueToken(subject, authorities)`.
Las authorities viajan en el claim `roles` y son dos cosas a la vez: **cada** rol con
prefijo (`ROLE_TEACHER`) y, junto a ellos, la unión de los permisos de esos roles
(`teachers:read`), de modo que `hasRole(...)` y `hasAuthority(...)` funcionan sin
traducción. Quien es administrador y docente inicia sesión una vez y lleva los dos.
El login y `/me` devuelven en cambio los permisos **por rol** (`RoleAccess`), porque el
front deja elegir con qué rol se trabaja y filtra el menú con los permisos de ese rol.
Esa elección es solo de vista: el backend sigue autorizando con la unión, y lo que no
debe mezclarse (ser staff y alumno del mismo curso, calificarse) lo impiden reglas por
curso, no el rol elegido.

**Roles y permisos.** El catálogo vive en código, no en tablas: `security/Permission`
enumera los permisos y `security/Role` asigna a cada rol los suyos.

**El rol de una persona *es* tener el perfil correspondiente activo**, así que no hay
tabla de roles ni columna que pueda divergir del perfil real. Cada módulo de persona
publica un `security/ProfileProvider` que, si esa persona tiene su perfil activo,
devuelve un `security/Profile` — **el rol y el id del perfil que lo concede** —, y
`security/PersonProfiles` los agrega en un `Map<Role, UUID>`. El id viaja junto al rol
porque quien pregunta suele necesitar los dos, y así sale de una sola pasada: el
proveedor de administrador, por ejemplo, devuelve el rol que trae su fila
(`SUPER_ADMIN` o `ADMIN`), no una constante. Es lo que permite que el módulo de cuentas
no conozca a los de docentes o administradores: la dependencia va de los módulos hacia
`security`, nunca al revés. **Un módulo de persona nuevo solo tiene que publicar su
`ProfileProvider`**; no hay que registrarlo en ningún otro sitio.

Como una persona puede tener varios roles, lo que decide una autorización es el
`security/Actor` (persona, username y sus perfiles), no un rol suelto: su alcance es la
**unión** de lo que administra cada uno de sus roles. `Actor.profileId(Role)` responde
«qué id tengo como docente / como estudiante», así que **ningún módulo tiene que volver a
preguntárselo al módulo del perfil**: el actor ya viene resuelto de la base.

`Role.manageableRoles()` sigue siendo la **única** fuente de quién administra a quién —
el superadmin administra admins, docentes y estudiantes; un admin solo docentes y
estudiantes; nadie administra a un `SUPER_ADMIN`, que solo nace de la semilla de
`V2__create_users.sql`. Tampoco puede tener otros perfiles: `UserService.ensureAccount`, por donde
pasa el alta de todo perfil, se niega a prestar su cuenta (`USR_SUPER_ADMIN_EXCLUSIVE`). De ahí salen sin código extra las dos reglas del enunciado: entre
admins no se tocan y el listado solo muestra los roles que el solicitante administra.

Los literales de `@PreAuthorize` salen de `Permission.Name`, no de cadenas sueltas: así un
permiso mal escrito no compila. El catálogo de cada rol se escribe permiso a permiso y
**nunca** con `Permission.values()`: atajarlo regalaría a los admins cualquier permiso
futuro que solo debería tener el superadmin — hoy ya difieren, porque `administrators:*`
es solo del superadmin.

**Operaciones con `$`.** Lo que no es CRUD va como sub-recurso con `$`:
`POST /teachers/{id}/$disable`, `$enable`. El `$` no es especial para `PathPattern` ni
para Spring Security, pero deja el recurso en el sustantivo y hace evidente en el log
que ese segmento es un verbo y no un id.

`$disable` apaga **el perfil, no la cuenta**: dar de baja a un docente que además es
estudiante lo deja entrando como estudiante. La cuenta no tiene interruptor propio ni le
hace falta — **sirve mientras quede algún perfil activo** —, y por eso no existe ninguna
pantalla ni ningún endpoint para activar o desactivar cuentas.

Toda operación recalcula al actor desde la base (`UserService.actor`, que vuelve a
preguntar a los `ProfileProvider`) en vez de fiarse del token: es lo que hace que dar de baja
a alguien surta efecto de inmediato en lugar de esperar a que caduque su JWT.

**`/me` es la única vía de autoservicio** (`user/MeController`): ver mis datos,
cambiarlos y cambiar mi contraseña dando la actual. No pasa por `manageableRoles()`,
porque nadie se administra a sí mismo — y es lo único que permite al superadmin, a quien
nadie administra, corregir sus propios datos.

**`/me/*` es lo único que no lleva `@PreAuthorize`**, y es deliberado: `SecurityConfig`
ya exige token para todo, y el resto de la condición la pone el servicio. Vale también
para `/me/enrollments`, que vive en `course/enrollment/MyEnrollmentController` y es
solo para estudiantes: quien no lo sea recibe `CRS_STUDENT_REQUIRED` desde
`courseAccess.requireStudent`. Anotarlo con `hasRole('STUDENT')` empeoraría las dos
cosas — partiría la regla entre la anotación y el servicio, y cambiaría un código de
error que el front sabe leer por un 403 genérico. No hay ningún permiso que encaje:
`enrollments:read` es de admin y docente, justo de quien **no** usa este endpoint.
`/me/dashboard` (módulo `dashboard`) sigue la misma regla: devuelve la parte de estudiante
y la de docente según los perfiles del actor, y nada a quien no tiene ninguno de los dos.
Es el único módulo que solo compone: no tiene entidades ni repositorios, solo pide a
`CourseService`, `AssignmentService` y `GradebookService`.
Lo mismo vale para `/invitations/*`, por la razón opuesta: son **públicas**, y quien
todavía no tiene cuenta no tiene ninguna authority que exigirle. Así que al auditar la
superficie de la API, los `@PreAuthorize` la describen entera **salvo `/me/*` y
`/invitations/*`**.

Dos detalles fáciles de romper:

- Las rutas de `SecurityConfig.PUBLIC_PATHS` se escriben **sin** el context-path `/api`;
  Spring Security las evalúa relativas al contexto. Y se escriben **una a una**
  (`/invitations/*`, `/invitations/*/$register`), nunca con `/**`: un comodín abriría
  cualquier sub-ruta que el controlador gane mañana.
- `JwtAuthenticationFilter` nunca corta la cadena: un token ausente o inválido deja la
  petición sin autenticar y es `SecurityConfig` quien decide 401 o acceso público.

`JwtService` recibe el `Clock` por constructor (bean de `ClockConfig`) para que los tests
controlen el tiempo sin un segundo constructor.

**Cursos.** `course` es **un módulo con cuatro agregados dentro**, no cuatro módulos: el
curso en la raíz del paquete, las unidades y su material en `unit/` (`Unit` es la
«semana» o tema — «módulo» ya significa otra cosa en este repo), la matrícula en
`enrollment/` y el tablón de avisos en `announcement/`. Comparten catálogo (`CRS`) y
permisos (`courses:read`, `courses:create`, `courses:update`; el de escritura cubre
también unidades, material y avisos).

**El catálogo de errores no se parte** aunque el paquete sí: todos viven juntos en
`course/exception/`, porque el enum es la lista legible de todo lo que el módulo puede
responder y repartirla en varias carpetas la haría ilegible. Los DTOs sí bajan con su
agregado; `dto/CourseConstraints` se queda arriba porque lo validan todos.

Quien decide el acceso es **el docente titular**, no el rol: `CourseAccess` es la
única pieza que lo resuelve y todos los servicios del módulo pasan por ella.
`readable` deja entrar a quien administra docentes (`Role.canManage(TEACHER)`, otra vez
la misma fuente) y al titular del curso; `writable` añade que el curso no esté
archivado. De ahí salen sin código extra el listado acotado a los cursos propios, el
traspaso de curso solo entre quienes alcanzas y el 403 de todo lo demás.

Un curso no se borra, se archiva: `$archive` / `$activate`. Archivado se lee pero no
se escribe. Las unidades se ordenan con `position` y se reordenan de una vez con
`POST /courses/{id}/units/$reorder`, que reescribe 1..n; por eso `uk_units_course_position`
es `DEFERRABLE INITIALLY DEFERRED`, para permitir los estados intermedios de esa
transacción.

El material guarda `storageKey` (la key del objeto, nunca una URL) o `externalUrl` si
es un enlace, y el tipo decide cuál de los dos es obligatorio. `publishedAt` y `visible`
sí filtran: `UnitService` elige el predicado una vez por lectura —el staff lo ve todo, el
estudiante solo lo publicado—, en vez de repartir `if` por el mapeo. La descarga pasa por
el mismo predicado (`UnitService.requireVisible`).

**Archivos.** Los bytes nunca pasan por la API: `common/storage/FileStorage` firma
subidas y descargas contra cualquier servicio compatible con S3, elegido solo por
variables de entorno (MinIO en local, AWS hoy, R2 o B2 mañana). Cinco reglas que no se
ven en un solo fichero:

- **La clave la genera el backend** (`MaterialFiles.keyFor`: prefijo del curso, un UUID y
  el nombre saneado). Al crear o cambiar material se exige que la clave sea del curso y
  que ningún otro material la use; si no, un docente podría enseñar el archivo de otro
  curso con solo copiar su clave.
- **La subida firma tamaño, tipo y la etiqueta `status=pending`.** El tamaño firmado es lo
  que impide saltarse el límite del tipo; la etiqueta es lo que la regla de ciclo de vida
  usa para borrar lo que nunca se confirmó. Confirmar (`FileStorage.claim`) quita la
  etiqueta dentro de la transacción: si falla, no se crea el material.
- **Borrar el objeto va después del commit** (`FileCleanup.deleteAfterCommit`), por la
  misma razón que la cola de notificaciones: un rollback no puede deshacer una llamada
  externa. Un fallo solo se registra; un huérfano es más barato que un material roto.
- **Las URLs firmadas no se guardan nunca**: se firman al pedirlas y caducan en minutos.
- El presigner **no** lleva `checksumValidationEnabled(false)`: está deprecado y no
  cambia nada en una URL firmada (comprobado con el SDK 2.55.3: misma firma y solo
  `host` firmado en la descarga, con el flag y sin él). Su reemplazo,
  `requestChecksumCalculation`, solo existe en `S3Client`, que no firma URLs.

Los límites por tipo viven en `MaterialType`, no en configuración: son dominio.

**Adjuntos de tareas y entregas** (`assignment/attachment`) siguen esas mismas reglas con
tres diferencias:

- Un adjunto es de una tarea **o** de una entrega: dos claves foráneas y un `CHECK
  num_nonnulls(...) = 1`, no una columna polimórfica, para que la base guarde la integridad.
- La lista de adjuntos **se reemplaza entera** al guardar la tarea o la entrega. Un archivo
  que ya estaba se reconoce por su `storageKey` y no se vuelve a reclamar; lo que falta se
  borra y su objeto se elimina tras el commit. Por eso la respuesta devuelve la clave: el
  front la reenvía para conservar el archivo.
- La clave del estudiante cuelga de `courses/{curso}/submissions/{estudiante}/`, así que un
  alumno no puede entregar el archivo que subió otro. Descargar un adjunto de entrega exige
  ser staff o ser ese estudiante (`AttachmentDownloadService`).

Los formatos y el límite (50 MB, 10 adjuntos) viven en `AttachmentFiles` y en su espejo del
front (`attachmentFile.ts`); si cambian, cambian los dos.

La invitación se reparte de dos formas y solo se guarda una: el `invitation_code` del
curso es el dato, y `Invitations` compone además la URL al responder, colgando el
código de `app.courses.invitation-base-url` (apunta al **frontend**). Por eso cambiar de
dominio no toca la base. Solo se le devuelve al staff: repartir el acceso es decisión
del docente, no del alumno que ya entró.

**`CourseAccess` es `public` y eso es una excepción, no la regla.** Lo es solo porque la
comparten los tres sub-paquetes de `course`, y Java no tiene visibilidad «de módulo».
Quien la sostiene ahora es `CourseAccessBoundaryTest`, que lee las fuentes de `src/main`
y falla si algún fichero **fuera** de `course` la nombra — el mismo truco que
`ErrorCatalogueTest`, un invariante garantizado por un test en vez de por el compilador.
Ese test trae además dos comprobaciones contra sí mismo (que la clase sigue donde cree y
que el escaneo llega a las fuentes), porque un test que busca en ficheros pasa igual de
verde si deja de mirar donde debe. **Desde fuera del módulo se habla con `CourseService`
o `UnitService`**, nunca con `CourseAccess`.

**Matrícula.** Vive en el mismo módulo porque la política es un campo del curso:
`enrollmentPolicy` decide si una inscripción nace `ACTIVE` (`AUTOMATIC`) o `PENDING`
(`ON_REQUEST`), y esa traducción vive en un solo sitio, `EnrollmentPolicy.initialStatus()`.

Hay **una fila por (curso, estudiante)**: quien fue rechazado o se retiró y vuelve a
pedir entrar reutiliza la suya (`Enrollment.restart`), para que el histórico no se
multiplique ni haya que aflojar la unicidad.

`CourseAccess` pasó de responder «sí/no» a responder **quién eres**: `readable` devuelve
un `Reader(course, staff)` porque leer un curso ya no es una sola cosa —el estudiante
matriculado entra, pero ve menos—, mientras que `managed` y `writable` siguen siendo
solo para el titular y quien administra docentes. `listingScope` hace lo propio con los
listados: filtro por docente para el staff, por matrícula activa para el estudiante.

**En el curso donde estás matriculado, eres alumno**, aunque administres docentes: la
matrícula activa le gana al alcance de admin (`CourseAccess.staffIn`). Sin eso, un admin
que además es estudiante sería staff en todos los cursos y no podría entregar nada.
`CourseResponse.staff` lleva esa misma respuesta al front, curso a curso, también en el
listado (`CourseAccess.attendedAmong`, una sola consulta por página): la pantalla del
curso decide qué enseñar con ese campo y no con los permisos del rol elegido.
La otra mitad la cierra el titular: no puede inscribirse en su curso
(`CRS_TITULAR_CANNOT_ENROLL`) ni recibir en traspaso uno donde tiene matrícula pendiente
o activa (`CRS_TITULAR_IS_ENROLLED`). Entre las dos, nadie es staff y alumno del mismo
curso a la vez.

**Si alguna vez se plantea sacar la matrícula a módulo propio, leer esto antes.** Hoy no
se puede *tal cual*: `course` y `enrollment` se necesitan en los dos sentidos.

- `CourseAccess` consulta `EnrollmentRepository` para saber si el actor está
  matriculado (`enrolledStudentIn`).
- `EnrollmentService` depende de `CourseAccess` para saber si puede tocar el curso.
- `CourseRepository.searchEnrolled` hace `join Enrollment e on e.courseId = c.id`, un
  join entre entidades sin asociación: el acoplamiento está también en SQL, no solo en
  Java.

Separarlos sin más da una **dependencia circular de beans por constructor**: no arranca.
Lo que habría que decidir primero es **quién es el dueño de «quién soy en este curso»**
—hoy repartido entre `CourseAccess.readable` y `CourseService.memberOf`— y a dónde se
lleva `searchEnrolled`. Mientras `course` sea el único consumidor, partirlo cuesta más
de lo que aporta; la señal para hacerlo es que aparezca un segundo módulo que necesite
la misma noción de pertenencia. `assignment` ya la necesitó y le bastó con pedir
`memberOf`, que es justo la prueba de que hoy no hace falta.

**Tareas y calificaciones.** `assignment` es el primero que **no** vive dentro de
`course`: tarea y entrega tienen su propio catálogo (`ASG`) y hablan con el módulo de
cursos por servicio, que es la regla de slices. El contrato conviene que no crezca:
`CourseService.courseOf`, `CourseService.requireWritable`, `CourseService.memberOf`, que
devuelve un `CourseMember(courseId, staff, studentId)` —**quién eres en este curso**—, y
dos que trajo el registro de notas: `requireActiveStudent` (calificar a alguien exige que
esté matriculado) y `activeStudentsOf` (las filas del registro). Los avisos por correo
añadieron `nameOf`, porque el asunto lleva el nombre del curso. `CourseMember` es lo que
evita repetir la regla de alcance en cada servicio nuevo: el módulo de exámenes
necesitó lo mismo y le bastó con pedir `memberOf`.

La tarea guarda su `courseId` desnormalizado (la unidad no cambia de curso), así que
`courseOf` solo hace falta al crearla o al listar por unidad. `courseOf` es de
`UnitService`, que es su dueño natural, pero **desde fuera se pide a `CourseService`**,
que delega: cuando el examen se convirtió en el segundo consumidor, la superficie pública
de `course` pasó a estar toda en un sitio y nadie fuera del módulo importa
`course/unit`. Un módulo nuevo que necesite algo de `course` lo pide a `CourseService`.

Una entrega por (tarea, estudiante): reentregar reemplaza la fila, no acumula intentos, y
se cierra cuando **se devuelve** la nota, no cuando se pone. El estado (`SUBMITTED`/`LATE`)
lo decide la fecha contra `dueAt`, no el cliente. Una tarea con entregas o notas no se
borra (`ASG_HAS_WORK`): borrarla se llevaría el trabajo de los alumnos.

**Registro de notas.** Las notas son de `gradebook` (`GRB`), no de `assignment`, porque
las pondrán también los exámenes: si vivieran en `assignment`, el examen dependería de
las tareas para calificar. Cinco cosas que no se ven en un solo fichero:

- **La nota cuelga del ítem y del alumno** —única por (`sourceType`, `sourceId`,
  `studentId`)—, **no de la entrega**: es lo que permite calificar a quien no entregó
  (`PUT /assignments/{id}/grades/{studentId}`). Guarda `maxScore` copiado al calificar,
  para que cambiar el puntaje de la tarea no altere notas ya puestas. El rango y «nadie se
  califica a sí mismo» (`GRB_OWN_GRADE`) viven en `GradeService.record`, que es lo que
  hereda el examen. `gradedBy` nulo significa corrección automática.
- **Borrador y devolución.** La nota nace con `returnedAt` nulo y el estudiante no la ve
  hasta `$return`. La regla de quién ve qué es una sola, `GradeService.shownTo` —el
  staff lo ve todo, el estudiante solo lo devuelto—, y la usan tanto las entregas como el
  registro. Por eso la nota final que ve el docente incluye sus borradores y la del
  alumno no.
- **`gradebook` no conoce a `assignment`**: las columnas del registro le llegan por
  `GradeItemProvider`, igual que los perfiles le llegan a `security`. La dependencia va de
  `assignment` hacia `gradebook`; al revés sería un ciclo de beans. El examen publica el
  suyo (`ExamGradeItems`) y no hubo que tocar `gradebook` para enchufarlo. Una nota cuyo
  ítem ya no existe no cuenta.
- **El esquema es del curso** (`grading_schemes`, `grade_categories`): método
  `WEIGHTED` o `TOTAL_POINTS` y nota aprobatoria. Un curso sin fila usa el de
  `GradingScheme.byDefault` (total de puntos, 13), así que no hay que sembrar nada al
  crear un curso —y `course` no tiene que conocer a `gradebook`—. El PUT reemplaza el
  esquema entero; por eso la unicidad del nombre de categoría es diferida, como la de
  posición de unidades. Borrar una categoría deja sus tareas sin categoría
  (`ON DELETE SET NULL`).
- **El cálculo está en un solo sitio, `GradeCalculator`**, puro y probado aparte. La
  escala es siempre 0–20 (`GradingScale`). Dentro de una categoría cada tarea pesa por sus
  puntos; en `WEIGHTED` las categorías sin notas ceden su peso a las demás y las tareas sin
  categoría no cuentan; en `TOTAL_POINTS` cuenta todo. La final sale con dos decimales y
  el entero de acta se redondea **desde esos dos decimales** (`.5` hacia arriba): si se
  redondeara desde el valor exacto, un 12.495 se mostraría 12.50 y quedaría en 12. Aprueba
  quien tiene el entero redondeado igual o por encima de la aprobatoria.

**Exámenes.** `exam` (`EXM`) es el segundo módulo de evaluación y el primero que califica
solo. Cuelga de una unidad como la tarea y guarda su curso por la misma razón. El banco de
preguntas vive en `exam/question` y es del curso, no del examen: el examen elige preguntas
del banco y les pone su propio puntaje (`exam_questions`), y su `maxScore` es la suma,
recalculada en un solo sitio al reemplazar la lista. Seis cosas que no se ven en un solo
fichero:

- **Verdadero o falso es una opción única con dos opciones** que genera el backend
  (`QuestionShape`). Así corregir es siempre lo mismo, el conjunto marcado contra el
  correcto, y la opción múltiple es todo o nada. La respuesta corta **la corrige siempre el
  docente**: el intento queda en `PENDING_REVIEW` hasta que la puntúa. Comparar texto daría
  falsos negativos por tildes o sinónimos. Una pregunta sin responder vale cero y no espera
  a nadie, sea del tipo que sea.
- **El plazo lo fija el servidor al empezar**: `min(inicio + tiempo límite, cierre)`. Quien
  empieza tarde tiene menos tiempo, no más. Una respuesta después del plazo se rechaza
  (`EXM_ATTEMPT_CLOSED`) y el intento vencido se cierra con lo guardado de dos formas: al
  leerlo y desde `ExpiredAttemptsSchedule`, que barre los vencidos para que la nota llegue
  al registro aunque nadie abra nada. Las escrituras sobre un intento lo bloquean
  (`findForUpdate`), para que entregar y barrer no se pisen.
- **La semilla del intento fija el orden aleatorio** de preguntas y opciones: recargar la
  página no baraja otra vez y no hace falta guardar el orden.
- **Cuenta la nota más alta.** La nota es una sola por (examen, alumno) y se recalcula como
  el máximo de los intentos `GRADED` cada vez que uno lo es (`ExamGrading`). La corrección
  automática la registra con `GradeService.recordAutomatic` (`gradedBy` nulo); la que
  cierra el docente al puntuar, con `record` en su nombre. El segundo intento solo existe
  si el examen lo habilita para todos, y no se puede empezar con la nota ya devuelta.
- **Qué ve el alumno lo decide `Disclosure`, en un solo sitio**
  (`AttemptService.disclosureFor`): durante el intento, la hoja sin claves; entregado y sin
  devolver, nada —ni preguntas ni puntaje—; devuelta la nota, sus puntos por pregunta y,
  si el docente lo eligió (`showsAnswers`), las respuestas correctas. Es la misma regla de
  borrador y devolución de las tareas, para que la clave no se filtre mientras otros aún
  rinden.
- **Lo que ya se rindió no se cambia**: con intentos, la lista de preguntas del examen
  queda fija (`EXM_HAS_ATTEMPTS`) y sus preguntas del banco no se editan
  (`EXM_QUESTION_LOCKED`). Una pregunta que usa algún examen no se borra
  (`EXM_QUESTION_IN_USE`) y un examen con intentos o notas tampoco (`EXM_HAS_WORK`).

El banco y las preguntas de un examen con sus respuestas son solo del staff, y eso lo
dicen dos cosas: el permiso (`questions:read`, que el estudiante no tiene) y
`AnswerKey.requireReadableBy`, para el admin que es alumno de ese curso.

## Tests

- Ningún test salvo el de contexto necesita base de datos; todos corren siempre.
- Las rodajas `@WebMvcTest` usan tokens reales y traen los beans de seguridad con `@Import`
  explícito — al añadir un bean a la cadena hay que añadirlo también ahí, en todas.
- Cada rodaja declara **su** controlador: `@WebMvcTest(TeacherController.class)`, y las de
  `security`/`common` apuntan a su `ProbeController` anidado. Un `@WebMvcTest` sin
  argumentos escanea todos los `@RestController` de la aplicación y revienta el contexto en
  cuanto otro módulo añade un controlador con dependencias que la rodaja no conoce.
- `AulaVirtualApplicationTests` levanta el contexto completo y necesita Postgres
  (`aula_virtual_test`). Está anotado con `@EnabledIf`: si no hay base accesible **se omite
  en lugar de fallar**, para que el build funcione en una máquina recién clonada. Un
  `BUILD SUCCESSFUL` con ese test omitido **no** demuestra que el esquema valide; para eso
  hay que levantar `docker compose up -d`, que crea las dos bases.

## Configuración del centro, auto-registro y notificaciones

**`settings` es una tabla de una sola fila** y eso lo garantiza la base, no una convención:
un `UNIQUE` sobre una columna que un `CHECK` obliga a ser `TRUE` no admite una segunda. La
fila la siembra `V12`, así que `SettingsService.current()` es una lectura y no una rama
crear-o-actualizar, y el valor por defecto está escrito en un solo sitio. La columna
centinela **no se mapea en la entidad** a propósito: es un invariante de la base, y no
mapearla impide además que Java intente insertar otra fila.

`studentIdentifier` decide de dónde sale el usuario de un estudiante nuevo
(`DOCUMENT_NUMBER`, `EMAIL` o `MANUAL`), y quien lo aplica es **`student/StudentCredentials`,
el único sitio**. Se usa solo cuando no vienen credenciales explícitas **y** la persona no
tiene cuenta todavía: ese orden es lo que conserva los dos comportamientos de
`UserService.ensureAccount` —el 409 `USR_ACCOUNT_ALREADY_EXISTS` y el reuso silencioso de
una cuenta existente—. La contraseña inicial siempre es el número de documento.

**El auto-registro por enlace** vive en `course/enrollment/SelfEnrollmentService` y es la
única vía por la que se crea una cuenta sin actor (`StudentService.register`, el único
método del módulo que no empieza por `requireManagerOf`). Tres cosas que no son obvias:

- Solo acepta **DNI y carné de extranjería**. Un pasaporte puede tener 6 caracteres, por
  debajo de `PASSWORD_MIN`, y `User.normalizeUsername` pasa a minúsculas mientras la
  búsqueda por documento distingue mayúsculas: «tu usuario es tu documento» dejaría de ser
  cierto.
- Documento repetido, correo repetido, username tomado y perfil ya existente responden
  **todos** `CRS_ACCOUNT_ALREADY_REGISTERED`. Códigos distintos convertirían una ruta
  pública en un oráculo de quién tiene cuenta en el centro.
- `selfRegistrationEnabled` nace en `FALSE` y es la ventana que abre y cierra la dirección.
  Es la mitigación principal contra el alta masiva de cuentas por un enlace filtrado: el
  código de invitación no caduca ni se puede rotar.

`CRS_COURSE_NOT_OPEN` sustituye a `CRS_ARCHIVED` en todo lo que lee un estudiante;
`ARCHIVED` se queda para el staff, porque su mensaje dice «actívalo para poder
modificarlo» y quien se inscribe no puede activar nada.

**Recuperar la contraseña** vive en `user/PasswordResetService` y tiene dos rutas
públicas (`/auth/password-reset` y su `$complete`). La primera responde 202 exista o no la
cuenta —la misma regla anti-oráculo que `CRS_ACCOUNT_ALREADY_REGISTERED`— y no manda un
segundo enlace a la misma cuenta dentro de `password-reset-cooldown`, que es lo único que
hoy impide inundar un buzón desde fuera. Se guarda el hash del token, nunca el token; el
enlace sirve una vez y al usarse cierra los demás abiertos de esa cuenta. El correo
incluye el **usuario**, porque quien olvida la contraseña a menudo olvidó también eso.

`CRS_ACCOUNT_ALREADY_REGISTERED` no es un callejón sin salida: el front lo convierte en
«ya tienes cuenta» con el login y la recuperación a mano, y el login vuelve al enlace.

**La cola de notificaciones es una tabla y no un broker**: la fila se escribe en la misma
transacción que el hecho que la provoca, así que una matrícula que hace rollback no deja un
correo prometido. Un broker no da esa garantía por sí solo. El día que haga falta, el sitio
donde enchufarlo es `NotificationDispatcher`.

El consumidor **no envuelve el lote en una transacción**, y son tres pasos: reclamar con
`FOR UPDATE SKIP LOCKED` y soltar el bloqueo, enviar fuera de toda transacción, y marcar
cada resultado por separado. Envolverlo retendría el bloqueo y su conexión durante el viaje
SMTP, un fallo desharía el `SENT` de los anteriores, y como `MailException` es
`RuntimeException` el `FAILED` no sobreviviría al `rollback-only` y el contador de intentos
no subiría nunca. Los intentos se cuentan **al reclamar**, no al fallar: una caída en medio
cuesta un reintento tardío en vez de una fila atascada.

`last_error` se recorta en Java. Si desbordara la columna, el `UPDATE` que marca `FAILED`
haría rollback y la fila se reenviaría en bucle.

Un destinatario en blanco **no encola**: `persons.email` es opcional y `recipient` es
`NOT NULL`, así que sin esa guarda aceptar la matrícula de alguien sin correo reventaría
dentro de su propia transacción.

En `test` el consumidor está apagado (`app.notifications.enabled: false`) para que el
contexto no deje un hilo sondeando la base después de cerrarse.

## Perfiles

Ninguno por defecto. `dev` (SQL en el log, actuator con `health,info,metrics`, secreto JWT
de desarrollo embebido), `test` (secreto fijo en `src/test/resources`), `prod` (logging
mínimo, `flyway.clean` deshabilitado). `dev` no expone `env` ni `configprops`, que
devolvían el secreto de firma a cualquier autenticado. El secreto debe ser Base64 de al
menos 32 bytes o `JwtService` falla al construirse.
