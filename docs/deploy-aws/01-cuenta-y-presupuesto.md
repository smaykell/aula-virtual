# 01 · Cuenta, usuario y presupuesto

## Qué vas a construir

Un usuario de trabajo con MFA (para dejar de usar el usuario *root*), una alerta de
gasto y la CLI conectada a tu cuenta.

## Qué aprendes

**IAM (Identity and Access Management)** es el servicio que decide *quién* puede hacer
*qué* en tu cuenta. Tiene cuatro conceptos que verás en todo el tutorial:

- **Usuario**: una persona (o programa) con credenciales permanentes.
- **Rol**: un conjunto de permisos que alguien *asume* temporalmente, sin contraseña. Tu
  servidor EC2 usará un rol en el capítulo 05.
- **Política**: un documento JSON que dice qué acciones se permiten sobre qué recursos.
- **Principio de mínimo privilegio**: dar solo los permisos necesarios, nada más.

El **usuario root** es el correo con el que abriste la cuenta. Puede hacerlo todo,
incluido cerrarla, y no se le puede quitar ningún permiso. Por eso se protege con MFA y se
guarda en un cajón: el trabajo diario se hace con otro usuario.

**AWS Budgets** vigila el gasto y te avisa por correo al pasar un umbral. No corta nada:
solo avisa.

## Pasos en la consola

### 1.1 MFA para el usuario root

- [ ] Entra como root → menú de la cuenta (arriba a la derecha) → **Security credentials**.
- [ ] En **Multi-factor authentication (MFA)** → **Assign MFA device**.
- [ ] Elige **Passkey or security key** o **Authenticator app** (Google Authenticator,
      Microsoft Authenticator, 1Password…) y sigue el asistente.

### 1.2 Un usuario de trabajo

- [ ] Busca **IAM** en la barra superior → **Users** → **Create user**.
- [ ] **User name**: `smaykell-admin` (o el que quieras).
- [ ] Marca **Provide user access to the AWS Management Console** → **I want to create an
      IAM user** → contraseña personalizada; desmarca *must create a new password*.
- [ ] **Permissions options** → **Attach policies directly** → busca y marca
      `AdministratorAccess`.
- [ ] **Create user**. Copia la **Console sign-in URL** que aparece
      (`https://<cuenta>.signin.aws.amazon.com/console`) y anota `<cuenta>`.

> **¿Por qué `AdministratorAccess` si hablamos de mínimo privilegio?** Porque este usuario
> eres tú aprendiendo: vas a tocar una docena de servicios. El mínimo privilegio lo
> aplicarás donde de verdad importa, en el rol del servidor (capítulo 05), que es lo que
> estaría expuesto a internet.

- [ ] Cierra sesión como root y entra con la URL de inicio de sesión y el usuario nuevo.
- [ ] **IAM → Users → smaykell-admin → Security credentials → Assign MFA device**. Sí,
      también este.

A partir de aquí **todo se hace con este usuario**.

### 1.3 Alerta de gasto

- [ ] Busca **Budgets** → **Create budget**.
- [ ] **Use a template (simplified)** → **Monthly cost budget**.
- [ ] **Budget name**: `aula-virtual-demo`. **Budgeted amount**: `25`.
- [ ] **Email recipients**: tu correo.
- [ ] **Create budget**.

La plantilla avisa al 85 % y al 100 % del importe, y también si el *pronóstico* del mes lo
supera, que es el aviso más útil: llega antes de gastarlo.

> Si quieres un aviso más temprano, edita el presupuesto y añade otro umbral al 40 %
> (10 USD).

### 1.4 La CLI

- [ ] Instala la **AWS CLI v2**: descarga el instalador MSI desde la documentación de AWS
      (*Installing or updating the latest version of the AWS CLI*) o, en PowerShell:

```powershell
winget install --id Amazon.AWSCLI
```

- [ ] Abre una terminal nueva y comprueba la versión (necesitas **2.32 o superior**):

```powershell
aws --version
```

- [ ] Conéctala a tu cuenta con las credenciales de la consola, sin crear claves de acceso:

```powershell
aws login --region us-east-1
```

Se abre el navegador, inicias sesión con `smaykell-admin` y la CLI recibe unas
credenciales **temporales** que se renuevan solas. Es mejor que las *access keys*
clásicas: si alguien copiara tu carpeta `~/.aws`, no se llevaría una clave que sirve para
siempre.

## Comprueba que funciona

```powershell
aws sts get-caller-identity
```

Debe responder con tu `Account` (el `<cuenta>` de la tabla) y un `Arn` que termina en
`user/smaykell-admin`. Si pone `root`, has iniciado sesión con el usuario equivocado.

- [ ] Root con MFA.
- [ ] Usuario de trabajo con MFA.
- [ ] Presupuesto creado (llega un correo de confirmación).
- [ ] `aws sts get-caller-identity` muestra tu usuario.

## Si algo falla

- **`aws: The term 'aws' is not recognized`**: la terminal se abrió antes de instalar.
  Ciérrala y abre otra.
- **`aws login` no existe**: tu CLI es anterior a la 2.32. Actualízala con
  `winget upgrade Amazon.AWSCLI`.
- **No encuentro Budgets**: está dentro de **Billing and Cost Management**, en el menú de
  la izquierda.

Siguiente: [02 · Red y Security Groups](02-red-y-security-groups.md)
