# 03 · Buckets S3

## Qué vas a construir

Tres buckets, cada uno con un solo propósito:

| Bucket | Para qué | Quién lo lee |
|---|---|---|
| `aula-virtual-demo-material-<sufijo>` | los archivos de los cursos (PDF, vídeos…) | el navegador, con URLs firmadas |
| `aula-virtual-demo-web-<sufijo>` | el front compilado (`index.html`, JS, CSS) | solo CloudFront |
| `aula-virtual-demo-deploy-<sufijo>` | el `.jar` que despliegas | solo el servidor |

## Qué aprendes

**S3** guarda *objetos* (archivos) dentro de *buckets*. Cada objeto tiene una **clave**
(`courses/…/tema-1.pdf`), que parece una ruta pero no lo es: S3 no tiene carpetas, solo
claves con barras.

Tres conceptos clave para este proyecto:

- **Block Public Access**: un interruptor a nivel de bucket que impide que nada sea
  público, aunque alguien se equivoque con una política. Los tres buckets lo tendrán
  encendido. **Nada de esta demo es público en S3.**
- **URL firmada (*presigned URL*)**: una URL que lleva dentro una firma hecha con las
  credenciales del backend y que caduca en minutos. Quien la tiene puede subir o bajar
  *ese* objeto, y nada más. Es como el aula virtual reparte archivos sin que pasen por la
  API (lo explica el README del repo, sección «Cursos, unidades y material»).
- **CORS**: el navegador bloquea las peticiones de JavaScript de un dominio
  (`aula.<dominio>.pe`) a otro (`…s3.amazonaws.com`) salvo que el destino lo permita
  expresamente. Como el navegador sube el archivo directamente a S3, el bucket de
  material tiene que decir «acepto `PUT` y `GET` de `https://aula.<dominio>.pe`».

Y una regla de **ciclo de vida**: S3 puede borrar objetos solo, según una condición. La
app marca cada subida con la etiqueta `status=pending` y se la quita cuando el material
se confirma. La regla borra al día siguiente lo que siga pendiente: subidas abandonadas.

## Pasos en la consola

Los nombres de bucket son **únicos en todo el mundo**: por eso llevan `<sufijo>`.

### 3.1 Bucket de material

- [ ] Busca **S3** → **Create bucket**.
- [ ] **Bucket type**: General purpose. **Bucket name**: `aula-virtual-demo-material-<sufijo>`.
- [ ] **Object Ownership**: ACLs disabled (lo recomendado).
- [ ] **Block Public Access settings**: **Block all public access** marcado.
- [ ] **Bucket Versioning**: Disable. **Default encryption**: SSE-S3 (lo que viene).
- [ ] **Create bucket**.

Ahora el **CORS**:

- [ ] Abre el bucket → pestaña **Permissions** → **Cross-origin resource sharing (CORS)** →
      **Edit** → pega (con tu dominio):

```json
[
  {
    "AllowedOrigins": ["https://aula.<dominio>.pe"],
    "AllowedMethods": ["GET", "PUT"],
    "AllowedHeaders": ["content-type", "x-amz-tagging"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3600
  }
]
```

> **¿Por qué solo esas dos cabeceras?** Son las que el front manda en la subida además de
> las que pone el navegador solo. La app **firma** el tipo, el tamaño y la etiqueta
> `status=pending`: si el navegador mandara otros, la firma no coincidiría y S3 rechazaría
> la subida. Así un usuario no puede subir 2 GB donde declaró 2 MB.

Y la **regla de ciclo de vida**:

- [ ] Pestaña **Management** → **Lifecycle rules** → **Create lifecycle rule**.
- [ ] **Lifecycle rule name**: `expire-pending-uploads`.
- [ ] **Choose a rule scope**: *Limit the scope of this rule using one or more filters* →
      **Object tags** → **Add tag**: Key `status`, Value `pending`.
- [ ] **Lifecycle rule actions**: marca **Expire current versions of objects** → **Days
      after object creation**: `1`.
- [ ] **Create rule**.

### 3.2 Bucket del front

- [ ] **Create bucket** → `aula-virtual-demo-web-<sufijo>`, con **Block all public access**
      marcado y lo demás por defecto.

Nada más. En el capítulo 10, CloudFront añadirá una política que le deja leerlo **solo a
él** (*Origin Access Control*).

> **¿Por qué no usar el «Static website hosting» de S3?** Porque exige que el bucket sea
> público y solo sirve HTTP. CloudFront delante de un bucket privado da HTTPS, caché
> mundial y un bucket que nadie puede leer directamente.

### 3.3 Bucket de despliegue

- [ ] **Create bucket** → `aula-virtual-demo-deploy-<sufijo>`, privado, lo demás por
      defecto.

Aquí subirás el `.jar` desde tu equipo y el servidor lo descargará. Es la forma más
sencilla de llevar un archivo a un servidor sin abrir SSH.

## Con la CLI (opcional)

```powershell
$sufijo = "<sufijo>"
foreach ($nombre in "material", "web", "deploy") {
  aws s3api create-bucket --bucket "aula-virtual-demo-$nombre-$sufijo" --region us-east-1
}
```

Block Public Access viene activado por defecto en los buckets nuevos. El CORS y el ciclo
de vida, desde la raíz del repo (el JSON de ciclo de vida es el mismo que usa el MinIO
local):

```powershell
Set-Content cors.json -Encoding utf8 '{"CORSRules":[{"AllowedOrigins":["https://aula.<dominio>.pe"],"AllowedMethods":["GET","PUT"],"AllowedHeaders":["content-type","x-amz-tagging"],"ExposeHeaders":["ETag"],"MaxAgeSeconds":3600}]}'
aws s3api put-bucket-cors --bucket "aula-virtual-demo-material-$sufijo" --cors-configuration file://cors.json
aws s3api put-bucket-lifecycle-configuration --bucket "aula-virtual-demo-material-$sufijo" --lifecycle-configuration file://docker/minio/lifecycle.json
Remove-Item cors.json
```

## Comprueba que funciona

```powershell
aws s3 ls
aws s3api get-bucket-cors --bucket aula-virtual-demo-material-<sufijo>
aws s3api get-bucket-lifecycle-configuration --bucket aula-virtual-demo-material-<sufijo>
aws s3api get-public-access-block --bucket aula-virtual-demo-web-<sufijo>
```

- [ ] Aparecen los tres buckets.
- [ ] El de material devuelve su CORS y la regla `expire-pending-uploads`.
- [ ] Los cuatro valores de `PublicAccessBlockConfiguration` están en `true`.
- [ ] Anota los tres nombres en tu tabla.

## Si algo falla

- **`BucketAlreadyExists`**: alguien en el mundo ya usa ese nombre. Cambia `<sufijo>`.
- **Error de JSON al guardar el CORS**: la consola espera la lista `[ ... ]` directamente;
  la CLI, en cambio, la quiere dentro de `{"CORSRules": [...]}`. Son el mismo contenido en
  dos envoltorios.

Siguiente: [04 · Base de datos RDS](04-base-de-datos-rds.md)
