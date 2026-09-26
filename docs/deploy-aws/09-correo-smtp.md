# 09 · Correo con tu servidor SMTP

## Qué vas a construir

Correos de verdad: la bienvenida al inscribirse por el enlace y la recuperación de
contraseña, enviados con **el mismo servidor SMTP que ya usas en desarrollo**.

## Qué aprendes

La app habla SMTP (`spring.mail`) y le da igual quién esté al otro lado: tu proveedor de
correo, Gmail o SES. Cambiar de servidor es cambiar **parámetros**, no código. Por eso aquí
no hace falta SES: tu buzón ya tiene el dominio configurado, sus registros SPF/DKIM
publicados y no está en ningún *sandbox*.

Lo que sí cambia respecto a tu máquina es **desde dónde** sale la conexión: ahora desde la
EC2. AWS bloquea por defecto la salida al puerto **25**, pero no al **587** (STARTTLS) ni
al **465** (SSL), que son los que se usan para enviar con usuario y contraseña.

Recuerda cómo funciona la cola (sección «Notificaciones» del README): la app no envía el
correo durante la petición, lo guarda en la tabla `notifications` y un proceso lo envía
cada 30 segundos. Si el servidor falla, el correo se reintenta, no se pierde.

## Si ya creaste cosas en SES

Una versión anterior de este capítulo usaba SES. Si llegaste a crearlo, bórralo ahora: no
cuesta dinero parado, pero el **usuario SMTP** tiene claves permanentes y es lo primero
que no debe quedarse olvidado en una cuenta.

- [ ] **IAM → Users** → el usuario `ses-smtp-user.…` → **Delete** (escribe su nombre para
      confirmar). La consola borra sus claves de acceso en el mismo paso.
- [ ] **Amazon SES → Configuration → Identities**: selecciona el dominio y cada correo
      verificado → **Delete**.
- [ ] **Tu proveedor DNS**: borra los **tres CNAME** `…._domainkey` que apuntan a
      `….dkim.amazonses.com`. Eran de SES y ya no firman nada.
      **No toques** los registros DKIM, SPF o MX de tu proveedor de correo: son los que
      hacen que tus correos no acaben en spam.
- [ ] El TXT `_dmarc` con `v=DMARC1; p=none;` puede quedarse: no depende de SES y le sirve
      igual a tu dominio. Bórralo solo si tu proveedor ya te había dado uno propio y ahora
      tienes dos (con dos `_dmarc`, los receptores ignoran ambos).
- [ ] Si ya creaste en Parameter Store los parámetros del correo con los valores de SES, no
      los borres: en el paso 9.2 los **editas**.

## Pasos en la consola

### 9.1 Reúne los datos de tu servidor

Son los mismos que tienes en tu máquina de desarrollo. En PowerShell:

```powershell
Get-ChildItem Env:SPRING_MAIL_*, Env:NOTIFICATIONS_FROM | Where-Object Name -ne SPRING_MAIL_PASSWORD
```

Anota el **host**, el **puerto**, el **usuario** y el **remitente**. La contraseña no la
imprimas: cópiala de donde la guardes o del panel de tu proveedor.

### 9.2 Los parámetros del correo

En **Parameter Store**, como en el capítulo 06 (**Create parameter**, o **Edit** si ya
existía):

| Name | Type | Value |
|---|---|---|
| `/aula-virtual/demo/SPRING_MAIL_HOST` | String | el host SMTP de tu proveedor |
| `/aula-virtual/demo/SPRING_MAIL_PORT` | String | `587` |
| `/aula-virtual/demo/SPRING_MAIL_USERNAME` | String | tu dirección de correo completa |
| `/aula-virtual/demo/SPRING_MAIL_PASSWORD` | **SecureString** | la contraseña de ese buzón |
| `/aula-virtual/demo/NOTIFICATIONS_FROM` | String | **la misma** dirección que el usuario |

- **`SPRING_MAIL_HOST`** es el interruptor: mientras no existe, los correos solo se escriben
  en el log. Con él, se envían.
- **587** es SMTP con STARTTLS, que es lo que la app usa por defecto. Si tu proveedor solo
  ofrece **465**, pon ese puerto y añade `/aula-virtual/demo/SPRING_MAIL_STARTTLS` = `false`.
- **`NOTIFICATIONS_FROM`** debe coincidir con el usuario con el que te autenticas: la
  mayoría de servidores rechazan enviar en nombre de otra dirección.
- La contraseña va como **SecureString** por lo mismo que la de la base: el rol de la EC2
  la descifra con KMS al leerla, y en la consola nadie la ve por accidente.

- [ ] Reinicia la app para que lea los parámetros nuevos, desde Session Manager:

```bash
sudo systemctl restart aula-virtual
```

## Con la CLI (opcional)

```powershell
aws ssm put-parameter --name /aula-virtual/demo/SPRING_MAIL_HOST --type String --value <host> --overwrite
aws ssm put-parameter --name /aula-virtual/demo/SPRING_MAIL_PORT --type String --value 587 --overwrite
aws ssm put-parameter --name /aula-virtual/demo/SPRING_MAIL_USERNAME --type String --value <correo> --overwrite
aws ssm put-parameter --name /aula-virtual/demo/SPRING_MAIL_PASSWORD --type SecureString --value '<contraseña>' --overwrite
aws ssm put-parameter --name /aula-virtual/demo/NOTIFICATIONS_FROM --type String --value <correo> --overwrite
```

`--overwrite` sirve igual para crear que para cambiar un valor. Y lo que hay que borrar
de SES, si prefieres la CLI:

```powershell
aws sesv2 list-email-identities
aws sesv2 delete-email-identity --email-identity <dominio-o-correo>
aws iam list-access-keys --user-name <ses-smtp-user.…>
aws iam delete-access-key --user-name <ses-smtp-user.…> --access-key-id <AKIA…>
aws iam list-attached-user-policies --user-name <ses-smtp-user.…>
aws iam list-user-policies --user-name <ses-smtp-user.…>
aws iam delete-user --user-name <ses-smtp-user.…>
```

`delete-user` falla mientras el usuario conserve claves o políticas: quítale antes las
adjuntas (`detach-user-policy`) y las en línea (`delete-user-policy`) que salgan en esos
listados. Por eso en la consola es un solo clic y aquí son varios.

## Comprueba que funciona

- [ ] `cut -d= -f1 /etc/aula-virtual.env` en el servidor muestra los cinco nombres del
      correo (seis si añadiste `SPRING_MAIL_STARTTLS`).
- [ ] Desde el servidor, el puerto de tu proveedor responde:

```bash
timeout 5 bash -c '</dev/tcp/<host>/587' && echo abierto
```

- [ ] Si borraste SES: **IAM → Users** ya no muestra `ses-smtp-user.…` y **SES →
      Identities** está vacío.

La prueba con la app (recuperar una contraseña y recibir el correo) está en el capítulo
11, cuando ya tengas el front y un usuario con tu correo.

## Si algo falla

Mira el log de la app en el servidor:

```bash
journalctl -u aula-virtual --since "10 min ago" | grep -i -E "mail|notification"
```

- **`535` o `Authentication failed`**: usuario o contraseña mal copiados. El usuario suele
  ser la dirección completa, no solo la parte antes de la `@`.
- **`550`/`553` … *not allowed to send as***: `NOTIFICATIONS_FROM` no coincide con el usuario.
- **Timeout conectando al 587**: o el SG de salida de la app se modificó (por defecto
  permite toda salida), o tu proveedor no acepta conexiones SMTP desde IPs de AWS. En ese
  caso pregúntale, o prueba el 465 con `SPRING_MAIL_STARTTLS=false`.
- **En local llega y desde la EC2 no, sin ningún error en el log**: la app no ve
  `SPRING_MAIL_HOST` y está escribiendo los correos en el log. Revisa el nombre exacto del
  parámetro y que reiniciaste el servicio.
- **Llega a spam**: revisa en tu proveedor que el dominio tenga SPF y DKIM activos. No
  depende de AWS: el correo sale del servidor de tu proveedor, no de la EC2.

Siguiente: [10 · El front en CloudFront](10-front-cloudfront.md)
