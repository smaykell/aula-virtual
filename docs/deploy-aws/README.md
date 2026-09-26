# Tutorial: publicar una demo del aula virtual en AWS

Este tutorial está pensado para seguirlo a tu ritmo, un capítulo por sesión si quieres.
Al terminar tendrás el aula virtual funcionando en internet con tu dominio, con HTTPS,
correo real y archivos en S3. Y, lo que importa, sabrás qué hace cada pieza de AWS y
por qué está configurada así.

Primero se monta todo **a mano en la consola**, para ver cada pantalla y entender cada
decisión. Al final (capítulo 12) se escribe lo mismo como código.

## Qué vas a construir

```
                         tu proveedor DNS (.pe)
                     aula.<dominio>.pe   api.<dominio>.pe
                            │                   │
                            ▼                   ▼
navegador ──HTTPS──►   CloudFront          Application Load Balancer
                            │               (HTTPS, certificado ACM)
                            ▼                   │ HTTP :8080
                  S3: web (index.html,          ▼
                  JS, CSS del front)      EC2  (Java 21 + el jar, systemd)
                                            │      │        │
                         ┌──────────────────┘      │        └───────────┐
                         ▼                         ▼                    ▼
               RDS PostgreSQL (privada)   S3: material (URLs      Tu servidor SMTP
                                          firmadas; el navegador   (puerto 587)
                                          sube y baja directo)
```

Detrás de todo: **IAM** (quién puede qué), **Security Groups** (quién puede hablar con
quién por la red), **Parameter Store** (los secretos) y **ACM** (los certificados).

## Capítulos

| # | Capítulo | Aprendes | Tiempo aprox. |
|---|---|---|---|
| 00 | [Antes de empezar](00-antes-de-empezar.md) | costes, región, qué anotar | 15 min |
| 01 | [Cuenta, usuario y presupuesto](01-cuenta-y-presupuesto.md) | IAM, MFA, Budgets, CLI | 30 min |
| 02 | [Red y Security Groups](02-red-y-security-groups.md) | VPC, subnets, SG | 20 min |
| 03 | [Buckets S3](03-buckets-s3.md) | S3, CORS, ciclo de vida | 30 min |
| 04 | [Base de datos RDS](04-base-de-datos-rds.md) | RDS, acceso privado | 20 min (+10 de espera) |
| 05 | [Rol IAM de la instancia](05-rol-iam-de-la-instancia.md) | roles, políticas, mínimo privilegio | 20 min |
| 06 | [Parámetros y secretos](06-parametros-ssm.md) | SSM Parameter Store | 20 min |
| 07 | [Servidor EC2](07-servidor-ec2.md) | EC2, Session Manager, systemd | 45 min |
| 08 | [HTTPS: certificado, balanceador y dominio](08-https-alb-y-dominio.md) | ACM, ALB, target groups, DNS | 45 min |
| 09 | [Correo con tu servidor SMTP](09-correo-smtp.md) | SMTP, parámetros, limpiar SES | 15 min |
| 10 | [El front en CloudFront](10-front-cloudfront.md) | CloudFront, OAC, SPA | 40 min |
| 11 | [Prueba de punta a punta](11-prueba-final.md) | depurar un sistema entero | 30 min |
| 12 | [Infraestructura como código](12-infraestructura-como-codigo.md) | Terraform o CDK | a tu ritmo |
| 13 | [Apagarlo todo](13-apagado.md) | no pagar lo que no usas | 20 min |

## Cómo está escrito cada capítulo

1. **Qué vas a construir** y **qué aprendes**: un poco de teoría antes de tocar nada.
2. **Pasos en la consola**, pantalla a pantalla. Los *¿por qué?* explican las decisiones
   que no son obvias.
3. **Con la CLI** (opcional): lo mismo en comandos, para repetirlo rápido o para ver qué
   hay debajo de la consola.
4. **Comprueba que funciona**: no pases al siguiente capítulo sin esto.
5. **Si algo falla**: los tropiezos más comunes de ese paso.

Cada paso tiene su casilla `- [ ]`. Si lees el tutorial en tu editor, márcalas con una
`x` a medida que avanzas y haz commit: así sabes dónde lo dejaste.

> La consola de AWS cambia de aspecto con frecuencia. Si un botón no está donde dice el
> tutorial, busca el mismo concepto con otro nombre: los conceptos no cambian.

## Convenciones

- Lo que va entre `<...>` lo sustituyes por tu valor: `<dominio>`, `<sufijo>`,
  `<cuenta>`… En el [capítulo 00](00-antes-de-empezar.md) hay una tabla para anotarlos.
- Todo va en la región **us-east-1 (N. Virginia)**. Mira siempre el selector de región
  arriba a la derecha de la consola: es el error más frecuente de todos.
- Los comandos de la CLI están escritos para **PowerShell** (el shell de este equipo),
  salvo los que se ejecutan dentro del servidor, que son **bash**.
