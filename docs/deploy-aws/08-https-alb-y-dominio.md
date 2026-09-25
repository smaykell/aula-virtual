# 08 · HTTPS: certificado, balanceador y dominio

## Qué vas a construir

`https://api.<dominio>.pe` sirviendo la API con un certificado válido:

```
navegador ──HTTPS 443──► ALB (certificado ACM) ──HTTP 8080──► EC2
          ──HTTP 80───► ALB: «vete a https://»
```

## Qué aprendes

**ACM (AWS Certificate Manager)** emite certificados TLS **gratis** y los renueva solo. Para
emitirlo tiene que comprobar que el dominio es tuyo: te pide crear un registro `CNAME`
concreto en tu DNS (*validación DNS*). Mientras ese registro exista, además, renueva el
certificado cada año sin que hagas nada.

Un certificado de ACM no se puede descargar: solo se engancha a servicios de AWS (ALB,
CloudFront…). Por eso el HTTPS termina en el balanceador y no en el servidor.

**ALB (Application Load Balancer)** recibe el tráfico público y lo reparte entre
*targets*. Tres piezas:

- **Listener**: un puerto donde escucha (443, 80) y qué hace con lo que llega.
- **Target group**: el grupo de destinos (tu EC2 en el puerto 8080).
- **Health check**: el ALB llama cada pocos segundos a una ruta del target. Si falla,
  deja de mandarle tráfico. La app ya tiene la ruta ideal, `/api/actuator/health`, que
  es pública y comprueba también la base de datos.

Con una sola instancia no hay nada que repartir, pero el ALB sigue siendo lo que daría
HTTPS a varias instancias, y la pieza que verías en cualquier empresa. Es el recurso más
caro de la demo (~0,60 USD/día) y **no se puede apagar ni pausar**: cobra mientras exista.
Cuando termines, bórralo el primero (capítulo 13).

Tu DNS está **fuera de AWS** (en tu proveedor del `.pe`), así que todos los registros los
crearás tú a mano allí. Es más trabajo que con Route 53, y también más instructivo: ves
exactamente qué registros hacen falta.

## Pasos en la consola

### 8.1 Pide el certificado

Un solo certificado para los dos nombres: el ALB usará `api` y CloudFront, `aula`
(capítulo 10). Por eso se pide en **us-east-1**.

- [ ] Busca **Certificate Manager** (región us-east-1) → **Request** → **Request a public
      certificate** → **Next**.
- [ ] **Fully qualified domain name**: `aula.<dominio>.pe` → **Add another name to this
      certificate** → `api.<dominio>.pe`.
- [ ] **Allow export**: Disable. **Validation method**: **DNS validation**. **Key
      algorithm**: RSA 2048.
- [ ] **Request**.

El certificado queda en **Pending validation**. Ábrelo: en **Domains** aparecen dos
filas, cada una con un **CNAME name** y un **CNAME value**, más o menos así:

| CNAME name | CNAME value |
|---|---|
| `_3f2a….aula.<dominio>.pe.` | `_9c1b….xxxx.acm-validations.aws.` |
| `_7d4e….api.<dominio>.pe.` | `_2a8f….xxxx.acm-validations.aws.` |

### 8.2 Crea los registros de validación en tu DNS

En el panel de tu proveedor DNS, crea **dos registros CNAME**, uno por fila.

> **La trampa más común:** casi todos los paneles añaden tu dominio al final del nombre
> solos. Si pegas `_3f2a….aula.<dominio>.pe.`, el registro acaba siendo
> `_3f2a….aula.<dominio>.pe.<dominio>.pe` y ACM no lo encuentra nunca. En el campo
> **nombre/host** escribe solo la parte anterior a tu dominio: `_3f2a….aula`. En el
> **valor/destino**, pega el valor completo (con o sin el punto final, según acepte el
> panel).

> Si tu DNS está en **Cloudflare**, pon esos registros en modo **DNS only** (nube gris), no
> *Proxied*.

Comprueba desde tu equipo que el registro ya se ve en internet:

```powershell
Resolve-DnsName -Type CNAME _3f2a….aula.<dominio>.pe
```

- [ ] Los dos registros resuelven al valor de ACM.
- [ ] En unos minutos (a veces hasta una hora) el certificado pasa a **Issued**.

**No borres esos CNAME nunca** mientras uses el certificado: son los que permiten
renovarlo.

### 8.3 El target group

- [ ] **EC2** → menú izquierdo **Load Balancing → Target Groups** → **Create target group**.
- [ ] **Target type**: Instances. **Name**: `aula-virtual-demo-app`.
- [ ] **Protocol : Port**: HTTP : `8080`. **VPC**: la de por defecto. **Protocol version**:
      HTTP1.
- [ ] **Health checks**: protocolo HTTP, **path** `/api/actuator/health`.
  - Despliega **Advanced health check settings**: **Healthy threshold** `2`, **Interval**
    `15` segundos, **Success codes** `200`.
- [ ] **Next** → marca tu instancia `aula-virtual-demo-app`, puerto `8080` → **Include as
      pending below** → **Create target group**.

### 8.4 El balanceador

- [ ] **Load Balancers** → **Create load balancer** → **Application Load Balancer** →
      **Create**.
- [ ] **Name**: `aula-virtual-demo-alb`. **Scheme**: Internet-facing. **IP address type**:
      IPv4.
- [ ] **Network mapping**: VPC por defecto; marca **dos** zonas, por ejemplo `us-east-1a` y
      `us-east-1b`. **Una de ellas tiene que ser la zona de tu instancia** (la ves en la
      columna *Availability Zone* de la lista de instancias).

  > **¿Por qué dos zonas si hay una instancia?** Es una exigencia del ALB: siempre vive en
  > al menos dos zonas, para seguir en pie si una cae.
- [ ] **Security groups**: quita `default` y elige `aula-virtual-demo-alb`.
- [ ] **Listeners and routing**: cambia el listener a **HTTPS : 443** → **Default action**:
      forward to `aula-virtual-demo-app`.
- [ ] **Secure listener settings**: **Default SSL/TLS server certificate** → *From ACM* →
      el certificado del paso 8.1. **Security policy**: la recomendada que viene.
- [ ] **Create load balancer**.

Ahora añade la redirección de HTTP a HTTPS:

- [ ] Abre el balanceador → pestaña **Listeners and rules** → **Add listener**.
- [ ] **Protocol : Port**: HTTP : 80. **Default action**: **Redirect to URL** → **URI
      parts** → Protocol `HTTPS`, Port `443`, **Status code** `301 - Permanently moved` →
      **Add**.

Espera a que el **State** del balanceador sea **Active** (2–3 minutos) y copia su **DNS
name** (`aula-virtual-demo-alb-….us-east-1.elb.amazonaws.com`) a tu tabla.

### 8.5 El nombre `api` en tu DNS

En tu proveedor DNS:

- [ ] Registro **CNAME**: nombre `api`, valor el **DNS name** del balanceador.

> **¿Por qué CNAME y no un registro A con la IP?** Las IP del balanceador **cambian** sin
> avisar: AWS las rota. Un CNAME apunta al nombre, y el nombre siempre resuelve a las IP
> buenas.

## Con la CLI (opcional)

La validación del certificado sigue siendo a mano en tu DNS. El resto:

```powershell
$vpc = aws ec2 describe-vpcs --filters Name=is-default,Values=true --query 'Vpcs[0].VpcId' --output text
$tg = aws elbv2 create-target-group --name aula-virtual-demo-app --protocol HTTP --port 8080 --vpc-id $vpc `
  --health-check-path /api/actuator/health --health-check-interval-seconds 15 --healthy-threshold-count 2 `
  --query 'TargetGroups[0].TargetGroupArn' --output text
aws elbv2 register-targets --target-group-arn $tg --targets Id=<instance-id>

$subnets = aws ec2 describe-subnets --filters Name=vpc-id,Values=$vpc Name=availability-zone,Values=us-east-1a,us-east-1b --query 'Subnets[].SubnetId' --output text
$sg = aws ec2 describe-security-groups --group-names aula-virtual-demo-alb --query 'SecurityGroups[0].GroupId' --output text
$alb = aws elbv2 create-load-balancer --name aula-virtual-demo-alb --subnets $subnets.Split() --security-groups $sg `
  --query 'LoadBalancers[0].LoadBalancerArn' --output text

aws elbv2 create-listener --load-balancer-arn $alb --protocol HTTPS --port 443 `
  --certificates CertificateArn=<arn-del-certificado> --default-actions Type=forward,TargetGroupArn=$tg
aws elbv2 create-listener --load-balancer-arn $alb --protocol HTTP --port 80 `
  --default-actions 'Type=redirect,RedirectConfig={Protocol=HTTPS,Port=443,StatusCode=HTTP_301}'
```

(Si tu instancia no está en `us-east-1a` ni en `us-east-1b`, cambia esas zonas.)

## Comprueba que funciona

- [ ] **Target Groups → aula-virtual-demo-app → Targets**: la instancia está **healthy**.
- [ ] Desde tu equipo:

```powershell
curl.exe -s https://api.<dominio>.pe/api/actuator/health
curl.exe -sI http://api.<dominio>.pe/api/actuator/health
```

  El primero responde `{"status":"UP"}`; el segundo, `301` con `Location: https://…`.
- [ ] `https://api.<dominio>.pe/api/swagger-ui.html` abre Swagger en el navegador, con el
      candado.

> **Hazlo ahora, no al final.** Desde este momento la API está en internet, y la contraseña
> inicial del superadmin (`Superadmin.2026`) está escrita en el README del repositorio.
> En Swagger: `POST /auth/login` con `superadmin` / `Superadmin.2026` → copia el `token`
> → botón **Authorize** → pégalo → `POST /me/$changePassword` con la contraseña actual y
> una nueva y larga.

- [ ] Contraseña del superadmin cambiada.

## Si algo falla

- **El certificado sigue en *Pending validation* tras una hora**: casi siempre es el
  dominio duplicado en el nombre del CNAME (ver 8.2). Compruébalo con `Resolve-DnsName`.
- **Target *unhealthy* con *Request timed out***: el SG de la app no deja entrar al del
  ALB en el 8080 (capítulo 02).
- **Target *unhealthy* con *Health checks failed with these codes: [404]***: la ruta del
  health check no lleva `/api` delante.
- **Target *unhealthy* con *[503]***: la app está arriba pero la base no responde; mira
  `journalctl` en el servidor.
- **`502 Bad Gateway`** en el navegador: el ALB no tiene ningún target sano. Es el mismo
  problema que los anteriores.
- **`503 Service Temporarily Unavailable`** en el navegador, con cabecera
  `Server: awselb/2.0` (`curl.exe -sI`): no es el *[503]* del health check. Lo responde el
  propio ALB porque no tiene **ningún target registrado** al que mandar la petición. En
  **Targets**, o la lista está vacía (faltó **Include as pending below** en 8.3) o la
  instancia sale `unused` porque su zona no está marcada en el balanceador (**Network
  mapping → Edit subnets**, ver 8.4).
- **El navegador dice que el certificado no es válido para `api…`**: el CNAME apunta al
  balanceador, pero el certificado no incluye ese nombre. Revisa el paso 8.1.
- **`curl` no resuelve el nombre**: el DNS tarda en propagarse. Prueba con
  `Resolve-DnsName api.<dominio>.pe` hasta que devuelva el nombre del ALB.
- **`DNS_PROBE_FINISHED_NXDOMAIN` en el navegador, pero
  `Resolve-DnsName api.<dominio>.pe -Server 8.8.8.8` sí responde**: el registro está bien.
  El DNS de tu proveedor de internet preguntó antes de que existiera y guardó el «no
  existe» durante el tiempo que marca el último número del SOA (7200 s = 2 horas). Espera,
  o pon `8.8.8.8` y `1.1.1.1` como DNS de tu conexión, y luego `ipconfig /flushdns`.

Siguiente: [09 · Correo con SES](09-correo-ses.md)
