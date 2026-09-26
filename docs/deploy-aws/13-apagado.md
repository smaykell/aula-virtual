# 13 · Apagarlo todo

## Qué vas a hacer

Borrar la demo en el orden correcto, para que no quede nada cobrando. AWS no borra nada
por su cuenta: un balanceador olvidado cobra lo mismo que uno que se usa.

## Pausar en vez de borrar

Si vas a volver a enseñar la demo en unos días:

- **EC2 → Instance state → Stop**: deja de cobrar la instancia y su IP pública; solo sigue
  el disco (céntimos al día). La IP cambia al volver a encenderla, pero da igual: el ALB
  la encuentra por el ID.
- **RDS → Actions → Stop temporarily**: se para un máximo de **7 días**; después AWS la
  vuelve a encender sola.
- **El ALB no se puede pausar**: es lo más caro que queda encendido (~0,60 USD/día). Si la
  pausa va a ser larga, borra solo el balanceador y vuelve a crearlo (capítulo 08, apartado
  8.4). El target group y el certificado no cobran: déjalos. Al recrearlo el balanceador
  tiene otro **DNS name**, así que actualiza también el CNAME `api` (apartado 8.5).

## Borrar, en este orden

El orden importa: no se puede borrar un Security Group que todavía usa alguien, ni un
certificado enganchado a un balanceador.

- [ ] **CloudFront**: abre la distribución → **Manage plan** → cancela el plan Free (se
      cancela en el acto) → **Disable** → espera a que termine → **Delete**.
- [ ] **Load balancer** `aula-virtual-demo-alb` → **Actions → Delete**.
- [ ] **Target group** `aula-virtual-demo-app` → **Delete**.
- [ ] **EC2**: la instancia → **Instance state → Terminate**.
- [ ] **RDS**: `aula-virtual-demo` → **Actions → Delete** → desmarca *Create final
      snapshot* y *Retain automated backups* (salvo que quieras guardar los datos).
- [ ] **S3**: los tres buckets → **Empty** → **Delete**.
- [ ] **Security groups**: `aula-virtual-demo-db`, luego `-app`, luego `-alb` (en ese orden:
      cada uno referencia al anterior).
- [ ] **Parameter Store**: selecciona los de `/aula-virtual/demo/` → **Delete**.
- [ ] **Certificate Manager**: el certificado → **Delete**.
- [ ] **IAM**: el rol `aula-virtual-demo-ec2` y la política `aula-virtual-demo-ec2`. Si
      seguiste la versión antigua del capítulo 09, comprueba que ya no existe el usuario `ses-smtp-user.…`.
- [ ] **WAF**: si al borrar CloudFront quedó una *Web ACL* suelta en **WAF & Shield**
      (región *Global*), bórrala.
- [ ] **Tu proveedor DNS**: los CNAME de `aula`, `api`, y los de validación de ACM. Si vas a hacer el capítulo 12 enseguida, puedes dejarlos.

## Comprueba que no queda nada

Espera un día y mira:

- [ ] **Billing → Bills**: el mes en curso, servicio a servicio. Nada debería seguir
      sumando.
- [ ] **EC2 → Elastic IPs**: vacío. Una IP elástica sin usar cobra.
- [ ] **EC2 → Volumes** y **Snapshots**: vacíos.
- [ ] **RDS → Snapshots**: vacío, salvo que guardaras uno a propósito.
- [ ] **Tag Editor** (búscalo en la barra) → región `us-east-1` → *All supported resource
      types* → **Search resources**: repasa si queda algo con `aula-virtual` en el nombre.

## Lo que has aprendido

Si llegaste hasta aquí, has manejado de verdad: IAM (usuarios, roles, políticas y el
mínimo privilegio), VPC y Security Groups, EC2 con Session Manager y systemd, RDS, S3 con
URLs firmadas, CORS y ciclo de vida, Parameter Store, ACM, ALB, CloudFront con OAC, Budgets y la CLI. Es prácticamente el temario de la certificación **AWS Certified
Cloud Practitioner** en su parte práctica, y buena parte de la de **Solutions Architect –
Associate**.
