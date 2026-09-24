# 09 · Correo con SES

## Qué vas a construir

Correos de verdad desde `no-reply@<dominio>.pe`: la bienvenida al inscribirse por el enlace
y la recuperación de contraseña.

## Qué aprendes

**SES (Simple Email Service)** envía correo por API o por **SMTP**. La app ya sabe hablar
SMTP (`spring.mail`), así que solo hay que darle el servidor y las credenciales: no se
toca código.

Para que un correo no acabe en spam, el destinatario comprueba que quien lo manda tiene
derecho a usar ese dominio. Para eso sirve **DKIM**: SES firma cada correo, y tú publicas
en tu DNS la clave pública con la que se comprueba la firma. Son tres registros CNAME.

**El sandbox.** Toda cuenta nueva de SES está *en sandbox*: solo puede enviar **a
direcciones verificadas** (que tú hayas confirmado), como mucho 200 correos al día. Para
una demo basta: verifica los correos que vayas a usar en las pruebas. Salir del sandbox
se pide con un formulario y AWS lo revisa a mano.

Recuerda cómo funciona la cola (sección «Notificaciones» del README): la app no envía el
correo durante la petición, lo guarda en la tabla `notifications` y un proceso lo envía
cada 30 segundos. Si SES falla, el correo se reintenta, no se pierde.

## Pasos en la consola

### 9.1 Verifica el dominio

- [ ] Busca **Amazon Simple Email Service** (us-east-1). Si ves un asistente de *Get
      started*, puedes seguirlo o ir directamente a **Configuration → Identities**.
- [ ] **Create identity** → **Domain** → `<dominio>.pe`.
- [ ] **Verifying your domain** → **Easy DKIM**, **RSA_2048_BIT**; deja **DKIM signatures**
      activado.
- [ ] **Create identity**.

En **DomainKeys Identified Mail (DKIM)** → **Publish DNS records** aparecen **tres CNAME**.

- [ ] Créalos en tu proveedor DNS con el mismo cuidado que en el capítulo 08: en el nombre,
      solo la parte anterior a tu dominio (`abc123._domainkey`).
- [ ] Añade además un registro **TXT** llamado `_dmarc` con el valor
      `v=DMARC1; p=none;`. No es obligatorio, pero Gmail y Outlook desconfían de los
      dominios sin DMARC.

A los pocos minutos la identidad pasa a **Verified**, con DKIM **Successful**.

### 9.2 Verifica los destinatarios de prueba

Mientras estés en sandbox:

- [ ] **Identities → Create identity → Email address** → tu correo personal (y los de
      quien vaya a probar la demo).
- [ ] Abre el correo de AWS que llega a cada uno y pulsa el enlace de verificación.

### 9.3 Credenciales SMTP

- [ ] **SMTP settings** (menú izquierdo). Anota el **SMTP endpoint**:
      `email-smtp.us-east-1.amazonaws.com`.
- [ ] **Create SMTP credentials** → deja el nombre de usuario que propone → **Create user**.
- [ ] **Descarga el CSV** o copia el **SMTP user name** y la **SMTP password**. La
      contraseña **no se vuelve a mostrar nunca**.

> **¿Qué acaba de pasar?** SES creó un **usuario IAM** con permiso para `ses:SendRawEmail` y
> unas claves de acceso; la «contraseña SMTP» se deriva de su clave secreta. Búscalo en
> **IAM → Users**: es el único usuario con claves permanentes de este tutorial, porque
> SMTP no entiende de roles.

### 9.4 Los parámetros del correo

En **Parameter Store**, como en el capítulo 06:

| Name | Type | Value |
|---|---|---|
| `/aula-virtual/demo/SPRING_MAIL_HOST` | String | `email-smtp.us-east-1.amazonaws.com` |
| `/aula-virtual/demo/SPRING_MAIL_PORT` | String | `587` |
| `/aula-virtual/demo/SPRING_MAIL_USERNAME` | String | el *SMTP user name* |
| `/aula-virtual/demo/SPRING_MAIL_PASSWORD` | **SecureString** | la *SMTP password* |
| `/aula-virtual/demo/NOTIFICATIONS_FROM` | String | `no-reply@<dominio>.pe` |

- **`SPRING_MAIL_HOST`** es el interruptor: mientras no existe, los correos solo se escriben
  en el log. Con él, se envían.
- **587** es SMTP con STARTTLS, que es lo que la app usa por defecto.
- **`NOTIFICATIONS_FROM`** tiene que ser de un dominio (o una dirección) **verificado** en
  SES. Como el dominio entero está verificado, sirve cualquier dirección `@<dominio>.pe`,
  aunque no exista un buzón detrás.

- [ ] Reinicia la app para que lea los parámetros nuevos, desde Session Manager:

```bash
sudo systemctl restart aula-virtual
```

## Con la CLI (opcional)

```powershell
aws sesv2 create-email-identity --email-identity <dominio>.pe
aws sesv2 get-email-identity --email-identity <dominio>.pe --query 'DkimAttributes.Tokens'
aws sesv2 create-email-identity --email-identity tu-correo@gmail.com
```

Cada *token* de DKIM es un CNAME: `<token>._domainkey.<dominio>.pe` →
`<token>.dkim.amazonses.com`. Las credenciales SMTP es más cómodo crearlas en la consola.

## Comprueba que funciona

- [ ] **Identities**: el dominio **Verified** y tu correo **Verified**.
- [ ] Prueba sin la app, desde la consola: abre la identidad del dominio → **Send test
      email** → From `no-reply@<dominio>.pe`, To tu correo verificado. Te llega.
- [ ] `cut -d= -f1 /etc/aula-virtual.env` en el servidor muestra ahora 15 nombres.

La prueba con la app (recuperar una contraseña y recibir el correo) está en el capítulo
11, cuando ya tengas el front y un usuario con tu correo.

## Si algo falla

Mira el log de la app en el servidor:

```bash
journalctl -u aula-virtual --since "10 min ago" | grep -i -E "mail|notification"
```

- **`Email address is not verified`**: estás en sandbox y el destinatario no está
  verificado (o el remitente no es de tu dominio).
- **`535 Authentication Credentials Invalid`**: usuario o contraseña SMTP mal copiados. Es la
  contraseña SMTP, **no** la clave secreta de IAM.
- **Timeout conectando al 587**: el SG de salida de la app se modificó. Por defecto permite
  toda salida.
- **Llega a spam**: normal las primeras veces con un dominio nuevo. Revisa que DKIM esté
  *Successful* y que exista `_dmarc`.

Siguiente: [10 · El front en CloudFront](10-front-cloudfront.md)
