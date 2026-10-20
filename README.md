# sf-jfx

Sistema de facturación para supermercados (extensible a minimercados y tiendas).
Backend en **Java core + JDBC sobre PostgreSQL**, frontend en **JavaFX**.

Nació como homenaje al primer sistema de facturación hecho a mano, ahora llevado a un
diseño profesional y modular.

## Estructura

```
sf-jfx/ (parent pom)
├── core/   Dominio, repositorios JDBC y reglas de negocio. Sin dependencia de la UI.
├── dev/    Herramientas de desarrollo (semillas). No forma parte del producto instalable.
├── ui/     Interfaz JavaFX (shell, Modo Caja, vistas por módulo).
├── dist/   Plantillas de empaquetado, actualización y operación (Linux/Windows).
└── docs/   Diseño: decisiones, modelo de datos, vistas de UI y roadmap.
```

## Requisitos

- JDK 21
- Maven 3.9+
- PostgreSQL (cliente/servidor 16+)

## Módulos funcionales

Organización multiempresa/sucursal, catálogo con variantes/kits, inventario con kardex y PPP,
compras y cuentas por pagar, ventas/POS/caja con arqueo, clientes y CxC, facturación electrónica
SRI simulada (XML firmado y RIDE), fidelización/promociones, reportes con exportación y
administración (dispositivos, respaldos, perfiles, importaciones y migraciones).

## Modelo de despliegue

El sistema se distribuye como **single-tenant auto-instalable**: cada cliente u operador tiene su
propia instalación y su propia base de datos PostgreSQL. **No** se adopta un SaaS multi-tenant
compartido.

- `empresa` es una entidad legal dentro de la instalación (útil para cadenas y holdings); no es
  un tenant de SaaS.
- Una base PostgreSQL por instalación. Si varios clientes comparten servidor PostgreSQL, cada uno
  usa una base independiente.
- Instalador `app-image` para **Windows y Linux**, con **PostgreSQL incluido** y administrado por
  el instalador. El alcance es **solo escritorio**: no hay modo servidor ni API.
- PostgreSQL con **puerto libre detectado** y **credenciales aleatorias** generadas en la
  instalación.
- Modo por defecto **`UNICA_EMPRESA`**; el modo `MULTIEMPRESA` es una opción avanzada explícita.
- **Instalación manual** con instalador y **actualizaciones remotas asistidas** desde un canal
  HTTPS propio, aprobadas por un técnico. No hay actualización automática por flota.
- **Licencia perpetua por instalación + mantenimiento anual**, mediante archivo firmado offline
  con clave pública embebida en el build. Sin licencia vigente la aplicación entra en **modo
  consulta**: no registra ventas, compras ni comprobantes, pero se puede seguir consultando,
  exportando y accediendo a los datos.
- El primer arranque se resuelve con un **asistente de instalación**, sin credenciales por
  defecto (Hito 12, Fase 1).
- Las semillas de desarrollo/demostración **no se incluyen en el instalador ni se ejecutan en
  producción**; son una herramienta local del desarrollador (Hito 12, Fase 0).

Decisión completa y plan de implementación:
[`docs/adr/0001-modelo-despliegue-single-tenant.md`](docs/adr/0001-modelo-despliegue-single-tenant.md).
Estado: **Fases 0 y 1 completadas; Fase 2 en curso; Fase 3 pendiente**.

## Compilar y probar

```bash
mvn clean package       # compila y ejecuta las 86 pruebas
mvn test                # solo pruebas
```

## Ejecutar la UI

Instala los módulos en el repositorio local (una vez, o cada vez que cambie `core`) y luego lanza la UI:

```bash
mvn install -DskipTests
mvn -pl ui javafx:run
```

> Por qué no `mvn -am -pl ui javafx:run`: al invocar un goal de plugin desde la línea de
> comandos, Maven lo ejecuta sobre **todos** los proyectos del reactor —incluido el `parent` y
> `core`, que no tienen `mainClass`— y falla. `mvn package` tampoco basta, porque deja `core`
> solo en `core/target` sin instalarlo en `~/.m2`.

## Primer arranque

Con la base vacía, la aplicación muestra el **asistente de instalación**:

1. Conexión a PostgreSQL y entorno (`Producción` o `Desarrollo local`).
2. Empresa, RUC, régimen, perfil y ambiente SRI.
3. Sucursal, bodega y caja.
4. Administrador inicial.

No se crean credenciales por defecto ni datos de demostración: el administrador define su propia
contraseña. Para una prueba local, elija **Desarrollo local** y, si desea datos de demostración,
ejecute `mvn -pl dev exec:java` después de instalar.

## Base de datos (Docker)

PostgreSQL corre en Docker, en el puerto **55432** (host) mapeado a 5432 (contenedor),
con volumen persistente `sfjfx-datos`.

```bash
docker compose up -d      # levanta la BD
docker compose ps         # estado / health
docker compose down       # detiene (conserva datos)
```

Credenciales de desarrollo: base `sfjfx`, usuario `sfjfx`, password `sfjfx`.

## Configuración

La conexión se lee de `sfjfx.properties` (no versionado). Cópialo y ajusta:

```bash
mkdir -p ~/.sfjfx
cp core/src/main/resources/sfjfx.properties.example ~/.sfjfx/sfjfx.properties
```

Para el contenedor Docker, `db.url=jdbc:postgresql://localhost:55432/sfjfx`.

Claves: `db.url`, `db.usuario`, `db.password`, `db.esquema`, `app.entorno` (`dev`/`prod`).
Si `app.entorno` no se define, se asume `prod`; para desarrollo local use `dev`.
Orden de búsqueda: `-Dsfjfx.config=<ruta>` → `$SFJFX_CONFIG` →
`~/.sfjfx/sfjfx.properties` → recurso `/sfjfx.properties` del classpath.

## Migraciones y semillas (solo desarrollo/demostración)

El esquema se versiona en `core/src/main/resources/db/migracion/` con el formato
`V<n>__descripcion.sql`. `MigradorEsquema` aplica las pendientes dentro de una
transacción y registra el historial en la tabla `migracion_esquema`.

La **Fase 0 ya está implementada**: `Arranque` solo aplica migraciones y nunca ejecuta semillas.

```bash
mvn -pl core exec:java              # arranque normal: solo migraciones
mvn -pl core install -DskipTests    # instala core actualizado en ~/.m2 (una vez)
mvn -pl dev exec:java               # semilla demo completa (solo desarrollo/local)
mvn -pl dev exec:java -Dexec.mainClass=ec.sfjfx.dev.SemillaDesarrollo  # solo empresa base
```

El módulo `dev` no es dependencia de `core` ni de `ui`; por eso no llega al instalador ni al
paquete de producción. Las semillas se rechazan si `app.entorno=prod`.

La semilla de demostración es idempotente y crea una empresa con dos sucursales, usuarios con
distintos roles, catálogo, stock/lotes, clientes, proveedores, ventas, comprobantes, compras,
promociones, cupones, dispositivos y reportes guardados.

Credenciales (password / PIN):

| Usuario | Password | PIN | Rol |
|---------|----------|-----|-----|
| `admin` | `admin` | `1234` | Administrador (superadmin) |
| `gerente` | `gerente123` | `1111` | Gerente |
| `supervisor` | `supervisor123` | `2222` | Supervisor |
| `cajero1` | `cajero123` | `3333` | Cajero · Matriz |
| `cajero2` | `cajero123` | `3333` | Cajero · Norte |
| `bodeguero` | `bodeguero123` | `4444` | Bodeguero |
| `contador` | `contador123` | `5555` | Contador |
| `consulta` | `consulta123` | `6666` | Consulta (solo lectura) |

Ver [`docs/datos-demo.md`](docs/datos-demo.md) para el detalle de los datos, escenarios de
prueba y cómo reiniciar el entorno.

## Atajos de teclado

- POS: seleccione un producto, ajuste la cantidad y use **Agregar**; el escaneo con `Enter` agrega una unidad.
- Listados: `F5` actualizar, `Ctrl+F` enfocar búsqueda.
- POS: `F2` buscar, `F7` nueva venta en paralelo, `F8` aparcar, `F9` venta rápida de un ítem,
  `F11` pantalla completa, `F12` cobrar, `Suprimir` quitar la línea seleccionada del carrito.

## Operación (Fase 2)

- **Pool HikariCP** configurable con `db.pool.*`.
- **Respaldos verificados** con `pg_dump` + `pg_restore --list`; la restauración se prueba desde
  **Configuración → Respaldos**.
- **Comandos de operación** (sin sesión de usuario, pensados para tareas del sistema operativo):
  ```bash
  mvn -pl core exec:java -Dexec.args="respaldo"   # genera y verifica un respaldo
  mvn -pl core exec:java -Dexec.args="salud"      # healthcheck: código de salida 0/1
  ```
- **Registro con rotación** en `~/.sfjfx/logs/sfjjx-YYYY-MM-DD.log`.
- **Licencia perpetua + mantenimiento anual** en archivo firmado offline. La pantalla
  **Configuración → Licencia** muestra el código de instalación y permite cargar/renovar.
- **Modo consulta**: sin licencia vigente (tras el periodo de prueba) o con mantenimiento vencido
  (tras la gracia), se bloquean ventas, compras, comprobantes y movimientos de stock; se puede
  seguir consultando, exportando y accediendo a los datos.
- **Actualizaciones asistidas** en **Configuración → Actualizaciones**: manifiesto firmado,
  descarga y verificación SHA-256. La aplicación del paquete la ejecuta el técnico con el
  instalador/script.
- **Estado de la instalación** en **Configuración → Estado**: versión, PostgreSQL, esquema,
  licencia, salud y último respaldo.
- Plantillas de distribución y procedimiento de rollback: [`dist/README.md`](dist/README.md).

## Respaldos

Los respaldos usan `pg_dump`/`pg_restore` del cliente PostgreSQL (deben estar en el `PATH`) y se
configuran desde **Configuración → Respaldos**. El directorio por defecto es `~/.sfjfx/respaldos`.
Cada respaldo se verifica con `pg_restore --list` y la UI permite verificar o restaurar un archivo.
Antes de una actualización, genere y verifique un respaldo; consulte
[`dist/README.md`](dist/README.md) para el procedimiento de rollback.

## Estado

- **Hitos 0 a 11 cerrados**: cimientos, organización/seguridad, catálogo, inventario, compras,
  ventas/POS/caja, clientes/pedidos, facturación SRI (simulada), fidelización/promociones,
  reportes/analítica, administración/operación y **pulido/aseguramiento** (paginación, índices,
  atajos de teclado, accesibilidad y 72 pruebas automatizadas).
- **Hito 12 — Despliegue auto-instalable:** **Fases 0 y 1 completadas; Fase 2 en curso.** Fase 0:
  arranque solo migraciones y semillas aisladas en `dev`. Fase 1: asistente de primer arranque con
  `V8`. Fase 2: HikariCP, advisory lock, respaldo verificado, licencia offline, modo consulta,
  estado de instalación y canal de updates firmado; instaladores y actualización de punta a
  punta pendientes de validación en release. 86 pruebas verdes. Ver
  [ADR-0001](docs/adr/0001-modelo-despliegue-single-tenant.md).
- Base de datos Docker (PostgreSQL 17, puerto 55432): 91 tablas (incluida `migracion_esquema`;
  `V1`–`V9`) + datos base. Los datos de demostración son opcionales y solo para desarrollo/local.
- El alcance funcional quedó completo; sigue mantenimiento evolutivo, validación con uso real y
  la implementación del despliegue auto-instalable.

Ver [`docs/`](docs/) para el diseño completo y [`docs/roadmap.md`](docs/roadmap.md) para el avance.
