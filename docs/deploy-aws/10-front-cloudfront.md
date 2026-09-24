# 10 · El front en CloudFront

## Qué vas a construir

`https://aula.<dominio>.pe` sirviendo el front (`aula-virtual-web`) desde el bucket privado
del capítulo 03, a través de CloudFront.

## Qué aprendes

**CloudFront** es la CDN de AWS: cientos de servidores (*edge locations*) repartidos por
el mundo que guardan una copia de tus archivos cerca del usuario. Hay uno en Lima. Además
pone el HTTPS con tu certificado de ACM.

- **Origen**: de dónde saca CloudFront los archivos la primera vez (tu bucket).
- **OAC (Origin Access Control)**: la identidad con la que CloudFront firma sus peticiones
  al bucket. La política del bucket dice «solo esta distribución puede leer», así que el
  bucket sigue siendo privado y nadie se salta CloudFront.
- **Caché**: CloudFront guarda cada archivo el tiempo que le digan las cabeceras
  `Cache-Control`. Para publicar una versión nueva al momento, se **invalida** la caché.

**Un front de una sola página (SPA)** tiene un problema propio. Vite genera **un único**
`index.html`, y el router del navegador (TanStack Router) decide qué pantalla enseñar según
la URL. Si alguien abre directamente `https://aula.<dominio>.pe/join/ABCD2345`, CloudFront
busca un objeto `join/ABCD2345` en el bucket, no lo encuentra y devuelve un error. La
solución: que cualquier «no existe» responda con `index.html` y un 200, y el router se
encarga del resto. Es lo que hace que funcionen los enlaces de invitación y los de
recuperar la contraseña que salen en los correos.

**Planes de precio.** Al crear una distribución, la consola ofrece planes de tarifa plana.
El **Free** (0 USD/mes) sobra para una demo, admite tu dominio aunque el DNS esté fuera de
AWS y lleva WAF (un cortafuegos de aplicación) incluido. Cada cuenta puede tener tres.

## Pasos

### 10.1 Compila el front

En el repo `aula-virtual-web`:

```powershell
$env:VITE_API_BASE_URL = 'https://api.<dominio>.pe/api'
pnpm install
pnpm build
Remove-Item Env:VITE_API_BASE_URL
```

`VITE_API_BASE_URL` se incrusta en el JavaScript **al compilar**, no al servir. Si cambias
de dominio, hay que compilar otra vez. Comprueba que se incrustó:

```powershell
Select-String -Path dist\assets\*.js -Pattern "api.<dominio>.pe" -List | Select-Object Path
```

### 10.2 Súbelo al bucket

```powershell
$web = "s3://aula-virtual-demo-web-<sufijo>"
aws s3 sync dist\assets "$web/assets" --delete --cache-control "public,max-age=31536000,immutable"
aws s3 sync dist $web --delete --exclude "assets/*" --cache-control "no-cache"
```

> **¿Por qué dos cachés distintas?** Vite pone un *hash* en el nombre de cada archivo de
> `assets/` (`index-3f9a1c.js`): si el contenido cambia, el nombre cambia. Por eso se pueden
> cachear un año. `index.html` en cambio se llama siempre igual y es el que apunta a los
> nuevos: `no-cache` obliga a comprobar si hay versión nueva cada vez.

### 10.3 Crea la distribución

- [ ] Busca **CloudFront** → **Create distribution**.
- [ ] **Choose a plan**: **Free**.
- [ ] **Distribution name**: `aula-virtual-demo-web`. **Distribution type**: *Single website
      or app*.
- [ ] Si te pide un dominio de **Route 53**, déjalo en blanco u omítelo: tu DNS está fuera
      y añadirás el dominio en el paso 10.4.
- [ ] **Origin type**: **Amazon S3** → **Browse S3** → `aula-virtual-demo-web-<sufijo>`.
- [ ] Marca **Allow private S3 bucket access to CloudFront** (crea el OAC y actualiza la
      política del bucket por ti).
- [ ] **Origin settings** y **Cache settings**: los recomendados.
- [ ] **Web Application Firewall (WAF)**: deja la protección que propone (va incluida en el
      plan).
- [ ] Revisa y **Create distribution**.

Anota el **Distribution domain name** (`d1234abcd.cloudfront.net`) y el **ID** (`E…`).

> La consola de CloudFront cambió mucho en 2025. Si tu asistente es distinto, lo que
> importa es: origen S3 privado **con OAC**, redirigir HTTP a HTTPS y los ajustes de los
> pasos siguientes.

### 10.4 Dominio, certificado y página por defecto

- [ ] Abre la distribución → pestaña **General** → **Settings** → **Edit**.
- [ ] **Alternate domain name (CNAME)** → **Add item** → `aula.<dominio>.pe`.
- [ ] **Custom SSL certificate**: el certificado del capítulo 08 (incluye `aula`).
- [ ] **Default root object**: `index.html`.
- [ ] **Save changes**.

### 10.5 Rutas del SPA

- [ ] Pestaña **Error pages** → **Create custom error response**:
  - **HTTP error code**: `403`. **Customize error response**: Yes. **Response page path**:
    `/index.html`. **HTTP response code**: `200`. **Create**.
- [ ] Repite con **HTTP error code** `404`.

> **¿Por qué también 403?** Con OAC, CloudFront no tiene permiso para *listar* el bucket, y
> S3 responde 403 (no 404) cuando se pide algo que no existe y no se puede listar. Es el
> mismo detalle que viste con el rol del servidor en el capítulo 05, visto desde el otro
> lado.

### 10.6 El nombre `aula` en tu DNS

- [ ] En tu proveedor DNS, registro **CNAME**: nombre `aula`, valor
      `d1234abcd.cloudfront.net` (el tuyo).

Los cambios de CloudFront tardan unos minutos en llegar a todas las *edge locations*
(**Last modified** deja de decir *Deploying*).

### 10.7 Publicar versiones nuevas del front

Cada vez que cambies el front: compilar (10.1), subir (10.2) e invalidar:

```powershell
aws cloudfront create-invalidation --distribution-id <id-distribucion> --paths "/*"
```

Las primeras 1000 rutas invalidadas al mes son gratis.

## Comprueba que funciona

- [ ] `https://aula.<dominio>.pe` abre la pantalla de login, con el candado.
- [ ] `https://aula.<dominio>.pe/reset-password` abierto **directamente** (pegado en la
      barra, no navegando) carga la pantalla, no un error XML.
- [ ] La URL del bucket directa
      (`https://aula-virtual-demo-web-<sufijo>.s3.amazonaws.com/index.html`) responde
      **AccessDenied**. El bucket sigue privado.
- [ ] Con las herramientas del navegador abiertas (F12 → **Network**), inicia sesión: las
      peticiones van a `https://api.<dominio>.pe/api/...` y ninguna falla por CORS.

## Si algo falla

- **Error de CORS en la consola del navegador al hacer login**: `CORS_ALLOWED_ORIGINS` no
  coincide **exactamente** con el origen del front: `https://aula.<dominio>.pe`, sin barra
  final. Corrígelo en Parameter Store y reinicia la app.
- **Las peticiones van a `/api/...` del propio `aula.<dominio>.pe`**: el front se compiló sin
  `VITE_API_BASE_URL`. Repite 10.1.
- **`AccessDenied` en XML al abrir la web**: falta la política del OAC en el bucket.
  CloudFront la muestra en **Origins → Edit** con un botón **Copy policy**; pégala en
  **S3 → bucket → Permissions → Bucket policy**.
- **No deja añadir el dominio alternativo**: el certificado no está *Issued*, no está en
  us-east-1 o no incluye `aula.<dominio>.pe`.
- **Sigue saliendo la versión vieja**: falta invalidar (10.7), o el navegador la tiene en
  caché (`Ctrl+F5`).

Siguiente: [11 · Prueba de punta a punta](11-prueba-final.md)
