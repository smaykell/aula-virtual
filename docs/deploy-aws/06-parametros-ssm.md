# 06 · Parámetros y secretos

## Qué vas a construir

La configuración de la app en producción, guardada en **SSM Parameter Store** bajo la ruta
`/aula-virtual/demo/`. El servidor la leerá al arrancar (capítulo 07).

## Qué aprendes

La app se configura **solo con variables de entorno** (tabla completa en el README del
repo). En tu equipo no hace falta ninguna, porque los valores por defecto apuntan a Docker.
En AWS hay que dárselas, y algunas son secretas: la contraseña de la base y el secreto con
el que se firman los JWT.

**Parameter Store** es un almacén clave-valor para configuración:

- Los parámetros se organizan como rutas (`/aula-virtual/demo/DB_URL`). Un solo permiso sobre
  la ruta, el que diste al rol en el capítulo 05, basta para leerlos todos.
- Tipo **String** para lo normal y **SecureString** para secretos: se cifran con KMS y la
  consola no los enseña salvo que lo pidas.
- El nivel *Standard* es **gratis**.

> **¿Y Secrets Manager?** Hace lo mismo con dos extras: rotación automática y réplica entre
> regiones. Cuesta 0,40 USD al mes por secreto. Para una demo no compensa, pero es la
> respuesta habitual en producción.

**Un límite que importa aquí:** Parameter Store **no admite valores vacíos**, y la app
necesita varias variables *definidas y vacías* (`STORAGE_ENDPOINT=` para usar AWS en vez de
MinIO, y las dos claves de S3 para usar el rol). Esas irán escritas en el servicio de
`systemd` (capítulo 07), no aquí. Tampoco son secretas.

## Pasos en la consola

### 6.1 Genera el secreto del JWT

Tiene que ser Base64 de al menos 32 bytes, o la app no arranca:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

### 6.2 Crea los parámetros

- [ ] Busca **Systems Manager** → menú izquierdo **Application Tools → Parameter Store**
      → **Create parameter**.

Para cada fila de la tabla: **Name** exactamente como aparece, **Tier** Standard, **Type**
el de la tabla, **Value** el tuyo → **Create parameter**.

| Name | Type | Value |
|---|---|---|
| `/aula-virtual/demo/SPRING_PROFILES_ACTIVE` | String | `prod` |
| `/aula-virtual/demo/DB_URL` | String | `jdbc:postgresql://<endpoint-rds>:5432/aula_virtual?sslmode=require` |
| `/aula-virtual/demo/DB_USER` | String | `aula_virtual` |
| `/aula-virtual/demo/DB_PASSWORD` | **SecureString** | la contraseña del capítulo 04 |
| `/aula-virtual/demo/JWT_SECRET` | **SecureString** | el valor de 6.1 |
| `/aula-virtual/demo/CORS_ALLOWED_ORIGINS` | String | `https://aula.<dominio>.pe` |
| `/aula-virtual/demo/COURSE_INVITATION_BASE_URL` | String | `https://aula.<dominio>.pe/join` |
| `/aula-virtual/demo/PASSWORD_RESET_URL` | String | `https://aula.<dominio>.pe/reset-password` |
| `/aula-virtual/demo/STORAGE_REGION` | String | `us-east-1` |
| `/aula-virtual/demo/STORAGE_BUCKET` | String | `aula-virtual-demo-material-<sufijo>` |

Por qué cada uno:

- **`SPRING_PROFILES_ACTIVE=prod`**: la app no tiene perfil por defecto. `prod` apaga el SQL
  en el log, desactiva `flyway.clean` y deja fuera el secreto de desarrollo.
- **`?sslmode=require`**: RDS exige conexiones cifradas desde PostgreSQL 15. El driver ya
  intenta TLS por su cuenta, pero así lo exige también el cliente y falla claro si no lo
  consigue.
- **`CORS_ALLOWED_ORIGINS`**: el front vivirá en otro dominio que la API, así que el navegador
  pedirá permiso. Solo tu front lo tiene.
- **`COURSE_INVITATION_BASE_URL`** y **`PASSWORD_RESET_URL`**: apuntan al **front**, no a la
  API. Son los enlaces que la app pone en las invitaciones y en los correos.

Los parámetros del correo (`SPRING_MAIL_*`, `NOTIFICATIONS_FROM`) se añaden en el capítulo
09. Mientras no existan, los correos se escriben en el log: la app funciona igual.

## Con la CLI (opcional)

```powershell
$p = "/aula-virtual/demo"
aws ssm put-parameter --name "$p/SPRING_PROFILES_ACTIVE" --type String --value prod
aws ssm put-parameter --name "$p/DB_URL" --type String --value "jdbc:postgresql://<endpoint-rds>:5432/aula_virtual?sslmode=require"
aws ssm put-parameter --name "$p/DB_USER" --type String --value aula_virtual
aws ssm put-parameter --name "$p/DB_PASSWORD" --type SecureString --value "<contraseña>"
aws ssm put-parameter --name "$p/JWT_SECRET" --type SecureString --value "<secreto-base64>"
aws ssm put-parameter --name "$p/CORS_ALLOWED_ORIGINS" --type String --value "https://aula.<dominio>.pe"
aws ssm put-parameter --name "$p/COURSE_INVITATION_BASE_URL" --type String --value "https://aula.<dominio>.pe/join"
aws ssm put-parameter --name "$p/PASSWORD_RESET_URL" --type String --value "https://aula.<dominio>.pe/reset-password"
aws ssm put-parameter --name "$p/STORAGE_REGION" --type String --value us-east-1
aws ssm put-parameter --name "$p/STORAGE_BUCKET" --type String --value "aula-virtual-demo-material-<sufijo>"
```

Para cambiar uno que ya existe, añade `--overwrite`.

## Comprueba que funciona

```powershell
aws ssm get-parameters-by-path --path /aula-virtual/demo --query 'Parameters[].[Name,Type]' --output table
```

- [ ] Salen los **10** parámetros.
- [ ] `DB_PASSWORD` y `JWT_SECRET` son `SecureString`.
- [ ] Ningún valor lleva los textos `<endpoint-rds>`, `<dominio>` o `<sufijo>` sin sustituir:

```powershell
aws ssm get-parameters-by-path --path /aula-virtual/demo --with-decryption --query 'Parameters[].[Name,Value]' --output text | Select-String '<'
```

  No debe imprimir nada. (Ojo: este comando enseña los secretos en pantalla.)

## Si algo falla

- **Un nombre mal escrito** (`/aula-virtual/demo/DB_URl`): los parámetros no se renombran.
  Bórralo y créalo de nuevo.
- **Te equivocaste de tipo**: el tipo no se puede cambiar a SecureString editándolo.
  Bórralo y créalo de nuevo.

Siguiente: [07 · Servidor EC2](07-servidor-ec2.md)
