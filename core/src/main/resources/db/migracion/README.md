# Migraciones versionadas del esquema

Formato del archivo: `V<version>__<descripcion>.sql` (por ejemplo `V7__indices_rendimiento.sql`).

El proyecto incluye las migraciones `V1` a `V7`:

- `V1` esquema inicial completo.
- `V2` datos base (permisos, roles, unidades, impuestos, medios de pago y tipos de movimiento).
- `V3` historial de precios y costos.
- `V4` cuentas por cobrar de clientes.
- `V5` reportes guardados.
- `V6` detalle de respaldos.
- `V7` índices de rendimiento.

`MigradorEsquema` aplica las pendientes dentro de una transacción y registra el historial en
`migracion_esquema`. Ejecute:

```bash
mvn -pl core exec:java
```

El mismo comando aplica la semilla base y los datos masivos de demostración
(ver `docs/datos-demo.md`).
