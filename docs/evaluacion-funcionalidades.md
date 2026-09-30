# Evaluación de funcionalidades — aula virtual

Revisión del estado al 2026-09-30 (`develop`, tras `1.0.0-M2`), backend y frontend
(`aula-virtual-web`). El objetivo es decidir qué se hace después: cada punto lleva una
prioridad propuesta y un esfuerzo estimado para discutirlos, no para darlos por buenos.

- **Prioridad**: P1 (bloquea un uso real del aula), P2 (se echa en falta pronto), P3 (mejora).
- **Esfuerzo**: S (horas), M (1–3 días), L (una semana o más).
- **Estado**: ✅ hecho (con la rama o el commit que lo trae). Lo que no lleva marca sigue
  pendiente.

---

## 1. Qué hay hoy

| Área | Qué cubre | Dónde |
|---|---|---|
| Identidad y perfiles | Persona por documento, perfiles docente/estudiante/admin/superadmin, varios perfiles por persona, alta/edición/baja de perfil, cambio de contraseña por quien administra | `person`, `teacher`, `student`, `administrator` |
| Cuenta y acceso | Login JWT (1 h), `/me` (ver, editar, cambiar contraseña), elegir rol de trabajo, recuperación de contraseña por correo | `user`, `security` |
| Cursos | CRUD, archivar/activar, fechas, traspaso de titular, código de invitación y URL | `course` |
| Unidades y material | Unidades ordenables, material PDF/video/PPT/DOC/enlace en S3, publicación programada y visibilidad | `course/unit` |
| Matrícula | Automática o por solicitud, aceptar/rechazar/retirar, auto-registro con cuenta nueva por enlace (conmutable por el centro) | `course/enrollment` |
| Tareas y entregas | Tareas por unidad con fecha, puntaje, tardías y categoría; entrega con texto y hasta 10 adjuntos; estado SUBMITTED/LATE | `assignment` |
| Calificaciones | Nota por ítem y alumno con retroalimentación, borrador y devolución, esquema ponderado o por puntos, escala 0–20, nota final y aprobado | `gradebook` |
| Configuración | Origen del usuario de un estudiante nuevo, ventana de auto-registro | `settings` |
| Notificaciones | Cola transaccional por correo: cuenta creada, matrícula solicitada/activa, reset de contraseña | `notification` |
| Operación | Perfiles dev/test/prod, Flyway, actuator, OpenAPI, CI de GitHub Actions, guía de despliegue en AWS | `config`, `docs/deploy-aws` |

La base técnica está bien resuelta (límites de módulo verificados por test, catálogo de
errores, cola transaccional, archivos firmados). Lo que falta es sobre todo **funcional**:
el aula ya sirve para publicar material y tareas y poner notas, pero no para *comunicar*,
*evaluar en línea* ni *hacer seguimiento*.

---

## 2. Funcionalidades que faltan

### 2.1 Evaluación

**Exámenes / cuestionarios** — P1 · L
Ya previsto en la arquitectura (`GradeItemProvider`, `GradeService.record`,
`CourseService.memberOf`). Mínimo viable: banco de preguntas por curso (opción única,
múltiple, verdadero/falso, respuesta corta), examen con ventana de apertura/cierre, tiempo
límite, intentos, orden aleatorio y corrección automática (`gradedBy` nulo). Las preguntas
abiertas quedan en borrador para el docente. Decidir antes: ¿un intento o varios (mejor
nota / última)?, ¿se muestran las respuestas correctas al devolver?

**Rúbricas** — P3 · M
Criterios con niveles y puntaje por tarea; la nota sale de la suma. Da coherencia a la
corrección y explica la nota al alumno.

**Tareas grupales** — P3 · L
Grupos dentro del curso y una entrega por grupo con la misma nota para todos.

**Pendientes ya anotados en la rama de notas** (sin decidir):
- Tarea sin nota o sin fecha límite.
- Vista «trabajo de los alumnos» con **todos** los matriculados y su estado (entregado,
  tarde, sin entregar), no solo quienes entregaron.
- Reabrir una entrega ya devuelta.
- Periodos o bimestres dentro del curso (nota por periodo y final).

### 2.2 Comunicación

✅ **Avisos del curso (tablón)** — P1 · M
Hoy el docente no tiene forma de escribir a sus alumnos dentro del aula. Un anuncio por
curso con notificación por correo resuelve la mayor parte del caso de uso.
**Hecho** en `feature/course-announcements` (los dos repos, migración `V22`): pestaña
«Avisos» del curso; el staff publica, edita y borra, y al publicar se envía el aviso
completo por correo a los matriculados. Es solo de ida: sin comentarios de los alumnos
(pregunta 4 de la sección 5, sigue abierta).

**Notificaciones que faltan** — P1 · S cada una
La cola existe y añadir un tipo es barato. Faltan los avisos que más se esperan:
- ✅ Tarea nueva publicada / material nuevo publicado. **Hecho solo para tareas** en
  `feature/missing-notifications`. El material puede publicarse en diferido
  (`publishedAt`) y editarse u ocultarse antes: avisarlo bien pide reconciliar la cola con
  esos cambios, así que queda fuera; el tablón de avisos cubre el caso a mano.
- ✅ Nota devuelta (`$return`). **Hecho** en `feature/missing-notifications`.
- ✅ Recordatorio de tarea que vence (p. ej. 24 h antes, para quien no entregó) — necesita
  un planificador, que ya hay (`SchedulingConfig`). **Hecho** en
  `feature/assignment-reminders` (migración `V21`): 24 h antes, una vez por tarea, y de
  nuevo si cambia la fecha.
- ✅ Al docente: solicitud de matrícula pendiente de revisar. **Hecho** en
  `feature/missing-notifications`, también cuando llega por el auto-registro.
- ✅ Matrícula rechazada (hoy el alumno no se entera). **Hecho** en
  `feature/missing-notifications`.

**Notificaciones dentro de la app** — P2 · M
Campana con no leídos. Reutilizaría los mismos hechos que la cola de correo; hay que decidir
si es la misma tabla con otro canal o una propia.

**Preferencias de notificación** — P3 · S
Que cada persona apague los correos que no quiere (excepto los de seguridad).

**Foro o comentarios** — P3 · L
Comentarios en tareas (privados alumno–docente) o un foro por unidad. El comentario privado
en la entrega es lo más útil y lo más barato de los dos.

### 2.3 Gestión académica

✅ **Matrícula por el staff** — P1 · S/M
`enrollments:create` solo lo tiene `STUDENT`: el docente o el admin **no pueden inscribir a
un alumno** directamente, solo esperar a que use el código. Falta
`POST /courses/{id}/enrollments` para staff (uno o varios estudiantes existentes).
**Hecho** en `feature/staff-enrollment` (los dos repos): el titular o quien administra
docentes inscribe por número de documento, hasta 100 a la vez, con resultado por
documento; botón «Inscribir estudiantes» en la pestaña Matrículas. Decidido por mí (la
pregunta 2 de la sección 5): pueden hacerlo el docente titular y el admin, con el mismo
permiso que aceptar solicitudes (`enrollments:update`).

**Carga masiva** — P2 · M
Importar estudiantes (y su matrícula en un curso) desde CSV/Excel, con informe de filas
rechazadas. Es como llegan las listas reales de un centro. Debe reutilizar
`StudentCredentials` y `PersonService.resolveOrCreate`.

**Co-docentes / asistentes** — P2 · M
El curso tiene un único `teacherId`. Un segundo docente o un asistente que califica no
tiene cabida. `CourseAccess` concentra la regla, así que el cambio está acotado, pero toca
`staffIn`, `listingScope` y el traspaso.

**Asistencia** — P2 · M
Sesiones por curso y registro presente/tarde/falta/justificada; opcionalmente como
categoría del registro de notas vía `GradeItemProvider`.

**Periodo académico** — P3 · M
Hoy el curso solo tiene fechas sueltas. Un periodo (2026-I) permite filtrar, archivar en
bloque y copiar cursos de un periodo al siguiente.

**Duplicar curso** — P2 · M
Copiar unidades, material (claves nuevas en S3) y tareas a un curso nuevo. Es lo primero
que pide un docente al empezar el siguiente ciclo.

**Calendario / agenda** — P3 · M
Vencimientos de tareas y exámenes de todos mis cursos en una vista; exportable a iCal.

### 2.4 Seguimiento y reportes

**Panel de inicio útil** — P1 · M
`DashboardPage` solo muestra contadores de administración; para el estudiante está vacío.
Propuesta por rol:
- Estudiante: próximas entregas, notas recién devueltas, cursos con novedades.
- Docente: entregas por calificar, solicitudes de matrícula pendientes.
- Admin: contadores actuales + cursos sin actividad.

✅ **Exportar el registro de notas** — P1 · S
CSV/Excel del gradebook del curso; es lo que se entrega como acta.
**Hecho** en `feature/grade-export` (los dos repos): `GET /courses/{id}/gradebook/$export`,
solo para el staff, en CSV (UTF-8 con BOM, comas, punto decimal) con documento, notas por
tarea, promedios por categoría, final, entero de acta y condición. Incluye los borradores,
como la pantalla del docente. Botón «Exportar CSV» en Calificaciones, que guarda
`notas-<curso>.csv`.

**Progreso del alumno** — P2 · M
Por alumno y curso: entregas hechas/pendientes, notas, material visto. «Material visto»
requiere registrar accesos (ver 2.6).

**Boleta / constancia** — P3 · M
PDF con las notas finales de un estudiante en un periodo, o constancia de matrícula.

### 2.5 Contenido

- **Material de tipo texto/página** (P2 · S): hoy todo material es archivo o enlace; un
  bloque de texto enriquecido evita subir un PDF para dos párrafos.
- **Descripción de la unidad** (P3 · S): `Unit` solo tiene título.
- **Más formatos** (P3 · S): imágenes, hojas de cálculo, audio y ZIP como material
  (`MaterialType` solo admite PDF, video, PPT, DOC).
- **Vista previa en el navegador** (P3 · M) de PDF e imagen sin descargar.
- **Imagen de portada del curso** (P3 · S).

### 2.6 Seguridad y cuenta

✅ **Límite de intentos de login** — P1 · S
No hay limitación ni bloqueo por intentos fallidos en `/auth/login`; con usuarios
predecibles (el documento) y contraseña inicial igual al documento es el riesgo más
concreto del sistema.
**Hecho** en `feature/login-throttling`: 5 fallos en 15 min bloquean ese usuario 15 min
(429 `USR_TOO_MANY_LOGIN_ATTEMPTS`), configurable por entorno. Se cuenta por usuario,
exista o no; entrar bien o recuperar la contraseña borra el contador. Queda en memoria:
con varias instancias habría que llevarlo a la base.

✅ **Forzar cambio de la contraseña inicial** — P1 · S
La contraseña inicial es el número de documento y el correo solo *pide* cambiarla. Una
marca `mustChangePassword` que el front respete cierra el hueco.
**Hecho** en `feature/force-initial-password-change` (los dos repos, migración `V19`): la
cuenta nueva y la contraseña que pone un administrador quedan marcadas; el front lleva a
`/change-password` antes de cualquier pantalla. La nueva no puede ser igual a la actual
(`USR_PASSWORD_UNCHANGED`). Límites: lo aplica el front, no el backend, y las cuentas
anteriores a `V19` no quedaron marcadas.

**Sesión más larga sin reducir seguridad** — P2 · M
El JWT dura 1 h y no hay refresh token: al caducar se pierde la sesión, incluso a mitad de
una entrega. Opciones: refresh token rotado en cookie `HttpOnly`, o avisar antes de
caducar. Relacionado: el token vive en `localStorage` (expuesto a XSS); la cookie
`HttpOnly` lo resolvería a la vez.

**Registro de auditoría** — P2 · M
Quién cambió una nota, dio de baja a un docente, traspasó un curso o cambió la
configuración. `BaseEntity` guarda fechas pero no autor. En notas es especialmente
exigible.

**Cerrar sesiones al cambiar contraseña** — P3 · S
Hoy un JWT emitido antes del cambio sigue valiendo hasta caducar (el actor se recalcula,
pero la cuenta sigue viva). Un `passwordChangedAt` comparado con `iat` lo resuelve.

**Correo verificado** — P3 · M
El correo de la persona es opcional y nunca se verifica, pero es la vía de recuperación.

---

## 3. Mejoras sobre lo que ya existe

### Backend

| Mejora | Prioridad | Esfuerzo | Nota |
|---|---|---|---|
| ✅ Búsqueda por texto en listados de docentes, estudiantes, admins y cursos | P1 | S | Hoy solo filtran por `active`/`status`; con cientos de alumnos no hay forma de encontrar a uno. **Hecho** en `feature/list-search` (los dos repos, migración `V20`): `?q=` por nombre, documento o correo (cursos: nombre), sin distinguir tildes; buscador en las cuatro pantallas, guardado en la URL. |
| Contrato público de `course` en un solo sitio | P3 | S | Mover `UnitService.courseOf` a `CourseService` al hacer exámenes (ya anotado en CLAUDE.md). |
| Historial de entregas | P3 | M | Reentregar reemplaza la fila; se pierde qué se entregó antes y cuándo. |
| Rotar el código de invitación | P2 | S | El código no caduca ni se rota; si se filtra, la única defensa es cerrar el auto-registro para todo el centro. |
| Limpieza de `password_resets` y `notifications` antiguas | P3 | S | Tareas programadas que borren lo vencido/enviado tras N días. |
| Reintentos de notificaciones con backoff y alerta | P3 | S | Revisar que una fila `FAILED` definitiva se vea en algún sitio (métrica o log de nivel alto). |
| Métricas de negocio en actuator | P3 | S | Cola pendiente, fallos de envío, logins fallidos. |

### Frontend

| Mejora | Prioridad | Esfuerzo | Nota |
|---|---|---|---|
| Bloquear «Guardar» del esquema si los pesos no suman 100 | P2 | S | Hoy lo rechaza el backend con un toast. |
| Estados vacíos con acción | P2 | S | Curso sin unidades, unidad sin material, alumno sin cursos: decir qué hacer. |
| Revisión móvil del staff | P2 | M | La revisión de `revision-ui` cubrió al estudiante; queda la vista del docente (registro de notas en pantalla estrecha). |
| Aviso antes de que caduque la sesión | P2 | S | Mientras no haya refresh token. |
| Accesibilidad (contraste, foco, lectores) | P3 | M | No hay revisión hecha. |

### Operación y calidad

| Mejora | Prioridad | Esfuerzo | Nota |
|---|---|---|---|
| ✅ CI también en `develop` | P1 | S | `api.yml` y `web.yml` corren en push a `main` y en PR; con Git Flow las fusiones a `develop` sin PR no se prueban en ninguno de los dos repos. **Hecho** en `feature/ci-on-develop` (los dos repos): push a `main` y `develop`. |
| Pruebas end-to-end | P2 | M | Los recorridos críticos (auto-registro, entregar, calificar y devolver) se han probado a mano con Playwright; convertirlos en suite. |
| Copias de seguridad y restauración | P1 | S | Documentar y probar backup de RDS y del bucket en la guía de despliegue. |
| Observabilidad en prod | P3 | M | Logs estructurados con `traceId` y alarmas mínimas (5xx, cola). |

---

## 4. Propuesta de orden

1. **Cerrar huecos baratos y de riesgo** — límite de login, forzar cambio de contraseña
   inicial, CI en `develop`, búsqueda en listados, matrícula por el staff, exportar notas.
2. **Comunicación** — avisos del curso y las notificaciones que faltan (tarea nueva, nota
   devuelta, recordatorio, solicitud pendiente).
3. **Panel de inicio por rol** y vista «trabajo de los alumnos» con todos los matriculados.
4. **Exámenes**, que es el siguiente módulo grande y ya tiene el terreno preparado.
5. **Gestión académica** — carga masiva, duplicar curso, co-docentes, asistencia.
6. Lo demás (rúbricas, foros, grupos, calendario, reportes PDF) según demanda real.

## 5. Preguntas para decidir

- ¿Quién usará primero el aula en producción (un colegio, una academia, un instituto)?
  Cambia la prioridad de asistencia, periodos y boletas.
- ¿El docente debe poder matricular alumnos directamente, o solo el admin?
- ¿Exámenes con un solo intento o varios? ¿Se revelan las respuestas al devolver?
- ¿Avisos con respuesta de los alumnos (foro) o solo de ida?
- ¿Se acepta mover el token a cookie `HttpOnly`? Implica CSRF y ajustar CORS.
