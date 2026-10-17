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

## Compilar

```bash
mvn clean package
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

## Migraciones

El esquema se versiona en `core/src/main/resources/db/migracion/` con el formato
`V<n>__descripcion.sql`. `MigradorEsquema` aplica las pendientes dentro de una
transacción y registra el historial en la tabla `migracion_esquema`.

```bash
mvn -pl core exec:java     # aplica las migraciones pendientes
```

## Estado

- **Hitos 0 a 9 cerrados**: cimientos, organización/seguridad, catálogo, inventario, compras,
  ventas/POS/caja, clientes/pedidos, facturación SRI (simulada), fidelización/promociones y
  **reportes/analítica** (dashboard con KPIs y gráficos, 22 reportes en 6 grupos, vistas guardadas
  y exportación CSV/XLSX/PDF).
- Base de datos Docker (PostgreSQL 17, puerto 55432): 87 tablas (`V1`–`V5`) + datos base.
- Usuario demo: `admin` / `admin`.
- Siguiente: **Hito 10 — Administración y operación** (dispositivos, respaldo/restauración,
  consola de módulos, importaciones masivas y estado de migraciones).

Ver [`docs/`](docs/) para el diseño completo y [`docs/roadmap.md`](docs/roadmap.md) para el avance.
