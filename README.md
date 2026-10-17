# sf-jfx

Sistema de facturación para supermercados (extensible a minimercados y tiendas).
Backend en **Java core + JDBC sobre PostgreSQL**, frontend en **JavaFX**.

Nació como homenaje al primer sistema de facturación hecho a mano, ahora llevado a un
diseño profesional y modular.

## Estructura

```
sf-jfx/ (parent pom)
├── core/   Dominio, repositorios JDBC y reglas de negocio. Sin dependencia de la UI.
├── ui/     Interfaz JavaFX (shell, Modo Caja, vistas por módulo).
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

## Compilar y probar

```bash
mvn clean package       # compila y ejecuta las 72 pruebas
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

Claves: `db.url`, `db.usuario`, `db.password`, `db.esquema`.
Orden de búsqueda: `-Dsfjfx.config=<ruta>` → `$SFJFX_CONFIG` →
`~/.sfjfx/sfjfx.properties` → recurso `/sfjfx.properties` del classpath.

## Migraciones y datos de demostración

El esquema se versiona en `core/src/main/resources/db/migracion/` con el formato
`V<n>__descripcion.sql`. `MigradorEsquema` aplica las pendientes dentro de una
transacción y registra el historial en la tabla `migracion_esquema`.

```bash
mvn -pl core exec:java     # aplica migraciones + semilla base + datos masivos de demo
```

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

- Listados: `F5` actualizar, `Ctrl+F` enfocar búsqueda.
- POS: `F2` buscar, `F7` nueva venta en paralelo, `F8` aparcar, `F9` venta rápida de un ítem,
  `F11` pantalla completa, `F12` cobrar.

## Respaldos

Los respaldos usan `pg_dump`/`pg_restore` del cliente PostgreSQL (deben estar en el `PATH`) y se
configuran desde **Configuración → Respaldos**. El directorio por defecto es `~/.sfjfx/respaldos`.

## Estado

- **Hitos 0 a 11 cerrados**: cimientos, organización/seguridad, catálogo, inventario, compras,
  ventas/POS/caja, clientes/pedidos, facturación SRI (simulada), fidelización/promociones,
  reportes/analítica, administración/operación y **pulido/aseguramiento** (paginación, índices,
  atajos de teclado, accesibilidad y 72 pruebas automatizadas).
- Base de datos Docker (PostgreSQL 17, puerto 55432): 90 tablas (`V1`–`V7`) + datos base y demo.
- El alcance congelado quedó completo; sigue mantenimiento evolutivo y validación con uso real.

Ver [`docs/`](docs/) para el diseño completo y [`docs/roadmap.md`](docs/roadmap.md) para el avance.
