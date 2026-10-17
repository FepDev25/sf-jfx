# Datos de demostración — sf-jfx

Semilla masiva para probar la aplicación sin cargar datos a mano. Es **idempotente**: se aplica
una sola vez y se registra en `configuracion` con la clave `demo.seed_version`.

## Cómo cargarla

Con PostgreSQL levantado y la configuración local lista:

```bash
docker compose up -d
mvn -pl core exec:java
```

`Arranque` aplica las migraciones (`V1`–`V7`), crea la empresa base si falta y luego ejecuta
`SemillaDemo`. Al final imprime las credenciales.

Si la semilla ya estaba aplicada, el comando no duplica datos. Para volver a empezar desde cero:

> **Advertencia:** el siguiente comando elimina el volumen de PostgreSQL y todos los datos.

```bash
docker compose down -v
docker compose up -d
mvn -pl core exec:java
```

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

1. **Cajero:** entrar con `cajero1` o `cajero2`, abrir el POS, agregar productos, aplicar una
   promoción/cupón, probar `F9` (venta rápida), `F8` (aparcar), `F7` (nueva venta en paralelo),
   descuento por línea con PIN `1234`/`3333` y cobrar con pago mixto.
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
