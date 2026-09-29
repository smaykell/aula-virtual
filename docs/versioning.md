# Versionado: guía rápida

Cada repo sigue la convención de su ecosistema:

| | Backend (`aula-virtual`) | Frontend (`aula-virtual-web`) |
|---|---|---|
| Convención | La de Spring | SemVer 2.0.0 |
| Dónde vive | `version` en `build.gradle` | `version` en `package.json` |
| Beta / milestone | `1.0.0-M1`, `1.0.0-M2` | `1.0.0-beta.1`, `1.0.0-beta.2` |
| Candidata | `1.0.0-RC1` | `1.0.0-rc.1` |
| Final | `1.0.0` | `1.0.0` |
| En `develop` | `1.0.0-SNAPSHOT` | la de la última release |
| Tag en `main` | `v1.0.0-M1` | `v1.0.0-beta.1` |

Los dos repos se versionan por separado: una beta del front no obliga a sacar una del
back. Lo que sí conviene es que la final `1.0.0` salga a la vez en los dos.

## Backend: convención de Spring

`MAJOR.MINOR.PATCH[-MODIFIER]`, el mismo esquema que Spring Boot, Spring Framework y
el resto de las dependencias del proyecto:

```
1.0.0-SNAPSHOT → 1.0.0-M1 → 1.0.0-M2 → 1.0.0-RC1 → 1.0.0
```

- `M<n>` — milestone. Es la beta: funcional, pero incompleta o sin estabilizar.
- `RC<n>` — release candidate: nada nuevo entra; solo correcciones.
- `-SNAPSHOT` — todo lo que no es una versión publicada. Es lo que lleva `develop`
  **siempre**, durante todas las milestones.
- Sin modificador — la versión final.

Sin punto antes del número (`M1`, no `M.1`) y en mayúsculas, como Spring. Maven y Gradle
ordenan `M1 < RC1 < SNAPSHOT < 1.0.0` sin distinguir mayúsculas.

## Frontend: SemVer

`MAJOR.MINOR.PATCH[-PRERELEASE]`, con la pre-release separada por **guion** y el número
separado por **punto**:

```
1.0.0-beta.1 → 1.0.0-beta.2 → 1.0.0-rc.1 → 1.0.0
```

- `1.0.0.beta.1` no es SemVer válido: el cuarto componente no puede ir con punto.
- `beta.2`, no `beta2`: con punto el número se compara como número (`beta.2 < beta.10`);
  pegado se compara como texto y `beta10` queda antes que `beta2`.
- npm no tiene `-SNAPSHOT`. `develop` conserva la versión de la última release y solo
  cambia en la rama `release/`.

## Publicar una beta con Git Flow

El deploy se dispara al llegar cambios a `main`, así que cada beta es una release
completa. Ejemplo del backend (en el front, lo mismo con `beta.1`):

```bash
git checkout -b release/1.0.0-M1 develop
# build.gradle: version = '1.0.0-M1'
git commit -am "Prepara la versión 1.0.0-M1"

git checkout main
git merge --no-ff release/1.0.0-M1
git tag -a v1.0.0-M1 -m "1.0.0-M1"          # dispara el deploy

git checkout develop
git merge --no-ff release/1.0.0-M1
# build.gradle: version = '1.0.0-SNAPSHOT'
git commit -am "Vuelve develop a 1.0.0-SNAPSHOT"
git branch -d release/1.0.0-M1
git push origin main develop v1.0.0-M1
```

En el front el cambio de versión se hace con npm, sin que cree su propio tag (el tag va
en `main`, no en la rama de release):

```bash
npm version 1.0.0-beta.1 --no-git-tag-version
```

La siguiente beta repite el ciclo con `M2` / `beta.2`; la final, con `1.0.0`. Tras la
final, `develop` del backend pasa a `1.1.0-SNAPSHOT`.

Un arreglo sobre algo ya publicado es un `hotfix/` desde `main` con la siguiente
versión (`hotfix/1.0.0-M2`, `hotfix/1.0.0-beta.2`), fusionado en `main` con tag y en
`develop`. En fase beta suele bastar con esperar a la siguiente.

## Referencias

- [Updates to Spring Versions](https://spring.io/blog/2020/04/30/updates-to-spring-versions) — el esquema `M` / `RC` / `SNAPSHOT` de Spring.
- [Semantic Versioning 2.0.0](https://semver.org/lang/es/)
- [Maven — orden de versiones](https://maven.apache.org/pom.html#version-order-specification)
- [Gradle — orden de versiones](https://docs.gradle.org/current/userguide/dependency_versions.html)
