# 07 · Servidor EC2

## Qué vas a construir

Un servidor Linux con Java 21 que arranca la API como servicio del sistema, lee su
configuración de Parameter Store y se actualiza con un comando.

```
tu equipo ── bootJar ──► S3 deploy ──► EC2: deploy.sh ──► systemd reinicia la app
                                         │
                                         └─ load-env.sh ◄── Parameter Store
```

## Qué aprendes

**EC2** alquila máquinas virtuales por segundos. Al lanzar una eliges:

- **AMI**: la imagen del disco (el sistema operativo). Usarás **Amazon Linux 2023**, la
  distribución de AWS: trae la CLI de AWS y el agente de Session Manager ya instalados.
- **Tipo de instancia**: CPU y memoria. `t4g.small` tiene 2 vCPU y 2 GB, con procesador
  **ARM (Graviton)**, un ~20 % más barato que el equivalente Intel. Java no nota la
  diferencia: el mismo `.jar` corre en los dos.
- **Red**: VPC, subnet, IP pública y Security Group.
- **Rol IAM**: el del capítulo 05.

**Session Manager** (parte de *Systems Manager*) te da una terminal en el navegador sin
SSH, sin claves `.pem` y sin puertos abiertos. El agente del servidor abre la conexión
*hacia* AWS, y AWS registra quién entró y cuándo.

**systemd** es quien arranca los servicios en Linux. Una *unidad* describe cómo lanzar la
app, con qué usuario, con qué variables de entorno y qué hacer si se cae.

## Pasos en la consola

### 7.1 Lanza la instancia

- [ ] **EC2** → **Instances** → **Launch instances**.
- [ ] **Name**: `aula-virtual-demo-app`.
- [ ] **Application and OS Images**: **Amazon Linux** → *Amazon Linux 2023 AMI* →
      **Architecture: 64-bit (Arm)**.
- [ ] **Instance type**: `t4g.small`.
- [ ] **Key pair**: *Proceed without a key pair*. No hace falta: entrarás con Session
      Manager.
- [ ] **Network settings** → **Edit**:
  - **VPC**: la de por defecto. **Subnet**: No preference.
  - **Auto-assign public IP**: **Enable**.
  - **Firewall**: *Select existing security group* → `aula-virtual-demo-app`.

  > **¿IP pública si nadie entra desde fuera?** Para poder *salir*: el servidor tiene que
  > hablar con Session Manager, S3, Parameter Store y los repositorios de paquetes. Sin IP
  > pública ni *NAT Gateway* (unos 33 USD al mes) no llegaría a ninguno. La entrada sigue
  > cerrada: el SG solo deja pasar el 8080 desde el balanceador.
- [ ] **Configure storage**: 8 GiB `gp3` (lo que viene) basta.
- [ ] **Advanced details** → **IAM instance profile**: `aula-virtual-demo-ec2`.
      **Metadata version**: *V2 only (token required)* (suele venir ya así).
- [ ] **Launch instance**. Anota el **Instance ID** (`i-…`).

### 7.2 Entra con Session Manager

Espera 2–3 minutos a que **Status check** diga *2/2 checks passed*.

- [ ] Selecciona la instancia → **Connect** → pestaña **Session Manager** → **Connect**.

Se abre una terminal con el usuario `ssm-user`. Hazte root para la preparación:

```bash
sudo -i
```

### 7.3 Instala Java y crea el usuario del servicio

```bash
dnf install -y java-21-amazon-corretto-headless
java -version
useradd --system --home-dir /opt/aula-virtual --shell /sbin/nologin aula
mkdir -p /opt/aula-virtual
chown aula:aula /opt/aula-virtual
```

> **¿Por qué un usuario `aula`?** Si alguien consiguiera ejecutar código a través de la
> app, lo haría como `aula`, que no puede instalar nada, ni leer `/etc/aula-virtual.env`,
> ni tocar el sistema. Nunca se ejecuta un servicio como root.

### 7.4 El script que carga la configuración

Crea `/opt/aula-virtual/load-env.sh`:

```bash
cat > /opt/aula-virtual/load-env.sh <<'EOF'
#!/bin/bash
set -euo pipefail
umask 077
aws ssm get-parameters-by-path \
  --region us-east-1 \
  --path /aula-virtual/demo \
  --with-decryption \
  --query 'Parameters[].[Name,Value]' \
  --output text \
  | awk -F'\t' '{ sub(".*/", "", $1); print $1 "=" $2 }' \
  > /etc/aula-virtual.env.tmp
mv /etc/aula-virtual.env.tmp /etc/aula-virtual.env
EOF
chmod 750 /opt/aula-virtual/load-env.sh
```

Lee todos los parámetros de `/aula-virtual/demo`, les quita la ruta (`/aula-virtual/demo/DB_URL`
→ `DB_URL`) y los escribe como `DB_URL=…` en un fichero que **solo root** puede leer
(`umask 077`). Lo hace en cada arranque: si cambias un parámetro, basta con reiniciar el
servicio.

Pruébalo ya:

```bash
/opt/aula-virtual/load-env.sh && cut -d= -f1 /etc/aula-virtual.env
```

Deben salir los 10 nombres (solo los nombres; `cut` no enseña los valores).

### 7.5 El servicio de systemd

```bash
cat > /etc/systemd/system/aula-virtual.service <<'EOF'
[Unit]
Description=Aula Virtual API
Wants=network-online.target
After=network-online.target

[Service]
User=aula
WorkingDirectory=/opt/aula-virtual
ExecStartPre=+/opt/aula-virtual/load-env.sh
EnvironmentFile=-/etc/aula-virtual.env
Environment=STORAGE_ENDPOINT=
Environment=STORAGE_ACCESS_KEY=
Environment=STORAGE_SECRET_KEY=
Environment=STORAGE_PATH_STYLE=false
Environment=SERVER_FORWARD_HEADERS_STRATEGY=framework
ExecStart=/usr/bin/java -XX:MaxRAMPercentage=60 -jar /opt/aula-virtual/app.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable aula-virtual
```

Línea a línea, lo que no es obvio:

| Línea | Por qué |
|---|---|
| `ExecStartPre=+…load-env.sh` | el `+` lo ejecuta como root (hace falta para escribir en `/etc`), aunque la app corra como `aula`. |
| `EnvironmentFile=-…` | systemd lee el fichero como root y pasa sus valores a la app. El `-` evita que falle si todavía no existe. |
| `STORAGE_ENDPOINT=` vacío | **definida y vacía** hace que la app hable con AWS S3 en vez de con MinIO. Ausente, usaría `http://localhost:9000`. |
| `STORAGE_ACCESS_KEY=` y `STORAGE_SECRET_KEY=` vacías | la app usa el **rol IAM** del capítulo 05. Ausentes, usaría las claves de MinIO y S3 las rechazaría. |
| `STORAGE_PATH_STYLE=false` | AWS usa `bucket.s3.amazonaws.com`; MinIO necesitaba `host/bucket`. |
| `SERVER_FORWARD_HEADERS_STRATEGY=framework` | detrás del balanceador la app recibe HTTP, pero el usuario usó HTTPS. Con esto lee las cabeceras `X-Forwarded-*` y, por ejemplo, Swagger genera URLs `https://`. |
| `-XX:MaxRAMPercentage=60` | deja a la JVM hasta el 60 % de los 2 GB. Sin ello solo usaría el 25 %. |
| `SuccessExitStatus=143` | Java termina con 143 al recibir `SIGTERM`; así systemd no lo toma por un fallo al parar. |
| `Restart=on-failure` | si la app se cae, systemd la levanta a los 10 segundos. |

Esas cinco variables van aquí y no en Parameter Store porque Parameter Store no admite
valores vacíos, y porque no son secretas ni cambian entre despliegues.

### 7.6 El script de despliegue

```bash
cat > /opt/aula-virtual/deploy.sh <<'EOF'
#!/bin/bash
set -euo pipefail
BUCKET=aula-virtual-demo-deploy-<sufijo>
aws s3 cp "s3://$BUCKET/aula-virtual.jar" /opt/aula-virtual/app.jar.new
chown aula:aula /opt/aula-virtual/app.jar.new
mv /opt/aula-virtual/app.jar.new /opt/aula-virtual/app.jar
systemctl restart aula-virtual
EOF
chmod 750 /opt/aula-virtual/deploy.sh
```

**Edita `<sufijo>`** dentro del script (`nano /opt/aula-virtual/deploy.sh`).

### 7.7 Primer despliegue

En **tu equipo**, desde la raíz del repo:

```powershell
.\gradlew.bat bootJar
aws s3 cp build\libs\aula-virtual-0.0.1-SNAPSHOT.jar s3://aula-virtual-demo-deploy-<sufijo>/aula-virtual.jar
```

En la **terminal del servidor**:

```bash
/opt/aula-virtual/deploy.sh
journalctl -u aula-virtual -f
```

`journalctl -f` muestra el log en directo (sal con `Ctrl+C`). La primera vez, verás a
Flyway crear el esquema. Busca estas líneas:

```
Successfully applied NN migrations to schema "public"
Tomcat started on port 8080 (http) with context path '/api'
Started AulaVirtualApplication in N seconds
```

A partir de ahora, **desplegar una versión nueva** son esos mismos tres comandos: `bootJar`,
`aws s3 cp` y `deploy.sh`.

## Comprueba que funciona

En la terminal del servidor:

```bash
curl -s localhost:8080/api/actuator/health
```

- [ ] Responde `{"status":"UP"}`.
- [ ] `systemctl status aula-virtual` dice **active (running)**.
- [ ] Reinicia el servidor entero (`reboot`), vuelve a conectar por Session Manager a los
      dos minutos y repite el `curl`: la app arranca sola.
- [ ] Desde tu equipo, `http://<ip-publica>:8080/api/actuator/health` **no** responde (se
      queda colgado). Es lo correcto: el SG solo deja entrar al balanceador.

## Si algo falla

Casi todo se diagnostica con `journalctl -u aula-virtual -n 100 --no-pager`.

- **La pestaña Session Manager dice que la instancia no está conectada**: espera 5 minutos;
  si sigue, revisa que el rol lleve `AmazonSSMManagedInstanceCore` y que la instancia
  tenga IP pública. Cambiar el rol de una instancia ya lanzada: **Actions → Security →
  Modify IAM role**, y luego **Reboot**.
- **`AccessDeniedException` en `load-env.sh`**: la política del rol no cubre la ruta de
  los parámetros, o la `<cuenta>` del ARN no es la tuya.
- **`Connection to …rds.amazonaws.com:5432 refused` o *timeout***: la base no acepta al
  servidor. Revisa que `sg-db` permita el 5432 desde `aula-virtual-demo-app` y que la
  instancia lleve ese SG. Prueba la red sin la app:

  ```bash
  timeout 3 bash -c '</dev/tcp/<endpoint-rds>/5432' && echo "puerto abierto"
  ```
- **`FATAL: database "aula_virtual" does not exist`**: faltó el *Initial database name* del
  capítulo 04.
- **`password authentication failed`**: `DB_PASSWORD` no coincide con la de RDS. Corrígelo en
  Parameter Store y `systemctl restart aula-virtual`.
- **`app.security.jwt.secret` … `must not be blank`**: falta `JWT_SECRET` o no se cargó el
  fichero de entorno. Mira `cut -d= -f1 /etc/aula-virtual.env`.
- **`Unable to load credentials`** o **`403`** de S3 al subir un archivo: el servicio no
  tiene `STORAGE_ACCESS_KEY=` vacía (apartado 7.5) o el rol no está enganchado a la
  instancia.

Siguiente: [08 · HTTPS: certificado, balanceador y dominio](08-https-alb-y-dominio.md)
