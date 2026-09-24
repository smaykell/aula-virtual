# 04 · Base de datos RDS

## Qué vas a construir

Un PostgreSQL gestionado, **sin acceso desde internet**, al que solo llega el servidor.

## Qué aprendes

**RDS (Relational Database Service)** te da una base de datos sin administrar el servidor
donde corre: AWS instala, parchea, hace copias de seguridad y te da un *endpoint* (un
nombre DNS) al que conectarte. Tú eliges motor, versión, tamaño y red.

Decisiones que vas a tomar y su porqué:

- **Sin acceso público.** La base solo tendrá IP privada. Aunque alguien adivinara la
  contraseña, no hay camino de red desde internet. Junto con `sg-db`, que solo acepta a
  `sg-app`, son dos barreras independientes.
- **Single-AZ.** Una sola instancia. *Multi-AZ* mantiene una réplica en otra zona que toma
  el relevo si la primera cae, y duplica el precio. Para una demo sobra.
- **Contraseña gestionada por ti**, no por Secrets Manager. Secrets Manager la *rota*
  automáticamente, y la app leería la antigua hasta que la reiniciaras. Para una demo
  de pocos días, una contraseña fija en Parameter Store (capítulo 06) es más simple.

La app crea el esquema sola: al arrancar, **Flyway** aplica todas las migraciones de
`src/main/resources/db/migration` sobre la base vacía. No tienes que ejecutar ningún SQL.

## Pasos en la consola

### 4.1 Genera la contraseña

Genera una contraseña **solo con letras y números** (así no dará problemas en ningún
fichero de configuración) y guárdala en tu gestor de contraseñas:

```powershell
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | ForEach-Object { [char]$_ })
```

### 4.2 Crea la base

- [ ] Busca **RDS** → **Databases** → **Create database**.
- [ ] **Choose a database creation method**: **Full configuration** (a veces llamado
      *Standard create*). El modo fácil esconde justo lo que quieres aprender.
- [ ] **Engine type**: PostgreSQL. **Engine version**: la **18.x** más reciente de la lista
      (el proyecto usa PostgreSQL 18 en local).
- [ ] **Templates**: **Free tier** si aparece; si no, **Sandbox** o **Dev/Test**.
- [ ] **Availability and durability**: **Single-AZ DB instance deployment**.
- [ ] **DB instance identifier**: `aula-virtual-demo`.
- [ ] **Master username**: `aula_virtual`.
- [ ] **Credentials management**: **Self managed** → pega la contraseña de 4.1 en
      **Master password** y **Confirm**.
- [ ] **Instance configuration**: **Burstable classes** → `db.t4g.micro`.
- [ ] **Storage**: `gp3`, **20 GiB**. Despliega **Storage autoscaling** y desmárcalo.
- [ ] **Connectivity**:
  - **Compute resource**: *Don't connect to an EC2 compute resource* (los SG ya los
    preparaste tú en el capítulo 02).
  - **VPC**: la de por defecto.
  - **Public access**: **No**.
  - **VPC security group**: *Choose existing* → quita `default` y añade
    `aula-virtual-demo-db`.
  - **Availability Zone**: No preference.
- [ ] **Monitoring**: si aparece **Database Insights**, elige *Standard*; desmarca
      **Enhanced Monitoring**.
- [ ] Despliega **Additional configuration**:
  - **Initial database name**: `aula_virtual`. **Imprescindible**: si lo dejas vacío, RDS
    no crea ninguna base y la app no arranca.
  - **Backup retention period**: `1` día.
  - **Deletion protection**: desmarcado (querrás borrarla en el capítulo 13).
- [ ] Mira el coste estimado al final de la página y **Create database**.

Tarda unos 10 minutos en pasar a **Available**. Mientras, puedes adelantar el capítulo 05.

### 4.3 Anota el endpoint

- [ ] Abre `aula-virtual-demo` → pestaña **Connectivity & security** → copia el
      **Endpoint** (`aula-virtual-demo.xxxx.us-east-1.rds.amazonaws.com`) a tu tabla.

## Con la CLI (opcional)

```powershell
$db = aws ec2 describe-security-groups --group-names aula-virtual-demo-db --query 'SecurityGroups[0].GroupId' --output text

aws rds create-db-instance `
  --db-instance-identifier aula-virtual-demo `
  --engine postgres --engine-version 18 `
  --db-instance-class db.t4g.micro `
  --allocated-storage 20 --storage-type gp3 `
  --master-username aula_virtual --master-user-password "<contraseña>" `
  --db-name aula_virtual `
  --vpc-security-group-ids $db `
  --no-publicly-accessible --no-multi-az `
  --backup-retention-period 1 --no-deletion-protection

aws rds wait db-instance-available --db-instance-identifier aula-virtual-demo
aws rds describe-db-instances --db-instance-identifier aula-virtual-demo --query 'DBInstances[0].Endpoint.Address' --output text
```

## Comprueba que funciona

- [ ] En **RDS → Databases**, `aula-virtual-demo` está **Available**.
- [ ] En **Connectivity & security**: **Publicly accessible = No**, y el SG es
      `aula-virtual-demo-db`.
- [ ] En **Configuration**: **DB name = aula_virtual**.

Todavía no puedes conectarte desde tu equipo, y así debe ser. La conexión real la
comprobarás desde el servidor en el capítulo 07.

## Si algo falla

- **No aparece la versión 18**: elige la 17 más reciente. Las migraciones del proyecto
  funcionan igual en las dos.
- **`db.t4g.micro` no aparece**: marca **Include previous generation classes** o elige
  `db.t3.micro`.
- **Olvidé el nombre de la base inicial**: no se puede añadir después desde la consola.
  Lo más rápido en una demo es borrar la instancia y crearla otra vez (sin *final
  snapshot*).

Siguiente: [05 · Rol IAM de la instancia](05-rol-iam-de-la-instancia.md)
