# 11 · Prueba de punta a punta

## Qué vas a hacer

Recorrer la demo como la usaría un centro de verdad y, en cada paso, saber **qué pieza de
AWS** estás probando. Si algo falla aquí, la tabla del final te dice dónde mirar.

Ten abierta una sesión de Session Manager con el log en directo durante toda la prueba:

```bash
sudo journalctl -u aula-virtual -f
```

## El recorrido

### 11.1 El superadministrador

- [ ] Entra en `https://aula.<dominio>.pe` con `superadmin`. Si todavía no cambiaste su
      contraseña (capítulo 08), hazlo **ahora** desde **Mi perfil**.
- [ ] En **Mi perfil**, corrige sus datos: la migración los dejó como `Pendiente` con un
      documento que no pasa la validación (explicado en el README, «El primer
      superadmin»). Pon **tu correo**.

*Pruebas:* CloudFront → ALB → EC2 → RDS; CORS de la API.

### 11.2 Docente, curso y material

- [ ] Crea un docente (con otro correo tuyo, si tienes).
- [ ] Crea un curso con ese docente como titular y la política de matrícula **Automática**.
- [ ] Añade una unidad y **sube un PDF** como material.

*Pruebas:* el navegador sube el archivo **directamente a S3** con una URL firmada por la
app usando el **rol IAM**; el **CORS** del bucket de material; la confirmación
(`DeleteObjectTagging`) y la comprobación del archivo (`HeadObject`).

Míralo en S3: el objeto aparece en `aula-virtual-demo-material-<sufijo>` bajo
`courses/…`, y en su pestaña **Properties → Tags** **no** tiene `status=pending` (se la
quitó la confirmación).

### 11.3 Estudiante por el enlace de invitación

- [ ] Como superadmin, en **Configuración**, activa **inscripción por enlace**.
- [ ] Copia el **enlace de invitación** del curso.
- [ ] Ábrelo en una **ventana de incógnito** y regístrate como estudiante con un DNI de
      prueba y **otro correo tuyo**.

> Un correo solo puede pertenecer a una persona: si repites el del superadmin, la app
> responde «ya tienes cuenta». Con Gmail puedes usar `tucorreo+alumno@gmail.com`, que llega
> al mismo buzón.

*Pruebas:* la ruta del SPA `/join/<código>` servida por CloudFront al abrirla directamente;
una ruta **pública** de la API; la **cola de notificaciones** y **tu servidor SMTP**: en ~30 segundos te
llega el correo de bienvenida.

### 11.4 El estudiante lee el material

- [ ] Inicia sesión como el estudiante (usuario y contraseña: el DNI, según la
      configuración por defecto).
- [ ] Abre el curso y **descarga el PDF**.

*Pruebas:* la **URL de descarga firmada** (caduca en 10 minutos: cópiala, espera 11 y
vuelve a abrirla; S3 responde `Request has expired`).

### 11.5 Recuperar la contraseña

- [ ] Cierra sesión → **¿Olvidaste tu contraseña?** → tu usuario.
- [ ] Abre el correo y sigue el enlace: lleva a `https://aula.<dominio>.pe/reset-password?token=…`.
- [ ] Cambia la contraseña y entra con la nueva.

*Pruebas:* `PASSWORD_RESET_URL` apunta bien al front; el correo; la ruta del SPA.

### 11.6 Cierra la ventana

- [ ] Como superadmin, **desactiva** la inscripción por enlace. El código de invitación no
      caduca: mientras la demo esté en internet, que no se pueda usar para crear cuentas.

## Mira lo que has construido

Unos minutos para ver los servicios con datos reales:

- [ ] **CloudWatch → Metrics → EC2**: el `CPUUtilization` de tu instancia durante la prueba.
- [ ] **EC2 → Target groups → Monitoring**: peticiones y tiempos de respuesta del ALB.
- [ ] **RDS → aula-virtual-demo → Monitoring**: conexiones abiertas (el pool de Hikari,
      hasta 10).
- [ ] **CloudFront → Reports & analytics**: peticiones servidas desde caché frente a las
      que fueron al bucket.
- [ ] **Billing → Bills** (o **Cost Explorer**): lo que llevas gastado, por servicio. Aparece
      con unas horas de retraso.

## Dónde mirar cuando algo falla

| Síntoma | Pieza | Dónde mirar |
|---|---|---|
| La web no carga | CloudFront, DNS de `aula` | capítulo 10 |
| La web carga pero el login no | CORS de la API, ALB, app | F12 → Console; capítulo 08 |
| Error `502` o `503` | ALB sin target sano | Target groups → Targets |
| Error con un `code` (`CRS_…`, `USR_…`) | la app, a propósito | es una regla de negocio, no un fallo de AWS |
| Error `500` | la app | `journalctl`, busca el `traceId` del error |
| La subida de un archivo falla | CORS del bucket o rol | F12 → Network; capítulos 03 y 05 |
| No llega un correo | parámetros `SPRING_MAIL_*`, puerto 587 | log con `grep -i mail`; capítulo 09 |

El `traceId` que devuelve cada error de la API también se escribe en el log del servidor:

```bash
sudo journalctl -u aula-virtual | grep <traceId>
```

Siguiente: [12 · Infraestructura como código](12-infraestructura-como-codigo.md)
