# 12 · Infraestructura como código

## Qué vas a hacer

Escribir como código todo lo que montaste a mano, **borrar lo hecho a mano** y recrearlo
desde el código. Si al recrearlo la demo funciona igual, entiendes de verdad lo que hay.

## Por qué

Lo que has hecho en la consola tiene tres problemas que no se notan hasta el segundo día:

- **No se puede repetir.** ¿Recuerdas qué casilla marcaste en el paso 4.2? El código sí.
- **No se puede revisar.** Un cambio en la consola no deja un *diff* ni pasa por un *pull
  request*.
- **No se puede apagar y encender.** Con código, la demo existe solo cuando la enseñas:
  `destroy` al terminar, `apply` media hora antes de la siguiente presentación. Es la forma
  más eficaz de estirar los créditos.

## Elige herramienta

| | Terraform (u OpenTofu) | AWS CDK |
|---|---|---|
| Lenguaje | HCL, declarativo | TypeScript, Java, Python… |
| Nube | cualquiera | solo AWS |
| Estado | un fichero `tfstate` que guardas tú (en local o en S3) | lo guarda CloudFormation |
| Lo verás en ofertas de empleo | muchísimo | bastante, en empresas muy AWS |
| Curva | leer HCL es inmediato | usas un lenguaje que ya conoces, pero hay que entender CloudFormation debajo |

Recomendación para aprender: **Terraform**. Cada recurso del código corresponde casi uno a
uno con lo que creaste en la consola, así que el mapa mental que ya tienes te sirve tal
cual.

## Cómo abordarlo

Ve en el mismo orden que el tutorial, un capítulo cada vez, y comprueba con `plan` antes de
cada `apply`:

| Capítulo | Recursos de Terraform (proveedor `aws`) |
|---|---|
| 02 | `data "aws_vpc"` (la de por defecto), `aws_security_group`, `aws_vpc_security_group_ingress_rule` |
| 03 | `aws_s3_bucket`, `aws_s3_bucket_public_access_block`, `aws_s3_bucket_cors_configuration`, `aws_s3_bucket_lifecycle_configuration` |
| 04 | `aws_db_instance` |
| 05 | `aws_iam_role`, `aws_iam_policy`, `aws_iam_role_policy_attachment`, `aws_iam_instance_profile` |
| 06 | `aws_ssm_parameter` (los secretos, desde variables marcadas `sensitive`) |
| 07 | `aws_instance`, con `user_data` que haga lo del capítulo 07 (Java, usuario, scripts, unidad de systemd) |
| 08 | `aws_acm_certificate`, `aws_lb`, `aws_lb_target_group`, `aws_lb_listener` |
| 09 | `aws_sesv2_email_identity` |
| 10 | `aws_cloudfront_origin_access_control`, `aws_cloudfront_distribution`, `aws_s3_bucket_policy` |

Tres cosas que el código **no** puede hacer solo, porque tu DNS está fuera de AWS:

- Crear los CNAME de validación de ACM y los de DKIM. Terraform te los da como `output` y
  tú los creas en tu proveedor. (Si alguna vez mueves el DNS a Route 53, ahí sí se cierra
  el círculo.)
- Los CNAME de `aula` y `api`.
- Esos registros los puedes **dejar puestos** entre un `destroy` y un `apply`: ACM
  reutiliza la validación si el nombre no cambia, y basta con actualizar el destino de
  `api` y `aula`.

Y dos buenas prácticas desde el primer día:

- Guarda el `tfstate` en un bucket S3 propio (con versionado), no en el repo: contiene los
  secretos en claro.
- Nunca escribas la contraseña de la base ni el secreto JWT en un `.tf`. Pásalos como
  variables (`TF_VAR_db_password`) o genéralos con el recurso `random_password`.

## Dónde vive el código

Un directorio `infra/` en este repo, o un repo aparte `aula-virtual-infra`. Si lo pones
aquí, añade `*.tfstate*`, `.terraform/` y `*.tfvars` al `.gitignore` antes del primer
commit.

## Comprueba que funciona

- [ ] Borraste todo lo creado a mano (capítulo 13).
- [ ] `terraform apply` desde cero y la prueba del capítulo 11 pasa entera.
- [ ] `terraform destroy` lo borra todo, y **Billing → Bills** al día siguiente no muestra
      recursos vivos.

Siguiente: [13 · Apagarlo todo](13-apagado.md)
