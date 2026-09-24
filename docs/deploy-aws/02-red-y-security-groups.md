# 02 · Red y Security Groups

## Qué vas a construir

Tres *Security Groups* que deciden quién puede hablar con quién:

```
internet ──443/80──► [sg-alb] balanceador ──8080──► [sg-app] EC2 ──5432──► [sg-db] RDS
```

## Qué aprendes

**VPC (Virtual Private Cloud)** es tu red privada dentro de AWS. Se divide en
**subnets**, y cada subnet vive en una **zona de disponibilidad** (AZ): un centro de datos
físico distinto dentro de la región (`us-east-1a`, `us-east-1b`…).

- Una subnet **pública** tiene ruta a internet a través de un *Internet Gateway*: lo que
  vive ahí puede tener IP pública.
- Una subnet **privada** no tiene esa ruta: nadie de fuera puede llegar.

Cada cuenta trae una **VPC por defecto** en cada región, con una subnet pública por AZ.
Para una demo es suficiente y es la que usarás. (En producción se crea una VPC propia con
subnets privadas para la base y la aplicación; es un buen ejercicio para el capítulo 12.)

Un **Security Group (SG)** es un cortafuegos que se pega a un recurso. Dos detalles que
lo cambian todo:

- Por defecto **deniega toda entrada** y permite toda salida. Solo abres lo que necesitas.
- Una regla puede permitir tráfico **desde otro SG** en vez de desde una IP. Así
  «la base solo acepta al servidor» sigue siendo cierto aunque el servidor cambie de IP o
  lo recrees. Es lo que vas a hacer aquí.

## Pasos en la consola

Comprueba que estás en **us-east-1** (arriba a la derecha).

### 2.1 Mira la VPC por defecto

- [ ] Busca **VPC** → **Your VPCs**. Hay una con **Default VPC = Yes**. Anota su ID
      (`vpc-…`).
- [ ] **Subnets**: verás seis, una por AZ, todas de la VPC por defecto. No toques nada:
      solo es para que sepas que existen.

### 2.2 `sg-alb`: el balanceador

- [ ] **EC2** → menú izquierdo **Network & Security → Security Groups** → **Create security
      group**.
- [ ] **Name**: `aula-virtual-demo-alb`. **Description**: `ALB publico de la demo`.
      **VPC**: la de por defecto.
- [ ] **Inbound rules** → **Add rule**:
  - Type `HTTPS`, Source `Anywhere-IPv4` (`0.0.0.0/0`).
  - Type `HTTP`, Source `Anywhere-IPv4` (`0.0.0.0/0`).
- [ ] Deja **Outbound** como está → **Create security group**.

> **¿Por qué abrir el 80 si todo será HTTPS?** Porque quien escriba
> `http://api.<dominio>.pe` tiene que llegar a algún sitio para que le redirijan a
> `https://`. El balanceador hará esa redirección (capítulo 08).

### 2.3 `sg-app`: el servidor

- [ ] **Create security group** → **Name**: `aula-virtual-demo-app`. **Description**:
      `Servidor de la API`. VPC por defecto.
- [ ] **Inbound rules** → **Add rule**: Type `Custom TCP`, Port `8080`, Source
      **Custom** → escribe `aula-virtual-demo-alb` y elige el SG que aparece.
- [ ] **Create security group**.

> **¿Y el puerto 22 (SSH)?** No se abre. Entrarás al servidor con **Session Manager**
> (capítulo 07), que funciona sin ningún puerto de entrada: el propio servidor abre la
> conexión hacia AWS. Un servidor sin puertos de administración abiertos no se puede
> atacar por ahí.

### 2.4 `sg-db`: la base de datos

- [ ] **Create security group** → **Name**: `aula-virtual-demo-db`. **Description**:
      `PostgreSQL de la demo`. VPC por defecto.
- [ ] **Inbound rules** → Type `PostgreSQL` (puerto 5432), Source **Custom** →
      `aula-virtual-demo-app`.
- [ ] **Create security group**.

## Con la CLI (opcional)

```powershell
$vpc = aws ec2 describe-vpcs --filters Name=is-default,Values=true --query 'Vpcs[0].VpcId' --output text

$alb = aws ec2 create-security-group --group-name aula-virtual-demo-alb --description "ALB publico de la demo" --vpc-id $vpc --query GroupId --output text
aws ec2 authorize-security-group-ingress --group-id $alb --protocol tcp --port 443 --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --group-id $alb --protocol tcp --port 80 --cidr 0.0.0.0/0

$app = aws ec2 create-security-group --group-name aula-virtual-demo-app --description "Servidor de la API" --vpc-id $vpc --query GroupId --output text
aws ec2 authorize-security-group-ingress --group-id $app --protocol tcp --port 8080 --source-group $alb

$db = aws ec2 create-security-group --group-name aula-virtual-demo-db --description "PostgreSQL de la demo" --vpc-id $vpc --query GroupId --output text
aws ec2 authorize-security-group-ingress --group-id $db --protocol tcp --port 5432 --source-group $app
```

## Comprueba que funciona

En **EC2 → Security Groups**, abre cada uno y mira la pestaña **Inbound rules**:

- [ ] `aula-virtual-demo-alb`: 443 y 80 desde `0.0.0.0/0`.
- [ ] `aula-virtual-demo-app`: 8080 desde `sg-…` (el del ALB). **Nada más**.
- [ ] `aula-virtual-demo-db`: 5432 desde `sg-…` (el de la app). **Nada más**.

## Si algo falla

- **No aparece el SG al escribir en Source**: está en otra VPC o en otra región.
  Comprueba las dos.
- **Pusiste una IP en vez de un SG**: borra la regla y créala otra vez eligiendo
  **Custom** y el nombre del SG.

Siguiente: [03 · Buckets S3](03-buckets-s3.md)
