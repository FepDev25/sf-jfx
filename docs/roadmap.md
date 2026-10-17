# Roadmap de Construcción — sf-jfx

## Cómo trabajamos

El sistema se construye **hito por hito**, en orden. Reglas del proceso:

1. **Un hito a la vez.** No se abre el siguiente hasta cerrar el actual.
2. **Cada hito entrega algo verificable**: compila (`mvn clean package`), con pruebas verdes, y
   funcionalmente usable (aunque sea por pantalla mínima).
3. **Un hito solo se marca cerrado** cuando *todos* sus puntos están tildados y se anota en el
   [Registro de cierre](#registro-de-cierre) con su fecha.
4. Los puntos se tildan a medida que se completan; los hitos en progreso se etiquetan `[EN CURSO]`.
5. Cambios de alcance se registran en [`decisiones.md`](decisiones.md), no aquí.

**Marcas:** `[x]` hecho · `[ ]` pendiente · la cabecera del hito lleva su estado.

> Sin prisa de MVP (decisión 2.2): se construye completo, con calidad, priorizando que cada entrega quede sólida.

## Arquitectura de módulos (Maven)

```
sf-jfx (parent pom)
├── core   → dominio, entidades POJO, repositorios JDBC, servicios, reglas de negocio,
│            migraciones SQL versionadas, utilidades (validación RUC/cédula, dinero, fechas),
│            generación XML SRI, integración ZXing e iText.
└── ui     → JavaFX: shell, vistas por módulo, Modo Caja, temas CSS, controladores, MVVM.
```

`core` no depende de `ui`. Toda la lógica y el acceso a datos vive en `core` (facilita pruebas y un futuro modo offline).

## Estado general

| Hito | Nombre | Estado |
|------|--------|--------|
| 0 | Cimientos | `HECHO` |
| 1 | Organización y seguridad | `HECHO` |
| 2 | Catálogo | `HECHO` |
| 3 | Inventario y bodegas | `HECHO` |
| 4 | Compras y proveedores | `HECHO` |
| 5 | Ventas, POS y caja | `HECHO` |
| 6 | Clientes y pedidos | `HECHO` |
| 7 | Facturación electrónica (SRI) | `HECHO` |
| 8 | Fidelización, precios y promociones | `HECHO` |
| 9 | Reportes y analítica | `HECHO` |
| 10 | Administración y operación | `HECHO` |
| 11 | Pulido y aseguramiento | `HECHO` |

---

## Hito 0 — Cimientos `[HECHO]`

Base técnica: proyecto, base de datos, migraciones, shell y sesión.

- [x] Scaffold Maven multi-módulo (`parent` + `core` + `ui`), Java 21.
- [x] Configuración por archivo (`AppConfig`) y conexión JDBC a PostgreSQL (`ConexionBD`).
- [x] Motor de **migraciones versionadas** (`MigradorEsquema`) + arranque unificado (`Arranque`).
- [x] `V1` — esquema inicial completo (86 tablas).
- [x] `V2` — datos base: 161 permisos, 6 roles, unidades de medida, impuestos SRI, medios de pago, tipos de movimiento.
- [x] Shell JavaFX navegable (sidebar agrupado + cabecera + área de contenido).
- [x] Login (contraseña con **PBKDF2**, bloqueo por intentos) y gestión de sesión (`Sesion`, `SesionActual`).
- [x] Selección de sucursal/caja en la cabecera.
- [x] **Feature flags**: módulos activos por empresa y visibilidad por permisos.
- [x] **Mockups** (login, shell/dashboard, POS, editor de producto) en `docs/mockups/`.

**Cierre del hito:** la app arranca, autentica (admin/admin), muestra el shell con los módulos activos y la BD tiene sus datos base. ✔

---

## Hito 1 — Organización y seguridad `[HECHO]`

- [x] Módulo `core`: entidades y repositorios de Empresa, Sucursal, Bodega, Caja (CRUD con `Bd`/transacciones).
- [x] Usuarios, Roles, Permisos (matriz módulo × acción) y asignación usuario↔sucursal.
- [x] Auditoría de acciones sensibles con antes/después (JSONB) + registro desde los servicios.
- [x] Configuración (parámetros clave/valor) y `empresa_modulo` (feature flags).
- [x] Servicios con validación (RUC/cédula EC módulo 11, email, duplicados) y auditoría.
- [x] UI: Empresa / Sucursales / Bodegas / Cajas (bajo Configuración).
- [x] UI: Usuarios / Roles y permisos (matriz) / Auditoría / Configuración / Módulos.
- [x] Botones y acciones de UI restringidos por permiso (`Perm`).
- [x] Pruebas de reglas de permisos y alcance por sucursal.

**Cierre:** se crea empresa, sucursales, bodegas, cajas, usuarios y roles; los permisos restringen la UI. ✔
_(Historial específico de precios/costos se activará al existir productos — Hito 2.)_

---

## Hito 2 — Catálogo `[HECHO]`

- [x] Categorías (árbol jerárquico), Marcas, Unidades de medida, Impuestos, Listas de precios.
- [x] Producto: SKU, costos (último y PPP), impuestos múltiples, códigos de barras (múltiples y tipificados), empaques con factor.
- [x] Imágenes (galería + principal, copiadas a `~/.sfjfx/uploads`), atributos personalizados por categoría.
- [x] Variantes (padre-hijo) y kits con componentes; granel/PLU.
- [x] Precios por lista (con vigencia; listas por tipo de cliente). Override por sucursal soportado en datos.
- [x] **Historial de precios/costos** (tabla `historial_precio`, registrado por el servicio).
- [x] Importar / Exportar catálogo (CSV) con validación y resumen.
- [x] Impresión de etiquetas / códigos de barras con **ZXing** (PNG + PDF con iText).
- [x] UI: lista y editor de producto (pestañas: General, Precios, Impuestos, Códigos, Empaques, Imágenes, Atributos, Componentes) y catálogos auxiliares.

**Cierre:** catálogo completo cargable y navegable; precios e impuestos resolviéndose por lista. ✔

---

## Hito 3 — Inventario y bodegas `[HECHO]`

- [x] Stock por (bodega, producto, lote) con cantidad reservada.
- [x] Kardex (`movimiento_inventario`) como fuente de verdad; tipos de movimiento (seed `V2`).
- [x] Lotes/vencimientos y números de serie (registro y estados).
- [x] Costeo **PPP** (actualizado al ingresar; registra historial de costo).
- [x] Ajustes de inventario (entrada/salida con motivo), transaccionales.
- [x] Transferencias entre bodegas/sucursales con estados (borrador → en tránsito → recibida); la mercadería en tránsito queda trazada por el documento.
- [x] Tomas de inventario con **conteo ciego** configurable, cálculo de diferencias y ajuste al aplicar.
- [x] UI: Stock, Kardex, Ajustes, Transferencias, Tomas, Lotes, Series.

**Cierre:** todo movimiento de stock queda trazado en el kardex y el inventario se valoriza a PPP. ✔

---

## Hito 4 — Compras y proveedores `[HECHO]`

- [x] Proveedores (datos fiscales, retenciones) y multi-proveedor por producto con histórico de costos.
- [x] Solicitud de compra (opcional) y aprobación/rechazo.
- [x] Órdenes de compra con seguimiento de cantidades recibidas (borrador → aprobada → parcial → recibida).
- [x] Recepciones parciales/múltiples (lote/vencimiento) que actualizan stock y **PPP**.
- [x] Facturas de proveedor, Cuentas por pagar y pagos parciales con múltiples medios.
- [x] **Comprobante de retención** (IVA y Renta). _El RIDE/XML SRI se generará en el Hito 7._
- [x] Devoluciones a proveedor (sale stock, reduce el saldo de la factura).
- [x] UI: proveedores, solicitudes, órdenes, recepciones, facturas, CxP/pagos, retenciones, devoluciones.

**Cierre:** ciclo de compra completo desde la orden hasta el pago y la retención. ✔

---

## Hito 5 — Ventas, POS y caja `[HECHO]`

- [x] Sesión de caja: apertura, arqueo por denominación, cierre y diferencias.
- [x] **Modo Caja (POS)**: carrito, búsqueda por nombre/código, escaneo (Enter), totales grandes.
- [x] Atajos de teclado (F2 buscar, F12 cobrar, F8 aparcar) y **pantalla completa** (F11).
- [x] Apartar y recuperar ventas (aparcadas).
- [x] Pagos con múltiples medios y **pago mixto** con cálculo de vuelto.
- [x] Descuento **global** y **propina** configurables.
- [x] Devoluciones de venta (parcial/total) con reintegro de stock.
- [x] UI: consulta de ventas (ver/anular) e historial de sesiones de caja.

**Cierre:** una venta se cobra, descuenta stock y se cierra el turno con arqueo. ✔
_Completado en Hito 11: descuento **por línea** con autorización por PIN, **venta rápida 1-ítem** (F9) y **nueva venta en paralelo** usando carritos aparcados (F7/F8). El enganche de la **Nota de Crédito** se hizo en el Hito 7._

---

## Hito 6 — Clientes y pedidos `[HECHO]`

- [x] Clientes (consumidor final / persona natural / empresa), contactos y múltiples direcciones.
- [x] Crédito: límite, plazo, **abonos**, **bloqueo por exceso** y Cuentas por cobrar (tabla `cxc_movimiento`, migración `V4`).
- [x] Ficha de cliente con historial de compras, número de compras y **ticket promedio**.
- [x] Pedidos con flujo de estados (Recibido → … → Entregado/Cancelado) y **conversión a venta** (pickup/delivery).
- [x] UI: clientes, ficha, CxC (estado de cuenta y abonos) y tablero de pedidos.

**Cierre:** pedidos gestionables de punta a punta y saldos de clientes correctos. ✔
_Productos "favoritos" por cliente → REEVALUABLE (Hito 9, analítica)._

---

## Hito 7 — Facturación electrónica (SRI, simulada)

- [x] Numeración establecimiento (3) + punto de emisión (3) + secuencial (9) con control de consecutivos.
- [x] Comprobantes: **Factura (01)**, **Nota de Crédito (04)**, **Nota de Débito (05)**, **Retención (07)**, **Guía de Remisión (06)** y **Liquidación de Compra (03)** (estructuras XML para los 6; emisión automática de Factura/NC/Retención).
- [x] Clave de acceso de 49 dígitos (módulo 11) y ficha del emisor (RUC, régimen, ambiente/tipo de emisión).
- [x] **XML con estructura SRI** + **firma XMLDSig enveloped** con certificado `.p12` (el refuerzo **XAdES-BES** completo queda como REEVALUABLE).
- [x] **RIDE en PDF** (iText) con clave de acceso y código de barras (ZXing).
- [x] Estados del comprobante; anulación con motivo. **Sin envío real al SRI** (enchufable).
- [x] UI: comprobantes emitidos, detalle, reimpresión de RIDE, anulación y certificados.

**Cierre:** se emite una factura desde una venta y se obtienen XML (firmado si hay certificado) + RIDE válidos. ✔
_Emisión automática de ND/Guía/Liquidación y XAdES-BES completo → REEVALUABLE._

---

## Hito 8 — Fidelización, precios y promociones `[HECHO]`

- [x] Fidelización: acumulación de puntos por compra (regla configurable), movimientos y **canje como medio de pago**.
- [x] Listas de precio por tipo de cliente con **ajustes masivos por categoría** (con historial).
- [x] Promociones: **porcentaje, monto fijo, NxM/2da unidad, precio fijo**; **prioridad sin acumular**.
- [x] Alcance por fechas, sucursal, categoría/marca/producto.
- [x] Cupones con código y control de uso.
- [x] UI: programa de puntos, promociones, cupones y ajuste masivo (en Listas de precio).

**Cierre:** una promoción y un canje de puntos se aplican correctamente en caja. ✔
_Pendiente (REEVALUABLE): promociones tipo **combo** y alcance por **tipo de cliente**; botón "Promociones" en el POS aplica la mejor oferta por línea._

---

## Hito 9 — Reportes y analítica `[HECHO]`

- [x] Dashboard con KPIs en vivo y gráficos JavaFX (barras, líneas, pastel).
- [x] Reportería por grupos: Ventas/POS, Inventario, Compras/Proveedores, Financiero, Impuestos/SRI, Operativo/Caja.
- [x] Reportes parametrizables y guardables (fecha, sucursal, categoría, cajero).
- [x] Exportación **PDF / Excel (XLSX) / CSV**.
- [x] UI: dashboard + visor de reporte con filtros y gráficos.

**Cierre:** todos los grupos de reportes disponibles con exportación. ✔
_22 reportes en 6 grupos sobre el kardex, ventas, compras, CxC/CxP, comprobantes y caja. Migración `V5` para vistas guardadas; Apache POI para XLSX (decisión 0.8)._

---

## Hito 10 — Administración y operación `[HECHO]`

- [x] Dispositivos: impresora térmica 80mm, A4, lector de barras (HID), balanza, gaveta.
- [x] Respaldo / restauración (pg_dump/pg_restore) con programación e historial.
- [x] Consola de módulos y perfiles (feature flags) y parámetros del negocio.
- [x] Importaciones masivas (catálogo, clientes, proveedores).
- [x] Estado de migraciones de esquema.

**Cierre:** administración integral sin tocar la base de datos a mano. ✔
_Dispositivos con configuración JSON por sucursal/caja; respaldos manuales y diarios programados con historial (`V6`); perfiles Supermercado/Minimercado/Tienda; importación de clientes y proveedores desde CSV/XLSX con vista previa y validación; consola de migraciones con aplicación de pendientes._

---

## Hito 11 — Pulido y aseguramiento `[HECHO]`

- [x] Coherencia visual final, atajos de teclado, accesibilidad y validaciones.
- [x] Pruebas unitarias de `core`: reglas fiscales (clave de acceso, impuestos), costeo PPP, resolución de precios/promociones.
- [x] Pruebas de flujo de caja y de ciclo de venta.
- [x] Revisión de rendimiento en listados grandes (paginación, índices).

**Cierre:** proyecto estable, cubierto por pruebas y listo para uso real. ✔
_Paginación en servidor para productos, ventas, clientes, proveedores y comprobantes; índices de rendimiento (`V7`); atajos F5/Ctrl+F en listados y F7/F9 en POS; descuento por línea autorizado por PIN; venta rápida de un ítem; etiquetas accesibles y foco automático en formularios; formato monetario unificado; pruebas de clave de acceso/catálogo SRI, PPP, promociones, autorización por PIN, caja y paginación._

---

## Orden de mockups (previo a UI por hito)

- [ ] Shell + Login.
- [ ] Dashboard.
- [ ] Modo Caja (POS) + cobro.
- [ ] Editor de producto.
- [ ] Listado de productos y de ventas.

---

## Registro de cierre

| Hito | Fecha de cierre | Notas |
|------|-----------------|-------|
| 0 | 2026-10-16 | Scaffold, config, migrador, `V1` (86 tablas), `V2` (datos base), login/sesión, shell con feature flags y mockups. 11 pruebas verdes. |
| 1 | 2026-10-16 | CRUD de empresa/sucursal/bodega/caja/usuario/rol, permisos, auditoría, configuración y módulos; UI de administración restringida por permisos. 21 pruebas verdes. |
| 2 | 2026-10-16 | Catálogo completo: categorías/marcas/unidades/impuestos/listas, producto con códigos/empaques/imágenes/atributos/variantes/kits, historial de precios, CSV y etiquetas ZXing. `V3`. 25 pruebas verdes. |
| 3 | 2026-10-16 | Inventario: stock, kardex, PPP, ajustes, transferencias con estados, tomas con conteo ciego, lotes y series. 26 pruebas verdes. |
| 4 | 2026-10-16 | Compras: proveedores, solicitudes, órdenes, recepciones con PPP, facturas/CxP, pagos parciales, retenciones y devoluciones. 27 pruebas verdes. |
| 5 | 2026-10-16 | Ventas/POS/Caja: sesión con arqueo, Modo Caja (carrito, atajos, aparcar), pago mixto con vuelto, descuento global/propina, devoluciones con reintegro, consulta y sesiones. 28 pruebas verdes. |
| 6 | 2026-10-16 | Clientes (persona/empresa/consumidor final), direcciones/contactos, crédito con límite y bloqueo por exceso, CxC con abonos (`V4`), ficha con ticket promedio, pedidos con estados y conversión a venta. 29 pruebas verdes. |
| 7 | 2026-10-16 | Facturación SRI simulada: numeración, clave de acceso 49 dígitos, XML SRI (factura/NC/ND/retención/guía/liquidación), firma XMLDSig con `.p12`, RIDE PDF con código de barras, emisión desde venta/devolución/retención, anulación y certificados. 35 pruebas verdes. |
| 8 | 2026-10-16 | Fidelización (puntos por compra y canje), promociones (%, monto, NxM, precio fijo con prioridad sin acumular), cupones con uso, ajuste masivo de precios por categoría; integración en caja (acumular/canje) y botón Promociones/Cupón en POS. 36 pruebas verdes. |
| 9 | 2026-10-16 | Reportes y analítica: dashboard con KPIs y gráficos en vivo, 22 reportes en 6 grupos con filtros (fecha/sucursal/categoría/cajero), vistas guardadas (`V5`), exportación CSV/XLSX/PDF (Apache POI). 42 pruebas verdes. |
| 10 | 2026-10-16 | Administración: dispositivos (impresoras/lector/balanza/gaveta), respaldo y restauración con programación e historial (`V6`), perfiles de instalación, importación CSV/XLSX de clientes y proveedores con vista previa, consola de migraciones. 49 pruebas verdes. |
| 11 | 2026-10-16 | Pulido y aseguramiento: paginación en servidor (productos, ventas, clientes, proveedores, comprobantes), índices (`V7`), atajos F5/Ctrl+F y POS F7/F9, descuento por línea con PIN, venta rápida, accesibilidad y foco en formularios, formato monetario unificado, pruebas fiscales, PPP, promociones, autorización, caja y paginación. 72 pruebas verdes. |
