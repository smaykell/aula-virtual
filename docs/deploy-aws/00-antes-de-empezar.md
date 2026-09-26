# 00 · Antes de empezar

## Qué vas a hacer

Nada en AWS todavía. Tres cosas: comprobar que tus créditos siguen vivos, entender lo que
va a costar, y preparar la tabla donde anotarás los valores que irán apareciendo.

## 1. Comprueba tus créditos (importante)

- [ ] Entra en la consola → menú de tu cuenta (arriba a la derecha) → **Billing and Cost
      Management** → **Credits**.
- [ ] Mira el **saldo** y la **fecha de caducidad**.

Si tu cuenta se creó después del 15 de julio de 2025, está en el sistema nuevo de AWS:
eliges entre un **Free plan** y un **Paid plan**, y los dos empiezan con créditos (100 USD,
y hasta 200 si completas las actividades de bienvenida).

- En el **Free plan**, la cuenta **se cierra sola** a los 6 meses de abrirla o al gastar
  los créditos, lo que llegue antes. Tienes 90 días para pasarla al plan de pago si quieres
  conservarla. Además, el Free plan no deja usar algunos servicios.
- En el **Paid plan**, los créditos siguen valiendo hasta su fecha de caducidad y, cuando
  se acaban, se cobra a la tarjeta.

> **Si tus créditos caducaban el 4 de junio de 2026, ya no te cubren.** Si la consola
> muestra el saldo a cero o caducado, todo lo de este tutorial se cobra a la tarjeta. Con
> la arquitectura de abajo son unos pocos dólares si la demo dura pocos días, pero
> conviene saberlo antes de empezar y no después.

Si algún servicio aparece bloqueado mientras sigues el tutorial (con un mensaje del estilo
*not available on the free plan*), la solución es pasar la cuenta al Paid plan en
**Billing → Account plan**.

## 2. Lo que cuesta

Precios aproximados en us-east-1, **por día** con todo encendido:

| Pieza | Tamaño | USD/día aprox. |
|---|---|---|
| EC2 | `t4g.small` | 0,40 |
| RDS PostgreSQL | `db.t4g.micro`, 20 GB | 0,45 |
| Application Load Balancer | 1, poco tráfico | 0,60 |
| IPv4 públicas | la de la EC2 y las del ALB (~3) | 0,36 |
| S3, CloudFront, Parameter Store | uso de demo | ~0 |
| **Total** | | **~1,8 USD/día** |

Unos cinco días de demo cuestan entre 10 y 15 USD. Lo más caro es lo que está **encendido
por horas** (EC2, RDS, ALB e IPs): por eso el [capítulo 13](13-apagado.md) existe, y por
eso en el capítulo 01 pondrás una alerta de gasto antes de crear nada.

## 3. Por qué us-east-1

- Es de las regiones más baratas y donde antes llegan los servicios nuevos.
- **CloudFront solo acepta certificados creados en us-east-1.** Si todo vive allí, un
  único certificado sirve para el front y para la API.
- La latencia desde Perú es algo peor que desde São Paulo (`sa-east-1`), pero São Paulo
  cuesta bastante más, y para una demo no se nota.

## 4. Tabla de valores

Copia esta tabla en un sitio tuyo (**no** en el repositorio si vas a apuntar contraseñas)
y rellénala a medida que avanzas. Los capítulos te dirán cuándo.

| Valor | Ejemplo | El tuyo | Aparece en |
|---|---|---|---|
| `<dominio>` | `midominio` (de `midominio.pe`) | | 00 |
| `<sufijo>` | `smk7` (algo corto y único para los buckets) | | 03 |
| `<cuenta>` | `123456789012` (ID de tu cuenta AWS) | | 01 |
| Bucket de material | `aula-virtual-demo-material-<sufijo>` | | 03 |
| Bucket del front | `aula-virtual-demo-web-<sufijo>` | | 03 |
| Bucket de despliegue | `aula-virtual-demo-deploy-<sufijo>` | | 03 |
| Endpoint de RDS | `aula-virtual-demo.xxxx.us-east-1.rds.amazonaws.com` | | 04 |
| Contraseña de la base | *(la guardas en Parameter Store, no aquí)* | | 04 |
| ID de la instancia EC2 | `i-0abc…` | | 07 |
| DNS del balanceador | `aula-virtual-demo-alb-….elb.amazonaws.com` | | 08 |
| Dominio de CloudFront | `d1234abcd.cloudfront.net` | | 10 |

Los dos nombres públicos serán:

- **`aula.<dominio>.pe`** → el front (lo que abre el usuario).
- **`api.<dominio>.pe`** → la API (lo que llama el front).

## 5. Qué necesitas en tu equipo

- [ ] Acceso al panel DNS de tu dominio `.pe` (vas a crear registros `CNAME`).
- [ ] Este repositorio y `aula-virtual-web`, los dos compilando en local.
- [ ] La AWS CLI v2 (se instala en el [capítulo 01](01-cuenta-y-presupuesto.md)).

## Comprueba que estás listo

- [ ] Sabes si tus créditos cubren la demo o si pagarás con tarjeta.
- [ ] Tienes elegidos `<dominio>` y `<sufijo>`.
- [ ] Puedes crear un registro DNS en tu proveedor.

Siguiente: [01 · Cuenta, usuario y presupuesto](01-cuenta-y-presupuesto.md)
