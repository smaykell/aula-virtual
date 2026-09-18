# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Comandos

```bash
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

Las variables de entorno se leen del proceso, no del `.env`. Para cargarlas en bash:
`set -a && source .env && set +a`.

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

Paquete raíz `io.github.smaykell.aulavirtual`, con tres zonas transversales (`config`,
`security`, `common`) más `modules/`, donde va todo el dominio. Hoy existe
`modules/user` (usuarios, login y aplicación del catálogo de roles).

**Vertical slice.** Cada módulo de dominio es un paquete autocontenido
(`modules/course/`: entidad, repositorio, servicio, controlador, `dto/`). Un módulo solo
depende de `common` y `config`; si dos módulos necesitan hablarse lo hacen a través del
*service* del otro, nunca de su repositorio ni de sus entidades.

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

El mensaje de una `ApiException` se envía tal cual al cliente: escribirlo para un usuario
final, sin detalles internos; los detalles van al log.

**Seguridad.** Cadena *stateless*, sin CSRF, sin usuarios en memoria. El login vive en
`modules/user` (`POST /auth/login`) y llama a `jwtService.issueToken(subject, authorities)`.
Las authorities viajan en el claim `roles` y son dos cosas a la vez: el rol **con prefijo**
(`ROLE_TEACHER`) y, junto a él, cada permiso del rol (`users:read`), de modo que
`hasRole(...)` y `hasAuthority(...)` funcionan sin traducción.

**Roles y permisos.** El catálogo vive en código, no en tablas: `security/Permission`
enumera los permisos y `security/Role` asigna a cada rol los suyos. Un usuario tiene
exactamente un rol (`users.role`). `Role.manageableRoles()` es la **única** fuente de quién
administra a quién — el superadmin administra admins, docentes y estudiantes; un admin solo
docentes y estudiantes; nadie administra a un `SUPER_ADMIN`, que solo nace de la semilla de
`V2__create_users.sql`. De ahí salen sin código extra las dos reglas del enunciado: entre
admins no se tocan y el listado solo muestra los roles que el solicitante administra.

Los literales de `@PreAuthorize` salen de `Permission.Name`, no de cadenas sueltas: así un
permiso mal escrito no compila.

Toda operación sobre usuarios recarga al actor desde la base (`UserService.activeActor`) en
vez de fiarse del token: es lo que hace que desactivar a alguien surta efecto de inmediato
en lugar de esperar a que caduque su JWT.

Dos detalles fáciles de romper:

- Las rutas de `SecurityConfig.PUBLIC_PATHS` se escriben **sin** el context-path `/api`;
  Spring Security las evalúa relativas al contexto.
- `JwtAuthenticationFilter` nunca corta la cadena: un token ausente o inválido deja la
  petición sin autenticar y es `SecurityConfig` quien decide 401 o acceso público.

`JwtService` recibe el `Clock` por constructor (bean de `ClockConfig`) para que los tests
controlen el tiempo sin un segundo constructor.

## Tests

- Ningún test salvo el de contexto necesita base de datos; todos corren siempre.
- Las rodajas `@WebMvcTest` usan tokens reales y traen los beans de seguridad con `@Import`
  explícito — al añadir un bean a la cadena hay que añadirlo también ahí, en todas.
- Cada rodaja declara **su** controlador: `@WebMvcTest(UserController.class)`, y las de
  `security`/`common` apuntan a su `ProbeController` anidado. Un `@WebMvcTest` sin
  argumentos escanea todos los `@RestController` de la aplicación y revienta el contexto en
  cuanto otro módulo añade un controlador con dependencias que la rodaja no conoce.
- `AulaVirtualApplicationTests` levanta el contexto completo y necesita Postgres
  (`aula_virtual_test`). Está anotado con `@EnabledIf`: si no hay base accesible **se omite
  en lugar de fallar**, para que el build funcione en una máquina recién clonada. Un
  `BUILD SUCCESSFUL` con ese test omitido **no** demuestra que el esquema valide; para eso
  hay que crear la base.

## Perfiles

Ninguno por defecto. `dev` (SQL en el log, actuator con `health,info,metrics`, secreto JWT
de desarrollo embebido), `test` (secreto fijo en `src/test/resources`), `prod` (logging
mínimo, `flyway.clean` deshabilitado). `dev` no expone `env` ni `configprops`, que
devolvían el secreto de firma a cualquier autenticado. El secreto debe ser Base64 de al
menos 32 bytes o `JwtService` falla al construirse.
