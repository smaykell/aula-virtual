# 05 · Rol IAM de la instancia

## Qué vas a construir

El rol `aula-virtual-demo-ec2`, que llevará puesto el servidor. Con él, la app firma URLs de
S3 y lee sus secretos **sin que haya una sola clave de acceso guardada** en el servidor,
en el repositorio ni en la configuración.

## Qué aprendes

Un **rol** es un conjunto de permisos sin contraseña. Cuando una EC2 tiene un rol
(técnicamente, un *instance profile*), AWS le entrega **credenciales temporales** a través
del servicio de metadatos de la instancia y las renueva solas antes de que caduquen.

La app ya sabe aprovecharlo: si `STORAGE_ACCESS_KEY` y `STORAGE_SECRET_KEY` están
**definidas y vacías**, el cliente de S3 usa la *cadena de credenciales por defecto* del
SDK de AWS, que en una EC2 termina en el rol
(`common/storage/StorageConfig.credentialsOf`). En local siguen valiendo las claves de
MinIO.

Un rol tiene dos políticas distintas, y conviene no confundirlas:

- **Trust policy** (de confianza): *quién* puede asumir el rol. Aquí: el servicio EC2.
- **Permissions policy** (de permisos): *qué* puede hacer quien lo asume.

Y aquí sí aplicas el **mínimo privilegio** de verdad: el servidor es lo que está expuesto
a internet. Si alguien encontrara un fallo en la app y tomara el control, solo podría hacer
lo que dice este rol: tocar objetos de *un* bucket, leer *un* `.jar` y leer *sus*
parámetros. No podría borrar la base, ni crear servidores, ni leer el bucket del front.

## Pasos en la consola

### 5.1 La política de permisos

- [ ] **IAM** → **Policies** → **Create policy** → pestaña **JSON** → pega esto,
      sustituyendo `<sufijo>` y `<cuenta>`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "MaterialObjects",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:PutObjectTagging",
        "s3:DeleteObjectTagging"
      ],
      "Resource": "arn:aws:s3:::aula-virtual-demo-material-<sufijo>/*"
    },
    {
      "Sid": "MaterialMissingIs404",
      "Effect": "Allow",
      "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::aula-virtual-demo-material-<sufijo>"
    },
    {
      "Sid": "DownloadJar",
      "Effect": "Allow",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::aula-virtual-demo-deploy-<sufijo>/*"
    },
    {
      "Sid": "ReadOwnParameters",
      "Effect": "Allow",
      "Action": "ssm:GetParametersByPath",
      "Resource": [
        "arn:aws:ssm:us-east-1:<cuenta>:parameter/aula-virtual/demo",
        "arn:aws:ssm:us-east-1:<cuenta>:parameter/aula-virtual/demo/*"
      ]
    }
  ]
}
```

- [ ] **Next** → **Policy name**: `aula-virtual-demo-ec2` → **Create policy**.

Qué hace cada bloque:

| Sid | Por qué la app lo necesita |
|---|---|
| `MaterialObjects` | firmar subidas (`PutObject` + `PutObjectTagging`, porque la subida lleva la etiqueta `status=pending`), firmar descargas y comprobar el archivo (`GetObject`), confirmarlo quitando la etiqueta (`DeleteObjectTagging`) y borrarlo (`DeleteObject`). |
| `MaterialMissingIs404` | sin `ListBucket`, S3 responde **403** en vez de **404** al preguntar por un objeto que no existe. La app espera el 404 para responder `CRS_FILE_NOT_UPLOADED`; con un 403 daría un error 500. |
| `DownloadJar` | el servidor descarga el `.jar` que subes al desplegar. Solo lectura. |
| `ReadOwnParameters` | leer los parámetros bajo `/aula-virtual/demo` (capítulo 06). Ninguno más. |

> **Una firma no es un permiso.** Una URL firmada se firma con las credenciales del rol y
> hereda sus permisos *en el momento de usarse*. Si el rol no pudiera hacer `PutObject`, la
> app firmaría URLs igual, pero S3 las rechazaría cuando el navegador las usara. Por eso el
> rol necesita los permisos de lo que firma, aunque la app nunca suba un byte ella misma.

### 5.2 El rol

- [ ] **IAM** → **Roles** → **Create role**.
- [ ] **Trusted entity type**: AWS service. **Use case**: **EC2** → **Next**.

  Esto genera la *trust policy* que dice «EC2 puede asumir este rol».
- [ ] **Add permissions**: busca y marca:
  - `aula-virtual-demo-ec2` (la tuya).
  - `AmazonSSMManagedInstanceCore` (de AWS): permite que **Session Manager** entre al
    servidor. Sin ella no podrás conectarte, porque no hay SSH.
- [ ] **Role name**: `aula-virtual-demo-ec2` → **Create role**.

## Con la CLI (opcional)

Guarda la política del paso 5.1 como `policy.json` y la de confianza como `trust.json`:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Service": "ec2.amazonaws.com" },
    "Action": "sts:AssumeRole"
  }]
}
```

```powershell
aws iam create-policy --policy-name aula-virtual-demo-ec2 --policy-document file://policy.json
aws iam create-role --role-name aula-virtual-demo-ec2 --assume-role-policy-document file://trust.json
aws iam attach-role-policy --role-name aula-virtual-demo-ec2 --policy-arn arn:aws:iam::<cuenta>:policy/aula-virtual-demo-ec2
aws iam attach-role-policy --role-name aula-virtual-demo-ec2 --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
aws iam create-instance-profile --instance-profile-name aula-virtual-demo-ec2
aws iam add-role-to-instance-profile --instance-profile-name aula-virtual-demo-ec2 --role-name aula-virtual-demo-ec2
```

Fíjate en las dos últimas líneas: la consola crea el *instance profile* sola, la CLI no.
Es lo que se engancha a la EC2; el rol va dentro.

## Comprueba que funciona

- [ ] **IAM → Roles → aula-virtual-demo-ec2 → Permissions**: dos políticas.
- [ ] Pestaña **Trust relationships**: `"Service": "ec2.amazonaws.com"`.
- [ ] En la política propia, los ARN llevan **tu** `<sufijo>` y **tu** `<cuenta>`, no los
      textos entre `< >`.

El **IAM Policy Simulator** (búscalo en Google: *IAM policy simulator*) deja probar sin
servidor: elige el rol, la acción `s3:PutObject` y el ARN de un objeto del bucket de
material → *allowed*; y del bucket del front → *denied*.

## Si algo falla

- **El editor JSON marca error**: suele ser una coma de más tras el último elemento de una
  lista.
- **Te equivocaste con un ARN**: **Policies → aula-virtual-demo-ec2 → Edit**. Los cambios
  en la política afectan al rol en segundos, sin reiniciar nada.

Siguiente: [06 · Parámetros y secretos](06-parametros-ssm.md)
