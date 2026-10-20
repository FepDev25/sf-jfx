# Datos de demostración — sf-jfx

Semilla masiva para probar la aplicación sin cargar datos a mano. Es **idempotente**: se aplica
una sola vez y se registra en `configuracion` con la clave `demo.seed_version`.

## Cómo cargarla (entorno local)

Con PostgreSQL levantado y la configuración local lista (`app.entorno=dev`):

```bash
docker compose up -d
mvn -pl core install -DskipTests    # instala core actualizado en ~/.m2
mvn -pl core exec:java              # solo migraciones
mvn -pl dev exec:java               # semilla demo completa (solo desarrollo/local)
```

> **Estado actual (Fases 0 y 1 implementadas; Fase 2 en curso):** `Arranque` solo aplica las
> migraciones (`V1`–`V9`). Las semillas viven en el módulo `dev`, que no es dependencia de `core`
> ni de `ui` y no se incluye en el producto instalable. Si `app.entorno=prod`, las semillas se
> rechazan con un error explícito.
>
> **Comandos operativos adicionales** (sin sesión de usuario, pensados para tareas del sistema
> operativo): `mvn -pl core exec:java -Dexec.args="respaldo"` genera y verifica un respaldo, y
> `mvn -pl core exec:java -Dexec.args="salud"` devuelve el estado de la instalación con código de
> salida 0/1.

Si la semilla ya estaba aplicada, el comando no duplica datos. Para volver a empezar desde cero:

> **Advertencia:** el siguiente comando elimina el volumen de PostgreSQL y todos los datos.

```bash
docker compose down -v
docker compose up -d
mvn -pl core install -DskipTests
mvn -pl core exec:java
mvn -pl dev exec:java
```

## Probar el asistente de instalación (Fase 1)

Para probar una instalación real en la PC del desarrollador:

1. Base limpia y migraciones aplicadas.
2. Lanzar la UI: `mvn -pl ui javafx:run`.
3. Completar el asistente: conexión, empresa/RUC/régimen, sucursal/bodega/caja y administrador.
   No hay credenciales por defecto ni datos de demostración.
4. Opcional: cargar la semilla demo con `mvn -pl dev exec:java` (solo si `app.entorno=dev`).
   Si la empresa instalada usa el RUC demo `1790011678001`, la semilla puebla esa empresa; si usa
   otro RUC, crea una empresa demo separada.

## Credenciales

| Usuario | Password | PIN | Rol / alcance |
|---------|----------|-----|----------------|
| `admin` | `admin` | `1234` | Administrador (superadmin) |
| `gerente` | `gerente123` | `1111` | Gerente · ambas sucursales |
| `supervisor` | `supervisor123` | `2222` | Supervisor · ambas sucursales |
| `cajero1` | `cajero123` | `3333` | Cajero · Matriz Quito |
| `cajero2` | `cajero123` | `3333` | Cajero · Sucursal Norte |
| `bodeguero` | `bodeguero123` | `4444` | Bodeguero · ambas sucursales |
| `contador` | `contador123` | `5555` | Contador · ambas sucursales |
| `consulta` | `consulta123` | `6666` | Solo lectura (permisos `VER`) |

Los PIN se usan para autorizar descuentos por línea en el POS. `consulta` permite validar que la
UI oculta acciones sin permiso.

## Contenido de la semilla

- **Organización:** 2 sucursales (Matriz Quito y Sucursal Norte), 4 bodegas, 2 cajas.
- **Usuarios:** 8 cuentas con roles distintos y un rol personalizado `Consulta`.
- **Catálogo:** 32 productos con códigos, códigos de barras EAN-13, categorías jerárquicas,
  marcas, impuestos IVA 15%/0%, listas `Público`, `Mayorista` y `Corporativo`.
- **Inventario:** stock inicial en ambas sucursales, lotes con vencimientos, movimientos iniciales
  en el kardex y 3 productos con stock bajo (incluye negativos) para probar alertas del dashboard.
- **Clientes:** 10 clientes (consumidor final, personas naturales y empresas), con crédito,
  límites, puntos de fidelidad y dirección principal.
- **Proveedores:** 6 proveedores con datos fiscales y retenciones configuradas.
- **Ventas:** 45 ventas distribuidas en los últimos 30 días, con pagos en efectivo, tarjeta,
  transferencia, crédito y una venta con pago mixto. Incluye 32 facturas electrónicas simuladas,
  movimientos de kardex, cargos de CxC y acumulación de puntos.
- **Compras:** 8 facturas de proveedor con detalles, pagos parciales/totales, cuentas por pagar,
  retenciones, una solicitud y una orden de compra.
- **Promociones:** 4 promociones activas (`%`, NxM, precio fijo y por marca) y 3 cupones.
- **Administración:** 6 dispositivos configurados, 3 reportes guardados y configuraciones del
  negocio.
- **Caja:** turnos cerrados con arqueo y una sesión abierta en cada caja para empezar a vender.

## Escenarios sugeridos

1. **Cajero:** entrar con `cajero1` o `cajero2`, abrir el POS, seleccionar productos con su cantidad y usar **Agregar**, aplicar una
   promoción/cupón, probar `F9` (venta rápida), `F8` (aparcar), `F7` (nueva venta en paralelo),
   descuento por línea con PIN `1234`/`3333`, quitar una línea con `Suprimir` y cobrar con pago mixto.
2. **Supervisor:** consultar ventas, anular una venta, revisar sesiones de caja y devoluciones.
3. **Bodeguero:** revisar stock, kardex, lotes por vencer, ajustes y transferencias.
4. **Contador:** revisar facturas de proveedor, CxP, retenciones, comprobantes emitidos y
   reportes de impuestos.
5. **Gerente:** abrir Reportes, ejecutar los 6 grupos, guardar una vista y exportar a CSV/XLSX/PDF.
6. **Admin:** administrar usuarios, roles, dispositivos, respaldos, perfiles de módulos,
   importaciones y estado de migraciones.
7. **Consulta:** verificar que las acciones de escritura no están disponibles.

## Notas

- Las fechas de ventas/compras son relativas al día en que se ejecuta la semilla.
- Los archivos de respaldo se guardan por defecto en `~/.sfjfx/respaldos`; `pg_dump` y
  `pg_restore` deben estar instalados.
- La empresa demo usa RUC `1790011678001`; no la use para producción.
