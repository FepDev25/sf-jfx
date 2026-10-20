# ADR-0001 — Modelo de despliegue: single-tenant auto-instalable

- **Estado:** `ACEPTADO` — Fases 0 y 1 implementadas; Fase 2 en curso; Fase 3 pendiente
- **Fecha:** 2026-10-19
- **Ámbito:** despliegue, persistencia, semillas, instalación y operación
- **Relacionado:** [`../decisiones.md`](../decisiones.md) (Fase 14), [`../roadmap.md`](../roadmap.md) (Hito 12)

---

## 1. Contexto

`sf-jfx` es un sistema de facturación para supermercados, minimercados y tiendas, con backend
Java/JDBC sobre PostgreSQL, frontend JavaFX y facturación electrónica simulada para el SRI de
Ecuador. El sistema ya soporta organización multiempresa:

```
empresa → sucursal → bodega
                   → caja
```

Hoy una instalación puede contener varias empresas, sucursales, bodegas y cajas. La conexión se
resuelve con un archivo `sfjfx.properties`, las migraciones se aplican al arrancar y existe una
semilla de desarrollo que crea una empresa demo con usuarios y datos masivos de prueba.

El proyecto nació con un alcance funcional congelado (Hitos 0–11 cerrados). Falta definir
explícitamente **cómo se instala, se opera y se aísla** el sistema en clientes reales. La duda
planteada fue:

> ¿Rediseñar el proyecto como multi-tenant compartido o declararlo auto-instalable en diferentes
> máquinas, como los sistemas clásicos?

### Fuerzas del contexto

1. **POS y hardware local.** Impresora térmica, gaveta, lector de barras, balanza y red local.
   La caja no puede depender de internet ni de la latencia de un servidor remoto.
2. **Obligaciones fiscales (SRI).** RUC, certificado `.p12`, secuencias, ambiente
   Pruebas/Producción, comprobantes y conservación de documentos. Cada cliente espera control
   sobre sus datos y sus respaldos.
3. **Operación clásica del negocio.** El supermercado suele tener su servidor o una máquina
   dedicada en tienda; el soporte se hace por instalación.
4. **Costo de ingeniería.** Multi-tenant compartido exige aislamiento por fila (RLS), contexto de
   tenant por conexión, unicidad por tenant, migraciones y respaldos por tenant, pruebas de fuga
   de datos y auditoría permanente.
5. **Estado actual del código.** El esquema ya usa `empresa_id` en la mayoría de tablas y los
   repositorios filtran por empresa. No hay `tenant_id` ni RLS. El modelo existente está más
   cerca de una instalación por operador que de un SaaS compartido.

---

## 2. Decisión

Se adopta el modelo **single-tenant auto-instalable**:

1. **Una instalación atiende a un operador/cliente.** Cada cliente tiene su propia instalación
   del sistema y su propia base de datos PostgreSQL.
2. **Una base de datos por instalación** (modelo *silo* o *database-per-tenant*). Si varios
   clientes comparten un servidor PostgreSQL, cada uno usa una base independiente. No se usa un
   esquema compartido como opción por defecto.
3. **`empresa` no es un tenant SaaS.** Es una entidad legal dentro de una instalación. El modelo
   multiempresa se conserva para cadenas, holdings o varios RUC del mismo operador.
4. **No se implementa multi-tenant compartido ahora.** Si en el futuro se ofrece SaaS, la
   evolución será **instancias gestionadas por cliente + panel de control**, no una base común
   con `tenant_id`.
5. **Las semillas se retiran del arranque automático.** No forman parte del arranque de una
   instalación real: el arranque de producción solo aplica migraciones. Las semillas de
   desarrollo y demostración pasan a ser comandos explícitos, ejecutables únicamente en entornos
   locales/de prueba.
6. **El primer arranque se resuelve con un asistente de instalación**, no con credenciales por
   defecto. El administrador se crea con contraseña elegida por el cliente y cambio obligatorio.
7. **El producto se entrega empaquetado y operable**: instalador `app-image` para **Windows y
   Linux** con **PostgreSQL incluido** (puerto libre detectado y credenciales aleatorias);
   respaldo y restauración, migraciones controladas, logs y verificación de salud. El alcance es
   **solo escritorio**: no se implementa modo servidor ni API.
8. **Modo por defecto `UNICA_EMPRESA`.** El modo `MULTIEMPRESA` existe para cadenas y holdings y
   se habilita de forma explícita.
9. **Las semillas no forman parte del producto instalable.** No se ejecutan en el arranque, no se
   ofrecen en el instalador y no se incluyen en el paquete de producción. Se conservan en el
   repositorio como herramienta local de desarrollo/pruebas, ejecutada manualmente por el
   desarrollador en su PC.
10. **Actualizaciones: instalación manual + actualización remota asistida.** La instalación
    inicial la realiza un técnico con el instalador. Las actualizaciones posteriores se descargan
    desde un servidor de actualizaciones, se validan por firma y las aprueba el técnico antes de
    respaldar, migrar y aplicar. No se adopta actualización automática por flota.
11. **Licenciamiento: perpetuo por instalación + mantenimiento anual.** La licencia se materializa
    en un archivo firmado offline, ligado al ID de instalación, con edición y límites. El
    mantenimiento anual habilita actualizaciones y soporte. Si vence, la aplicación sigue
    operando y los datos siguen disponibles; solo se detienen las actualizaciones y el soporte.

### Definiciones

| Término | Significado en este ADR |
|---|---|
| **Instalación** | Despliegue concreto del software para un operador. Incluye una base PostgreSQL local administrada por el instalador, configuración y archivos locales. |
| **Tenant** | Cliente/operador aislado. En este modelo, tenant = instalación = base de datos. |
| **Empresa** | Entidad legal dentro de la instalación. Puede haber una o varias. |
| **Semilla** | Datos creados por código para desarrollo/demostración. No es esquema, no es catálogo base y **no se distribuye con el producto**. |
| **Datos base** | Catálogos necesarios para operar (permisos, roles, unidades, impuestos, medios de pago, tipos de movimiento), aplicados por migraciones. |
| **Licencia perpetua** | Derecho de uso indefinido de la versión licenciada. No incluye automáticamente versiones futuras. |
| **Mantenimiento anual** | Servicio renovable que habilita actualizaciones, soporte y nuevas versiones durante su vigencia. |
| **Actualización remota asistida** | Actualización descargada desde un servidor, validada por firma y aprobada por un técnico antes de aplicarse. No es automática ni desatendida. |

---

## 3. Consecuencias

### Positivas

- **Aislamiento real:** cada cliente tiene su base; un error o una consulta costosa no afecta a
  otros.
- **Respaldo y restauración simples:** `pg_dump`/`pg_restore` por cliente, sin filtrar tenants.
- **Continuidad del POS:** la operación de caja queda en la red local y no depende de la nube.
- **Soporte y actualización controlados:** se puede actualizar, diagnosticar y revertir por
  instalación.
- **Cumplimiento fiscal más claro:** certificados, secuencias y comprobantes quedan en el
  entorno del cliente.
- **Menor complejidad de dominio:** no hay que añadir `tenant_id`, RLS ni contexto de tenant a
  todas las consultas.
- **Camino a SaaS sin reescritura:** una instancia administrada por cliente sigue siendo válida.
- **Ingreso recurrente por mantenimiento:** el cliente puede seguir operando aunque no renueve;
  la renovación habilita updates y soporte.
- **Operación sin internet para la licencia:** el archivo firmado se valida localmente; la caja no
  depende de un servidor de licencias.

### Negativas

- **Más instalaciones que operar.** La operación manual no escala; se mitiga con automatización
  y, más adelante, un panel de control de instancias.
- **Actualizaciones distribuidas.** Cada cliente puede quedar en una versión distinta; se
  necesita un procedimiento claro y migraciones idempotentes.
- **Sin analítica cross-cliente nativa.** Para reportes globales habrá que recopilar datos de
  forma explícita, no consultar una base común.
- **Onboarding más pesado.** Requiere asistente de instalación y validaciones de primer arranque.
- **Instalador más pesado.** Incluir PostgreSQL implica administrar binarios, servicio, data
  directory, puerto y actualizaciones mayores de la base, no solo de la aplicación.
- **Infraestructura de actualización y firma.** Hay que operar un servidor de updates, custodiar
  la clave privada de firma y mantener el mecanismo de rollback.
- **Gestión de licencias.** Emitir, renovar y reemplazar archivos de licencia añade un proceso
  administrativo que debe ser simple y auditable.

### Neutras

- No se adopta actualización automática por flota; el técnico aprueba cada actualización.

- `empresa_id` se mantiene para la jerarquía multiempresa; no se convierte en `tenant_id`.
- No se necesita RLS en el corto/medio plazo.
- `db.esquema` queda reservado para el esquema PostgreSQL (`public` por defecto); no se usa para
  separar clientes.

---

## 4. Alternativas consideradas

### A. Multi-tenant compartido (schema compartido + `tenant_id`)

Una sola base y una sola instancia para todos los clientes, con aislamiento lógico por fila.

**Ventajas:** menor costo operativo a gran escala, onboarding self-service, analítica central.

**Motivos de rechazo ahora:**

- No encaja con hardware local ni con la operación de caja.
- Exige RLS, contexto de tenant, unicidad por tenant y pruebas de fuga.
- Cambia el modelo mental de `empresa` y afecta login, configuración, certificados, secuencias y
  respaldos.
- El volumen esperado no justifica la complejidad.

**Cuándo reevaluar:** cientos o miles de clientes pequeños, autoservicio, sin hardware crítico,
equipo dedicado a mantener el aislamiento y la facturación SaaS.

### B. Schema por tenant (una base, un esquema por cliente)

**Ventajas:** más barato que una base por cliente; respaldo por esquema.

**Motivos de rechazo como opción por defecto:**

- Mayor riesgo de errores de `search_path`.
- `db.esquema` está declarado en la configuración pero hoy `ConexionBD` no lo aplica.
- El respaldo/restauración por esquema es más delicado que una base independiente.

**Cuándo considerarla:** si una base por cliente resulta demasiado costosa para un despliegue
masivo y se acepta la complejidad operativa.

### C. Single-tenant auto-instalable (elegida)

Una instalación y una base por cliente, con empaquetado y primer arranque controlados.

**Ventajas:** aislamiento, continuidad local, respaldos simples, menor riesgo fiscal y de datos.
**Costo:** más operación por cliente, mitigable con automatización y panel de instancias.

---

## 5. Política de semillas y entornos

Esta es una decisión explícita del ADR: **las semillas no se ejecutan en producción, no se
ofrecen en el instalador y no se incluyen en el paquete de producción.** Son una herramienta
local del desarrollador.

### Clasificación

| Elemento | ¿Qué es? | ¿Se aplica en producción? | ¿Se distribuye con el producto? |
|---|---|---|---|
| `V1`–`V9` | Esquema versionado y evolución de la base. | **Sí**, siempre. | **Sí**. |
| `V2` (datos base) | Permisos, roles, unidades, impuestos, medios de pago y tipos de movimiento. | **Sí**, forma parte del sistema. | **Sí**. |
| `SemillaDesarrollo` | Empresa demo, sucursales, bodegas, cajas, usuario `admin/admin`. | **No.** | **No.** Solo repositorio local. |
| `SemillaDemo` | Datos masivos de demostración (productos, ventas, compras, etc.). | **No.** | **No.** Solo repositorio local. |

### Reglas

1. **Producción:** el arranque solo aplica migraciones pendientes. No crea empresas, usuarios,
   contraseñas por defecto ni datos de demostración.
2. **Primer arranque real:** el asistente crea la empresa, sucursal, bodega, caja y usuario
   administrador con contraseña elegida por el cliente. El cambio de contraseña es obligatorio.
   El asistente **no ofrece cargar semillas**.
3. **Desarrollo:** las semillas se ejecutan con un comando explícito y una bandera de entorno.
   Nunca de forma implícita y nunca desde el instalador.
4. **Instalación local de prueba:** el desarrollador puede ejecutar las semillas manualmente en su
   propia PC para probar el sistema. Ese flujo es parte del repositorio/herramientas de
   desarrollo, no del producto que se entrega al cliente.
5. **Idempotencia:** las semillas siguen siendo idempotentes, pero la idempotencia no las hace
   seguras para producción; la separación es por entorno, por comando y por artefacto.
6. **Empaquetado:** el instalador y el artefacto de producción excluyen las clases/recursos de
   semilla o los dejan fuera del perfil de ejecución. La Fase 0 define el mecanismo exacto.

### Matriz de entornos

| Entorno | Migraciones | Datos base `V2` | `SemillaDesarrollo` | `SemillaDemo` |
|---|---|---|---|---|
| Instalador / producción | Sí | Sí | No | No |
| Desarrollo local (PC del desarrollador) | Sí | Sí | Sí, manual | Opcional, manual |
| Demo local (PC del desarrollador) | Sí | Sí | Sí, manual | Sí, manual |

### Estado actual (Fases 0 y 1 implementadas)

`Arranque` solo aplica migraciones. `SemillaDesarrollo` y `SemillaDemo` viven en el módulo
`dev`, que no es dependencia de `core` ni de `ui` y no se incluye en el producto instalable. Las
semillas se ejecutan manualmente:

```bash
mvn -pl core install -DskipTests
mvn -pl dev exec:java
```

Si `app.entorno=prod` (o un valor desconocido), las semillas se rechazan con un error explícito.
El valor por defecto de `app.entorno` es `prod`; el archivo de ejemplo usa `dev` para desarrollo
local.

---

## 6. Plan de implementación (Fases 0–3)

> Estado: **Fases 0 y 1 implementadas (2026-10-19); Fase 2 en curso; Fase 3 no iniciada.**
> Ninguna fase se considera ejecutada hasta que sus criterios de aceptación estén verificados y se
> registre el cierre en el roadmap.

### Fase 0 — Formalización y guardas de semilla `[IMPLEMENTADA]`

**Objetivo:** dejar el modelo documentado y eliminar el riesgo de sembrar datos en producción.

**Entregables:**

- ADR-0001 y actualización de `README.md`, `docs/decisiones.md`, `docs/roadmap.md`,
  `docs/datos-demo.md`, `docs/modelo-datos.md` y `docs/vistas-ui.md`.
- Separación explícita de entornos (`app.entorno=dev|prod`) y guardia de semillas.
- `Arranque` con comportamiento por defecto: **solo migraciones**.
- Comandos explícitos para `SemillaDesarrollo` y `SemillaDemo` en el módulo `dev`.
- Exclusión de las semillas del instalador y del artefacto de producción (módulo separado).
- Aviso en consola y en documentación cuando se ejecute una semilla.
- Pruebas de arranque: base vacía en modo producción no crea empresa ni usuario.

**Criterios de aceptación:**

- [x] Un arranque en modo producción sobre una base vacía deja 0 empresas y 0 usuarios.
- [x] Las migraciones y los datos base de `V2` se aplican correctamente.
- [x] Las semillas solo se ejecutan con una bandera/comando explícito.
- [x] El instalador y el artefacto de producción no incluyen ni ofrecen semillas.
- [x] La documentación distingue claramente datos base, semilla de desarrollo y semilla demo.
- [x] No se introducen credenciales por defecto en el flujo de producción.

**Cierre:** 2026-10-19. `core` solo migra; semillas en `dev`; guardia `app.entorno=prod`;
77 pruebas verdes (75 de `core` + 2 de `dev`).

**Fuera de alcance:** asistente de instalación, empaquetado y panel de instancias.

### Fase 1 — Primer arranque e instalación `[IMPLEMENTADA]`

**Objetivo:** instalar el sistema en una máquina limpia sin tocar SQL ni credenciales por
defecto.

**Entregables:**

- Asistente de primer arranque: conexión a PostgreSQL, empresa/RUC/régimen, sucursal, bodega,
  caja, usuario administrador y ambiente SRI.
- Mockup del asistente antes de implementarlo, según la decisión 13.6 (mockups antes de código).
- Modo por defecto `UNICA_EMPRESA`; `MULTIEMPRESA` se habilita explícitamente como opción
  avanzada.
- El asistente no ofrece semillas ni credenciales por defecto.
- Metadatos de instalación (`instalacion`, migración `V8`): nombre, modo
  (`UNICA_EMPRESA` o `MULTIEMPRESA`), versión de esquema y fecha de instalación.
- Validaciones de RUC/cédula, duplicados y conexión.
- Contraseña elegida por el cliente; cambio obligatorio si se genera una temporal.
- Ejecución de migraciones desde el asistente y verificación de salud.
- Opción de ejecutar semillas **solo** en una instalación local de prueba, de forma explícita.
- La semilla demo reutiliza la empresa si su RUC coincide con el RUC demo; si no, crea una empresa
  demo separada. En ningún caso se ejecuta desde el instalador ni en producción.
- Lista de precio `Publico` por defecto y bodega de venta `PISO` para que el POS pueda operar.

**Criterios de aceptación:**

- [x] Una instalación limpia queda usable desde la UI sin comandos SQL manuales.
- [x] No se crea `admin/admin` ni ningún dato de demostración.
- [x] El asistente no ofrece cargar semillas ni datos demo.
- [x] El administrador define su propia contraseña y se le obliga a cambiarla si se generó una
  temporal.
- [x] La instalación queda en modo `UNICA_EMPRESA` por defecto; el modo multiempresa es explícito.
- [x] El asistente puede reanudarse o corregirse si falla la conexión o una validación.

**Cierre Fase 1 (2026-10-19):** asistente JavaFX de 4 pasos, `V8`, creación transaccional de
empresa/sucursal/bodega/caja/administrador, módulos por perfil, secuencias SRI, configuración y
lista de precio inicial. 79 pruebas verdes en su cierre (77 `core` + 2 `dev`).

**Fuera de alcance:** empaquetado nativo, panel de instancias y actualizaciones automáticas.

### Fase 2 — Empaquetado y operación `[EN CURSO]`

**Objetivo:** entregar un producto instalable y operable en campo.

**Implementado en esta etapa:**

- **Pool HikariCP** con configuración `db.pool.*`.
- **Bloqueo de migraciones** con advisory lock para evitar ejecuciones concurrentes.
- **Respaldo verificado**: `pg_dump` + `pg_restore --list`, con prueba real de restauración
  sobre base temporal (91 tablas, `V1`–`V9`).
- **CLI operativa**: `Arranque respaldo` (sin sesión de usuario, para tareas del sistema
  operativo) y `Arranque salud` (healthcheck con código de salida 0/1).
- **Registro con rotación** en `~/.sfjfx/logs/sfjfx-YYYY-MM-DD.log`, con límite de días
  conservados y redirección de `System.out`/`System.err`.
- **Estado de la instalación**: versión, entorno, PostgreSQL, esquema, licencia, salud y último
  respaldo.
- **Licencia perpetua + mantenimiento anual**: archivo firmado RSA/SHA-256, validación offline
  contra `codigo_unico`, clave pública embebida en el build, pantalla de carga y herramienta
  `LicenciaEmisor` en `dev`.
- **Modo consulta** (decisión 14.12): con la licencia vencida o el periodo de prueba agotado se
  bloquean ventas, compras, comprobantes y movimientos de stock, pero se sigue consultando y
  exportando. Banda de aviso en la cabecera.
- **Canal de actualización**: manifiesto firmado, verificación de firma, descarga y SHA-256.
- **Plantillas de distribución**: `dist/README.md`, constructores y actualizadores Linux/Windows.
- **Documentación operativa** con checklist, rollback y salto mayor de PostgreSQL.

**Entregables pendientes de validación en entorno de release:**

- Generar y firmar instaladores `app-image` Windows/Linux con PostgreSQL incluido.
- Validar el ciclo de vida del PostgreSQL incluido (servicio, data directory, upgrades).
- Ejecutar una actualización de punta a punta desde una versión anterior y su rollback.

**Criterios de aceptación:**

- [ ] Instalación completa en Windows y Linux siguiendo solo la documentación.
- [ ] El instalador deja PostgreSQL operativo y la aplicación conectada sin intervención manual.
- [x] Respaldo y restauración verificados sobre una base con datos reales de prueba.
- [ ] Actualización remota asistida funciona desde una versión anterior con aprobación del
  técnico, respaldo previo y reporte de resultado.
- [ ] Si una actualización falla, se puede restaurar el respaldo y volver a la versión anterior.
- [ ] Actualización de versión aplica migraciones pendientes sin duplicar ni perder datos.
- [x] La actualización de la aplicación no fuerza un salto mayor de PostgreSQL sin respaldo y
  procedimiento explícito.
- [x] La licencia firmada offline se valida contra el ID de instalación y los límites declarados.
- [x] Sin licencia vigente la aplicación pasa a modo consulta tras el periodo de prueba, y
  bloquea la operación comercial sin perder la consulta ni los datos.
- [x] La instalación reporta versión, estado de la base, licencia y último respaldo.
- [x] Un fallo de migración no deja la base en estado inconsistente (transacciones + bloqueo).
- [x] El respaldo programado puede ejecutarse sin sesión de usuario (tarea del sistema operativo).
- [x] Existe un healthcheck verificable con código de salida para monitorización.

**Fuera de alcance:** panel central de clientes y multi-tenant compartido.

### Fase 3 (opcional) — Gestión de instancias

**Objetivo:** operar varias instalaciones sin compartir base de datos.

**Entregables:**

- Panel de control de instancias: inventario de clientes, versión, último respaldo, salud y
  estado de licencia/mantenimiento.
- Diagnóstico remoto opt-in y notificaciones de respaldos fallidos.
- Automatización de aprovisionamiento (crear instalación/base/configuración inicial).
- Supervisión de versiones y de actualizaciones remotas asistidas (sin ejecutarlas en automático).
- Portal opcional de renovación de licencias y activación, sin dependencia para operar.

**Criterios de aceptación:**

- [ ] Se pueden listar y monitorear N instalaciones sin acceder a los datos de negocio de cada
  cliente.
- [ ] El aprovisionamiento de una instancia nueva es repetible y auditable.
- [ ] El panel no requiere una base compartida de datos de negocio.
- [ ] El estado de licencia y mantenimiento se consulta sin exponer datos de negocio.
- [ ] Las actualizaciones siguen siendo aprobadas por un técnico; el panel no las aplica en
  automático.

**Fuera de alcance:** multi-tenant compartido y actualización automática por flota. El
multi-tenant solo se reevaluará si se cumplen las condiciones de la sección 4A.

---

## 7. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Las semillas siguen ejecutándose por costumbre en producción. | Fase 0: valor por defecto “solo migraciones”, bandera explícita, aviso en consola y prueba automatizada. |
| El instalador se vuelve un proyecto aparte y se retrasa. | Fase 1 acotada al primer arranque; Fase 2 al empaquetado. No mezclar con nuevas funcionalidades de negocio. |
| Actualizaciones divergentes entre clientes. | Versión visible, migraciones idempotentes, procedimiento de actualización y registro por instalación. |
| Actualización mayor de PostgreSQL incluido. | Versión de PostgreSQL fijada por instalador, respaldo previo, actualización separada de la aplicación y prueba de restauración. |
| Respaldos que nunca se restauran. | Prueba de restauración obligatoria en Fase 2 y verificación periódica. |
| Fuga de credenciales o certificados. | Sin credenciales por defecto; secretos fuera del repositorio; permisos mínimos de BD. |
| Compromiso de la clave privada de firma de licencias o paquetes. | Custodia offline, acceso restringido, rotación de claves y clave pública embebida en la app. |
| Servidor de actualizaciones comprometido. | HTTPS, paquetes firmados, verificación de integridad y aprobación del técnico antes de aplicar. |
| Vencimiento de mantenimiento que bloquee la caja. | Regla explícita: el vencimiento solo detiene updates/soporte; ventas y datos siguen operativos. |
| Reloj del sistema manipulado para extender la licencia. | Validación contra la última fecha conocida y registro de la licencia; no bloquear la operación por esto. |
| Complejidad operativa al crecer. | Fase 3 opcional con panel de instancias y automatización. |

---

## 8. Fuera de alcance

- Multi-tenant compartido con `tenant_id` y RLS.
- Esquema por tenant como opción por defecto.
- Sincronización offline avanzada.
- Envío real al SRI (sigue simulado).
- Facturación SaaS automatizada, pasarela de cobro y cobro recurrente en línea.
- Actualización automática por flota o desatendida.
- Renovación de licencias totalmente automática; la renovación offline es el flujo base.

---

## 9. Decisiones de revisión

### Confirmadas (2026-10-19)

| # | Tema | Decisión |
|---|------|----------|
| 1 | Plataformas | **Windows y Linux**. Instalador nativo para ambos. |
| 2 | PostgreSQL | **Incluido en el instalador**. Instancia local administrada por el instalador; Docker como alternativa para servidor. |
| 3 | Modo por defecto | **`UNICA_EMPRESA`**; `MULTIEMPRESA` se habilita explícitamente como opción avanzada. |
| 4 | Semillas | **Nunca en el instalador ni en producción.** Solo herramienta local del desarrollador, ejecutada manualmente en su PC. |

### 9.1 Decisiones de despliegue (2026-10-20)

| # | Tema | Decisión |
|---|------|----------|
| 1 | Alcance del producto | **Solo escritorio.** No se implementa modo servidor ni API. |
| 2 | Formato de distribución | **`app-image`** como formato base para Windows y Linux. |
| 3 | PostgreSQL incluido | **Sí**, con puerto libre detectado y credenciales aleatorias. |
| 4 | Canal de actualizaciones | **Servidor HTTPS propio** con manifiesto firmado. |
| 5 | Licencia vencida o ausente | **Modo consulta**: sin operación comercial, con consulta y datos intactos. |
| 6 | Clave pública de licencias | **Embebida en el build**; la privada se custodia fuera del repositorio. |
| 7 | Respaldos programados | **Tarea del sistema operativo** (`Arranque respaldo`), sin sesión de usuario. |

### 9.2 Licencia perpetua + mantenimiento anual

**Modelo comercial**

- Pago único por la licencia perpetua de la versión adquirida.
- Mantenimiento anual renovable que incluye actualizaciones, nuevas versiones y soporte.
- La edición (`SUPERMERCADO`, `MINIMERCADO`, `TIENDA`) y los límites (sucursales, cajas) se
  declaran en la licencia.
- El mantenimiento vencido **no bloquea** la operación; detiene actualizaciones y soporte.

**Contenido del archivo de licencia**

| Campo | Uso |
|---|---|
| `versionLicencia` | Formato/versionado del archivo de licencia (hoy `1`). |
| `idInstalacion` | Código único de la instalación (`instalacion.codigo_unico`). |
| `cliente` | Nombre comercial del titular. |
| `ruc` | Identificación fiscal del titular. |
| `edicion` | `SUPERMERCADO`, `MINIMERCADO` o `TIENDA`. |
| `maxSucursales` / `maxCajas` | Límites declarados. |
| `emitidaEn` | Fecha de emisión. |
| `mantenimientoHasta` | Fin del período de mantenimiento contratado. |
| `firma` | Firma digital RSA/SHA-256 del payload con la clave privada del proveedor. |

**Validación**

- La aplicación valida la firma, el `idInstalacion` y los límites con la clave pública embebida.
- No requiere conexión a internet.
- Si el archivo falta, no coincide o está corrupto, se aplica un período de prueba o gracia
  definido en la Fase 2. Nunca se eliminan ni se ocultan los datos del cliente.
- La renovación consiste en reemplazar el archivo de licencia; no requiere reinstalar.
- La generación se hace con una herramienta interna. La clave privada nunca se distribuye con la
  aplicación.

**Comportamiento**

| Estado | Operación y ventas | Actualizaciones | Soporte |
|---|---|---|---|
| Licencia vigente | Sí | Sí | Sí |
| Licencia válida, mantenimiento vencido (en gracia) | Sí | No | No (renovable) |
| Licencia válida, mantenimiento vencido (vencida la gracia) | No — modo consulta | No | No |
| Sin licencia (en periodo de prueba) | Sí | No | No |
| Sin licencia (agotado el periodo de prueba) | No — modo consulta | No | No |

**Regla de oro:** la licencia no secuestra datos ni bloquea la caja por un problema
administrativo más allá del período de gracia. En modo consulta se conserva el acceso a la
información y a los datos ya registrados.

### 9.3 Actualización remota asistida

1. El proveedor publica un manifiesto firmado y un paquete `.zip`.
2. En **Configuración → Actualizaciones**, el técnico consulta el canal.
3. La aplicación verifica la firma del manifiesto y descarga el paquete.
4. La aplicación verifica el SHA-256 del paquete.
5. El técnico cierra la aplicación y ejecuta `actualizar.sh`/`actualizar.ps1`.
6. El script respalda, reemplaza la aplicación, aplica migraciones y permite rollback.

El canal solo descarga y verifica. La aplicación del paquete es una acción explícita del técnico:
no hay actualización desatendida por flota.

### 9.4 Pendientes para la próxima sesión

Detalle operativo en [`../roadmap.md`](../roadmap.md#pendientes-para-la-próxima-sesión). Resumen:

1. **Cerrar la Fase 2 en entorno de release:** `app-image` de Linux y Windows, PostgreSQL
   portátil como servicio, instalación limpia verificada y firma de paquetes.
2. **Actualización de punta a punta:** publicar el canal HTTPS con manifiesto firmado, ejecutar el
   actualizador con respaldo previo y probar el rollback.
3. **Decisiones de producto:** ciclo de incompatibleidad de esquema (migraciones *forward-only*),
   valores comerciales de la gracia y el periodo de prueba, aplicación de los límites de licencia
   en la UI y artefacto independiente del emisor para el proveedor.
4. **Verificaciones:** respaldo con datos reales de cliente, modo consulta de extremo a extremo,
   migraciones desde el jar instalado, concurrencia del pool y rotación de logs con volumen alto.
5. **Deuda técnica menor:** trazas estructuradas en el log, advisory lock ante caída de red,
   orden de versiones de dos dígitos, formato único de la CLI y cobertura de las 91 tablas en el
   modelo de datos.

### 9.5 Cierre

No hay preguntas abiertas para el alcance de este ADR. Cualquier cambio futuro se registra como un
nuevo ADR o como una nueva fila en [`../decisiones.md`](../decisiones.md).

---

## 10. Referencias

- [`../decisiones.md`](../decisiones.md) — Fase 14, decisiones de despliegue.
- [`../roadmap.md`](../roadmap.md) — Hito 12, plan de despliegue.
- [`../datos-demo.md`](../datos-demo.md) — semillas y entornos de prueba.
- [`../modelo-datos.md`](../modelo-datos.md) — modelo de datos y jerarquía organizacional.
- [`../vistas-ui.md`](../vistas-ui.md) — asistente de instalación y pantallas de operación.
- `core/src/main/java/ec/sfjfx/core/arranque/Arranque.java` — arranque (solo migraciones desde Fase 0).
- `core/src/main/java/ec/sfjfx/core/servicio/ServicioInstalacion.java` — primer arranque transaccional.
- `core/src/main/java/ec/sfjfx/core/repositorio/InstalacionRepositorio.java` — metadatos de instalación.
- `core/src/main/resources/db/migracion/V8__instalacion.sql` — tabla `instalacion`.
- `ui/src/main/java/ec/sfjfx/ui/vistas/AsistenteInstalacion.java` — asistente JavaFX de 4 pasos.
- `dev/src/main/java/ec/sfjfx/dev/SemillaDesarrollo.java` — semilla de desarrollo (módulo `dev`).
- `dev/src/main/java/ec/sfjfx/dev/SemillaDemo.java` — semilla de demostración (módulo `dev`).
- `dev/src/main/java/ec/sfjfx/dev/GuardiaSemilla.java` — rechazo de semillas en producción.
- `core/src/main/java/ec/sfjfx/core/config/AppConfig.java` — configuración por instalación y entorno.
