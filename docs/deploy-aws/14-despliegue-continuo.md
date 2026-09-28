# 14 · Despliegue continuo con GitHub Actions

## Qué vas a construir

Un botón **Run workflow** en cada repo que prueba, empaqueta y publica la versión nueva sin
que tengas que abrir una terminal. Hace lo mismo que hacías a mano en los capítulos 07 y 10,
pero lo ejecuta GitHub:

```
aula-virtual (push)       ─► tests + Postgres ─┐
aula-virtual (Run workflow)                    ├─► jar ─► S3 deploy/releases/<sha>.jar
                                               │           │
                                               │           └─► SSM Run Command ─► EC2: deploy.sh
                                               │                                  (si no arranca,
                                               │                                   vuelve al anterior)
aula-virtual-web (push)   ─► lint + tests ─────┤
aula-virtual-web (Run workflow)                └─► pnpm build ─► S3 web ─► invalidación CloudFront
```

**No vas a guardar ninguna clave de AWS en GitHub**, ni tampoco la `.pem` del servidor, que
de hecho no existe.

Puedes hacer este capítulo en cuanto el 11 pase entero. Si ya apagaste la demo (capítulo 13),
vuelve a levantarla primero.

## Qué aprendes

**GitHub Actions** ejecuta *workflows*: ficheros YAML en `.github/workflows/` que dicen
*cuándo* se ejecutan (un push, un PR o un botón) y *qué* hacen. Un workflow tiene *jobs*, y
cada job corre en una máquina virtual limpia (`ubuntu-latest`) que se destruye al terminar.

**OIDC (OpenID Connect)** es cómo GitHub entra en AWS sin claves. En cada ejecución GitHub
emite un *token* firmado que dice «soy el workflow del repo `smaykell/aula-virtual`, en el
entorno `demo`». AWS comprueba la firma, compara lo que dice el token con la *trust policy*
del rol y, si coincide, entrega credenciales temporales de una hora. Es el mismo mecanismo
que el rol de la EC2 del capítulo 05, con otra identidad en la *trust policy*: allí era
`ec2.amazonaws.com` y aquí es un repo concreto de GitHub.

> **¿Por qué no una access key en los *secrets* de GitHub?** Porque es permanente: si se
> filtra (en un log, en un fork, en una acción de terceros comprometida), sirve hasta que
> alguien se dé cuenta y la borre. Un token OIDC caduca en minutos y solo lo acepta el rol
> cuya *trust policy* nombra a ese repo y ese entorno.

**Un *environment* de GitHub** (`demo`) agrupa las variables de un destino y decide qué
ramas pueden desplegar en él. Con tu plan Pro y repos privados tienes variables, *secrets* y
restricción por rama. Lo que **no** tienes es la aprobación manual (*required reviewers*),
que en repos privados es solo de Enterprise. Por eso aquí el despliegue se lanza con un
botón (`workflow_dispatch`) y no con cada push: el botón hace de aprobación.

**SSM Run Command** ejecuta un comando en la instancia a través del mismo agente que usa
Session Manager, sin SSH ni puertos abiertos. Y en lugar de dejar que GitHub ejecute
*cualquier* comando como root, vas a crear un **documento SSM** propio que solo sabe hacer
una cosa: `deploy.sh <jar>`, con un nombre de jar que tiene que cumplir un patrón. Así el
peor caso de un workflow comprometido es desplegar una versión tuya anterior.

## Lo que vas a gastar

Cada ejecución del back tarda unos 4–6 minutos (con Postgres y los tests), y la del front
1–2. Tus 3.000 minutos al mes dan para varios cientos. En **GitHub → Settings → Billing and
licensing → Budgets and alerts**, crea un presupuesto de **Actions** de **0 USD** con
*Stop usage when budget limit is reached*: si algún día te pasas, los jobs se paran en vez
de cobrarte.

## Pasos

### 14.1 El proveedor de identidad de GitHub en AWS

Se crea una vez por cuenta de AWS y sirve para todos tus repos.

- [ ] **IAM** → **Identity providers** → **Add provider**.
- [ ] **Provider type**: **OpenID Connect**.
- [ ] **Provider URL**: `https://token.actions.githubusercontent.com`.
- [ ] **Audience**: `sts.amazonaws.com`.
- [ ] **Add provider**.

> Si la consola te pide un *thumbprint*, pulsa **Get thumbprint**. Hoy AWS valida los
> certificados de GitHub por su cuenta y ya no lo usa, pero algunas versiones de la pantalla
> todavía lo piden.

### 14.2 El documento que despliega

- [ ] **Systems Manager** → **Documents** → **Create document** → **Command or Session**.
- [ ] **Name**: `aula-virtual-deploy`. **Target type**: `/AWS::EC2::Instance`.
      **Document type**: Command. **Content**: **YAML**, y pega:

```yaml
schemaVersion: "2.2"
description: Despliega una version del aula virtual ya subida al bucket de deploy
parameters:
  key:
    type: String
    description: clave del jar dentro del bucket, releases/<sha>.jar
    allowedPattern: '^releases/[0-9a-f]{40}\.jar$'
mainSteps:
  - action: aws:runShellScript
    name: deploy
    inputs:
      timeoutSeconds: "300"
      runCommand:
        - /opt/aula-virtual/deploy.sh {{ key }}
```

- [ ] **Create document**.

> **¿Por qué el patrón?** Lo que llega en `key` se pega dentro de un comando que se ejecuta
> como root. Si no se validara, un `key` como `x; curl …| bash` ejecutaría cualquier cosa.
> Con el patrón, SSM rechaza la petición antes de tocar el servidor: solo pasan 40
> caracteres hexadecimales, que es lo que mide el SHA de un commit.

### 14.3 Un `deploy.sh` que sabe volver atrás

El script del capítulo 07 reiniciaba la app y ya está: si la versión nueva no arrancaba (por
una migración de Flyway rota, por ejemplo), la demo se quedaba caída. El nuevo espera a que
la app responda y, si en tres minutos no responde, vuelve a poner el jar anterior.

Entra por Session Manager (capítulo 07, apartado 7.2), hazte root con `sudo -i` y
sustitúyelo:

```bash
cat > /opt/aula-virtual/deploy.sh <<'EOF'
#!/bin/bash
set -euo pipefail
BUCKET=aula-virtual-demo-deploy-<sufijo>
KEY=${1:-aula-virtual.jar}
DIR=/opt/aula-virtual

healthy() {
  for _ in $(seq 1 36); do
    curl -sf localhost:8080/api/actuator/health >/dev/null && return 0
    sleep 5
  done
  return 1
}

aws s3 cp "s3://$BUCKET/$KEY" "$DIR/app.jar.new"
chown aula:aula "$DIR/app.jar.new"
if [ -f "$DIR/app.jar" ]; then cp -p "$DIR/app.jar" "$DIR/app.jar.previous"; fi
mv "$DIR/app.jar.new" "$DIR/app.jar"
systemctl restart aula-virtual

if healthy; then
  echo "Desplegado $KEY"
  exit 0
fi

echo "La version $KEY no arranco; vuelvo a la anterior" >&2
journalctl -u aula-virtual -n 40 --no-pager >&2
mv "$DIR/app.jar.previous" "$DIR/app.jar"
systemctl restart aula-virtual
exit 1
EOF
chmod 750 /opt/aula-virtual/deploy.sh
```

**Edita `<sufijo>`** igual que en el capítulo 07.

Lo que no es obvio:

| Línea | Por qué |
|---|---|
| `KEY=${1:-aula-virtual.jar}` | sin argumento hace lo de siempre, así que el despliegue a mano del capítulo 07 sigue funcionando. |
| `curl -sf …/health` | `-f` hace que `curl` falle con un 503. El *health* de Spring responde 503 mientras la app no está `UP`. |
| `36 × 5 s` | tres minutos. Un arranque normal en `t4g.small` tarda menos de uno; lo que pase de tres no es lentitud, es que no arranca. |
| `journalctl … >&2` | las últimas líneas del log van a la salida de error, y el workflow las enseña. Así ves el fallo desde GitHub sin entrar al servidor. |
| `exit 1` tras volver atrás | el workflow tiene que quedar **en rojo** aunque la demo siga en pie: la versión nueva **no** se desplegó. |

> **Volver atrás tiene un límite: la base.** Si la versión nueva aplicó una migración, el jar
> anterior arranca contra el esquema nuevo. Flyway lo tolera (ignora las migraciones que no
> conoce), pero el código viejo tiene que entender ese esquema. Por eso las migraciones se
> escriben **compatibles hacia atrás**: primero se añade una columna nueva y, en otra versión
> posterior, se borra la vieja. Nunca las dos cosas a la vez. Si la migración falla a medias,
> no pasa nada: PostgreSQL deshace el DDL de la transacción entera.

### 14.4 El rol del back

- [ ] **IAM** → **Policies** → **Create policy** → **JSON**, sustituyendo `<sufijo>`,
      `<cuenta>` e `<instance-id>` (el `i-…` del capítulo 07):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "UploadRelease",
      "Effect": "Allow",
      "Action": "s3:PutObject",
      "Resource": "arn:aws:s3:::aula-virtual-demo-deploy-<sufijo>/releases/*"
    },
    {
      "Sid": "RunOnlyTheDeployDocument",
      "Effect": "Allow",
      "Action": "ssm:SendCommand",
      "Resource": [
        "arn:aws:ssm:us-east-1:<cuenta>:document/aula-virtual-deploy",
        "arn:aws:ec2:us-east-1:<cuenta>:instance/<instance-id>"
      ]
    },
    {
      "Sid": "ReadCommandResult",
      "Effect": "Allow",
      "Action": "ssm:GetCommandInvocation",
      "Resource": "*"
    }
  ]
}
```

- [ ] **Policy name**: `aula-virtual-demo-gha-api` → **Create policy**.

| Sid | Por qué |
|---|---|
| `UploadRelease` | subir el jar, y solo bajo `releases/`. No puede leer ni borrar nada, ni tocar otro bucket. |
| `RunOnlyTheDeployDocument` | `SendCommand` necesita permiso **sobre las dos cosas**: el documento que ejecuta y la instancia donde lo ejecuta. Como solo nombra `aula-virtual-deploy`, no puede usar `AWS-RunShellScript`, el documento de AWS que ejecuta cualquier comando. |
| `ReadCommandResult` | leer si el comando terminó y qué imprimió. Esta acción no admite restringir por recurso: el `*` es obligatorio, y solo sirve para leer. |

- [ ] **IAM** → **Roles** → **Create role** → **Trusted entity type**: **Custom trust policy**,
      y pega (con tu `<cuenta>`):

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::<cuenta>:oidc-provider/token.actions.githubusercontent.com"
    },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
        "token.actions.githubusercontent.com:sub": "repo:smaykell/aula-virtual:environment:demo"
      }
    }
  }]
}
```

- [ ] **Next** → marca `aula-virtual-demo-gha-api` → **Role name**: `aula-virtual-demo-gha-api`
      → **Create role**. Anota su **ARN**.

> **La línea que importa es `sub`.** Dice «solo un job del repo `smaykell/aula-virtual` que
> corra en el entorno `demo`». Otro repo tuyo, un fork o un job sin `environment: demo`
> reciben *AccessDenied*. Y el entorno, a su vez, solo lo puede usar `main` (apartado 14.6).
> Cada pieza cierra una puerta distinta.
>
> La consola ofrece también un asistente *Web identity* que rellena la *trust policy* por ti,
> pero genera un `sub` por rama, no por entorno. Por eso aquí se pega a mano.

### 14.5 El rol del front

- [ ] Política `aula-virtual-demo-gha-web`, con tu `<sufijo>`, tu `<cuenta>` y el
      `<id-distribucion>` del capítulo 10:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SyncListsTheBucket",
      "Effect": "Allow",
      "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::aula-virtual-demo-web-<sufijo>"
    },
    {
      "Sid": "SyncWritesAndDeletes",
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:DeleteObject"],
      "Resource": "arn:aws:s3:::aula-virtual-demo-web-<sufijo>/*"
    },
    {
      "Sid": "Invalidate",
      "Effect": "Allow",
      "Action": "cloudfront:CreateInvalidation",
      "Resource": "arn:aws:cloudfront::<cuenta>:distribution/<id-distribucion>"
    }
  ]
}
```

`aws s3 sync` necesita **listar** para saber qué ha cambiado y qué sobra (`--delete`). El
ARN de CloudFront no lleva región, porque CloudFront es un servicio global.

- [ ] Rol `aula-virtual-demo-gha-web`: la misma *trust policy* del 14.4 con
      `"repo:smaykell/aula-virtual-web:environment:demo"` y esta política. Anota su **ARN**.

### 14.6 El entorno `demo` en cada repo

En **GitHub → smaykell/aula-virtual → Settings → Environments → New environment**:

- [ ] **Name**: `demo` → **Configure environment**.
- [ ] **Deployment branches and tags** → **Selected branches and tags** → **Add deployment
      branch or tag rule** → `main`.
- [ ] **Environment variables** → **Add environment variable**, una por una:

| Variable | Valor |
|---|---|
| `AWS_ROLE_ARN` | el ARN de `aula-virtual-demo-gha-api` |
| `DEPLOY_BUCKET` | `aula-virtual-demo-deploy-<sufijo>` |
| `INSTANCE_ID` | `i-…` |
| `API_URL` | `https://api.<dominio>.pe/api` |

En **smaykell/aula-virtual-web**, otro entorno `demo` con la misma regla de rama y:

| Variable | Valor |
|---|---|
| `AWS_ROLE_ARN` | el ARN de `aula-virtual-demo-gha-web` |
| `WEB_BUCKET` | `aula-virtual-demo-web-<sufijo>` |
| `DISTRIBUTION_ID` | `E…` |
| `VITE_API_BASE_URL` | `https://api.<dominio>.pe/api` |

> **Variables, no *secrets*.** Nada de esto es secreto: un ARN o un ID de instancia no dan
> acceso a nada sin la *trust policy*. Guardarlos como *secret* solo haría que GitHub los
> tape con `***` en el log y te costaría depurar.

### 14.7 El workflow del back

Primero, un detalle del repo: `gradlew` está guardado **sin permiso de ejecución**. En
Windows da igual, pero en el runner de Linux daría `Permission denied`. Desde la raíz de
`aula-virtual`:

```powershell
git update-index --chmod=+x gradlew
git commit -m "Marca gradlew como ejecutable para los runners de Linux"
```

Crea `.github/workflows/api.yml`:

```yaml
name: API

on:
  push:
    branches: [main]
  pull_request:
  workflow_dispatch:

permissions:
  contents: read

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:18-alpine
        env:
          POSTGRES_USER: aula_virtual
          POSTGRES_PASSWORD: aula_virtual
          POSTGRES_DB: aula_virtual_test
        ports:
          - 5432:5432
        options: >-
          --health-cmd "pg_isready -U aula_virtual -d aula_virtual_test"
          --health-interval 5s --health-timeout 3s --health-retries 10
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: 21
      - uses: gradle/actions/setup-gradle@v6
      - run: ./gradlew build
      - name: El test de contexto corrio contra Postgres
        run: grep -q 'skipped="0"' build/test-results/test/TEST-io.github.smaykell.aulavirtual.AulaVirtualApplicationTests.xml
      - if: github.event_name == 'workflow_dispatch'
        run: cp build/libs/*-SNAPSHOT.jar aula-virtual.jar
      - if: github.event_name == 'workflow_dispatch'
        uses: actions/upload-artifact@v7
        with:
          name: jar
          path: aula-virtual.jar
          retention-days: 1

  deploy:
    if: github.event_name == 'workflow_dispatch'
    needs: test
    runs-on: ubuntu-latest
    environment: demo
    concurrency: deploy-demo-api
    permissions:
      contents: read
      id-token: write
    env:
      KEY: releases/${{ github.sha }}.jar
    steps:
      - uses: actions/download-artifact@v8
        with:
          name: jar
      - uses: aws-actions/configure-aws-credentials@v6
        with:
          role-to-assume: ${{ vars.AWS_ROLE_ARN }}
          aws-region: us-east-1
      - name: Sube el jar
        run: aws s3 cp aula-virtual.jar "s3://${{ vars.DEPLOY_BUCKET }}/$KEY"
      - name: Despliega en el servidor
        env:
          INSTANCE_ID: ${{ vars.INSTANCE_ID }}
        run: |
          id=$(aws ssm send-command \
            --instance-ids "$INSTANCE_ID" \
            --document-name aula-virtual-deploy \
            --parameters "key=$KEY" \
            --comment "aula-virtual ${GITHUB_SHA::7}" \
            --query Command.CommandId --output text)
          status=Pending
          for _ in $(seq 1 60); do
            sleep 5
            status=$(aws ssm get-command-invocation --command-id "$id" --instance-id "$INSTANCE_ID" \
              --query Status --output text 2>/dev/null || echo Pending)
            case "$status" in Success|Failed|Cancelled|TimedOut) break ;; esac
          done
          aws ssm get-command-invocation --command-id "$id" --instance-id "$INSTANCE_ID" \
            --query '[StandardOutputContent,StandardErrorContent]' --output text
          test "$status" = Success
      - name: Responde a traves del balanceador
        run: |
          for _ in $(seq 1 12); do
            curl -sf "${{ vars.API_URL }}/actuator/health" && exit 0
            sleep 10
          done
          exit 1
```

Lo que no es obvio:

| Parte | Por qué |
|---|---|
| `services: postgres` | un Postgres de verdad junto al job. Sin él, `AulaVirtualApplicationTests` **se omite** y el build sale verde sin haber validado el esquema, que es justo lo que tumbaría la EC2. |
| `grep -q 'skipped="0"'` | comprueba que ese test **corrió**. Si mañana el contenedor no arranca, el job falla en vez de pasar en silencio. |
| `upload-artifact` / `download-artifact` | el jar que se despliega es **el mismo** que pasó los tests, no uno recompilado. `retention-days: 1` evita que se coma tu 1 GB de artifacts. |
| `releases/<sha>.jar` | cada versión queda en S3 con el commit en el nombre. Volver a una anterior es desplegar otro nombre (ver *Volver atrás a mano*). |
| `environment: demo` | es lo que pone `environment:demo` en el `sub` del token (14.4) y lo que aplica la regla «solo `main`». |
| `id-token: write` | permite al job pedir el token OIDC. Solo lo tiene `deploy`: el job de tests no puede entrar en AWS aunque quisiera. |
| `concurrency` | si pulsas el botón dos veces, el segundo espera al primero en vez de pisarlo. |
| El bucle de `get-command-invocation` | `send-command` vuelve al instante; el bucle espera a que `deploy.sh` termine (hasta 5 min). El `|| echo Pending` cubre los primeros segundos, en los que AWS todavía no conoce la invocación. |
| El `curl` final | `deploy.sh` ya comprobó la app en `localhost`. Esto comprueba el camino que usan los usuarios: DNS, certificado, ALB y *target group*. Tras un reinicio el ALB tarda unos 30 s en volver a marcar la instancia como *healthy* (2 comprobaciones cada 15 s, capítulo 08). |

- [ ] Haz commit y push a `main`. En **Actions** verás correr solo `test` (el push no despliega).

### 14.8 El workflow del front

En `aula-virtual-web`, crea `.github/workflows/web.yml`:

```yaml
name: Web

on:
  push:
    branches: [main]
  pull_request:
  workflow_dispatch:

permissions:
  contents: read

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: pnpm/action-setup@v6
      - uses: actions/setup-node@v7
        with:
          node-version: 24
          cache: pnpm
      - run: pnpm install --frozen-lockfile
      - run: pnpm lint
      - run: pnpm test
      - run: pnpm build

  deploy:
    if: github.event_name == 'workflow_dispatch'
    needs: test
    runs-on: ubuntu-latest
    environment: demo
    concurrency: deploy-demo-web
    permissions:
      contents: read
      id-token: write
    steps:
      - uses: actions/checkout@v7
      - uses: pnpm/action-setup@v6
      - uses: actions/setup-node@v7
        with:
          node-version: 24
          cache: pnpm
      - run: pnpm install --frozen-lockfile
      - run: pnpm build
        env:
          VITE_API_BASE_URL: ${{ vars.VITE_API_BASE_URL }}
      - name: La URL de la API quedo incrustada
        run: grep -rqF "${{ vars.VITE_API_BASE_URL }}" dist/assets
      - uses: aws-actions/configure-aws-credentials@v6
        with:
          role-to-assume: ${{ vars.AWS_ROLE_ARN }}
          aws-region: us-east-1
      - name: Sube a S3
        env:
          WEB: s3://${{ vars.WEB_BUCKET }}
        run: |
          aws s3 sync dist/assets "$WEB/assets" --delete --cache-control "public,max-age=31536000,immutable"
          aws s3 sync dist "$WEB" --delete --exclude "assets/*" --cache-control "no-cache"
      - name: Invalida CloudFront
        run: aws cloudfront create-invalidation --distribution-id "${{ vars.DISTRIBUTION_ID }}" --paths "/*"
```

`pnpm/action-setup` sin `version` lee la del campo `packageManager` de `package.json`, así que
CI usa exactamente tu pnpm. El `grep` es la comprobación del apartado 10.1 hecha
automáticamente: un front compilado sin `VITE_API_BASE_URL` no llega al bucket.

Aquí se compila dos veces (una en `test` y otra en `deploy`) y es a propósito: la
URL de la API se incrusta **al compilar**, y el job de tests no debe conocerla.

- [ ] Commit y push a `main`.

### 14.9 Desplegar

Siempre **primero el back y después el front**:

- [ ] **aula-virtual → Actions → API → Run workflow** (rama `main`) → espera a que termine en
      verde.
- [ ] **aula-virtual-web → Actions → Web → Run workflow**.

> **¿Por qué ese orden?** Hay usuarios con la pestaña abierta y el JavaScript viejo cargado.
> Durante un rato, el back nuevo recibe peticiones del front viejo. Si el back es compatible
> con el front anterior (un campo nuevo es opcional, un endpoint viejo sigue existiendo
> hasta la siguiente versión), el orden funciona. Al revés, el front nuevo llamaría a
> endpoints que todavía no existen.

## Volver atrás a mano

Si `deploy.sh` no pudo arrancar la versión nueva, ya volvió solo. Pero si la versión nueva
arranca y **funciona mal**, desde tu PowerShell (tu usuario sí puede, es administrador):

```powershell
aws s3 ls s3://aula-virtual-demo-deploy-<sufijo>/releases/
aws ssm send-command --instance-ids <instance-id> --document-name aula-virtual-deploy --parameters "key=releases/<sha-bueno>.jar"
```

El primer comando lista las versiones que hay, y el segundo despliega la que elijas. El
front no guarda versiones: para volver atrás, `git revert` y otro **Run workflow**.

## Comprueba que funciona

- [ ] Un push a `main` en cada repo ejecuta solo el job `test`, y termina en verde.
- [ ] En el log de `test` del back, el paso *El test de contexto corrio contra Postgres* pasa.
- [ ] **Run workflow** en el back termina en verde y el log de *Despliega en el servidor*
      acaba con `Desplegado releases/<sha>.jar`.
- [ ] En **Systems Manager → Run Command → Command history** aparece la ejecución con el
      comentario `aula-virtual <sha corto>`.
- [ ] **Run workflow** en el front, y `https://aula.<dominio>.pe` muestra el cambio tras un
      `Ctrl+F5`.
- [ ] **Prueba la vuelta atrás.** En una rama, rompe el arranque a propósito (por ejemplo, en
      `application-prod.yml` pon `spring.datasource.url: jdbc:postgresql://nada:5432/x`),
      súbela y lanza **Run workflow** eligiendo **esa rama**. Debe fallar *antes* de tocar
      AWS, con `Branch "…" is not allowed to deploy to demo`: es la regla del entorno. Ahora
      fusiónalo en `main` y lánzalo: el paso *Despliega en el servidor* termina en rojo, el
      log enseña el error de conexión y `https://api.<dominio>.pe/api/actuator/health`
      sigue respondiendo `UP` con la versión anterior. Deshaz el cambio con `git revert` y
      vuelve a desplegar.

## Si algo falla

- **`Could not assume role with OIDC: Not authorized to perform sts:AssumeRoleWithWebIdentity`**:
  el `sub` de la *trust policy* no coincide con el token. Revisa, letra a letra, el dueño y
  el nombre del repo, que diga `environment:demo` y que el job lleve `environment: demo`.
- **`Retry validateCredentials: attempt N of 12 failed: Credentials could not be loaded … from
  any providers`**: la acción no recibió ningún rol. Mira el bloque `with:` del paso en el
  log: si no aparece `role-to-assume`, `vars.AWS_ROLE_ARN` llegó vacía. Casi siempre es que
  se creó en **Environment secrets** en lugar de **Environment variables** (están en la
  misma página y los botones se parecen). `vars.` no ve los *secrets*: créalas como
  variables y borra los *secrets*. Si `role-to-assume` sí aparece, lo que falta es
  `id-token: write` en el job.
- **`./gradlew: Permission denied`**: falta el `git update-index --chmod=+x gradlew` del
  apartado 14.7.
- **El paso *El test de contexto corrio contra Postgres* falla**: el test se omitió. Casi
  siempre es que el `POSTGRES_DB` del servicio no es `aula_virtual_test`, o que las
  credenciales no son `aula_virtual`/`aula_virtual`, que es lo que espera
  `application-test.yml`.
- **`InvalidInstanceId`** en `send-command`: la instancia está parada o el agente de SSM no
  está conectado. Es lo mismo que te impediría entrar por Session Manager: capítulo 07,
  *Si algo falla*.
- **`AccessDeniedException` … `ssm:SendCommand` … `document/AWS-RunShellScript`**: el
  workflow está usando el documento de AWS y no el tuyo. Revisa `--document-name`.
- **`InvalidParameters`** en `send-command`: la `key` no cumple el patrón del documento. El
  patrón exige los 40 caracteres del SHA completo, no los 7 del corto.
- ***Despliega en el servidor* en rojo sin más salida**: el comando duró más de 5 minutos.
  Mira **Run Command → Command history** en la consola, o `journalctl -u aula-virtual` por
  Session Manager.
- **Tests del front con `Test timed out in 5000ms`**: en una máquina cargada, algunas
  pruebas de pantalla (con antd y MSW) pasan de los 5 s que da Vitest por defecto. Pasa
  también en local si corren muchas a la vez. Súbelo en `vitest.config.ts`
  (`test: { testTimeout: 15000 }`) en vez de reintentar el job hasta que salga verde.
- **`s3 sync` da `AccessDenied` en `ListObjectsV2`**: falta `s3:ListBucket`, que va sobre el
  ARN del bucket **sin** `/*`.
- **El front sigue viejo**: la invalidación tarda un par de minutos. Míralo en **CloudFront →
  tu distribución → Invalidations**.

Siguiente: vuelve a [13 · Apagarlo todo](13-apagado.md) cuando termines. Allí están también
las piezas de este capítulo.
