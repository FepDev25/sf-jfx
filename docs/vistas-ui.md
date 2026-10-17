# Vistas de UI — sf-jfx

Inventario de pantallas necesarias. Estructura: **ventana única** con sidebar + **Modo Caja** dedicado (fullscreen).
Dirección de arte: **moderno claro (SaaS)**, tipografía Inter/Roboto, iconos Ikonli, un solo tema (claro).
Rol que ve cada vista entre paréntesis. `[flag]` = módulo que debe estar activo.

---

## Transversales
- **Login** — usuario + contraseña (hash). Única sesión por caja.
- **Selección de sucursal/caja** — si el usuario tiene varias; elige terminal al abrir turno.
- **Shell principal** — sidebar colapsable por módulo, barra superior (empresa/sucursal, usuario, hora), área de contenido, notificaciones.
- **Dashboard** — KPIs (ventas del día, ticket promedio, top productos, alertas de stock/bajo inventario, CxC/CxP), gráficos JavaFX. (Todos, según permisos)
- **Listados** — paginación en servidor, búsqueda con `Ctrl+F` y actualización con `F5` en productos, ventas, clientes, proveedores y comprobantes; formato monetario unificado.
- **Bloqueo por inactividad / cierre de sesión**. (Todos)
- **Centro de notificaciones / alertas** — bajo stock, por vencer, sesiones de caja abiertas, respaldos fallidos. (Admin/Gerente)

## Modo Caja (POS) — `[POS]`
- **POS principal** — fullscreen, carrito grande, total visible, teclado, atajos F-keys, catálogo paginado (50 por página), búsqueda por nombre/código y escaneo. (Cajero)
- **Cobro / pagos** — medios múltiples, pago mixto, cálculo de vuelto, referencia de tarjeta, canje de puntos. (Cajero)
- **Aplicar descuento** — por línea (con autorización usuario + PIN) o global. (Cajero/Supervisor)
- **Venta rápida 1-ítem** — `F9`, cobra el carrito de un solo ítem en efectivo. (Cajero)
- **Ventas en paralelo** — `F7` aparca la venta actual y abre un carrito nuevo; se retoman con Recuperar. (Cajero)
- **Ventas aparcadas** — hold y recuperación; nueva venta en paralelo (`F7`). (Cajero)
- **Devolución en caja** — seleccionar venta/ítems, reintegro, generar nota de crédito. (Supervisor)
- **Apertura de turno** — monto inicial. (Cajero)
- **Cierre de turno / arqueo** — conteo por denominación, diferencia, observación. (Cajero/Supervisor)
- **Ticket impreso / RIDE rápido** — impresión térmica 80mm. (Cajero)

## Catálogo — `[PRODUCTOS]`
- **Lista de productos** — tabla con filtros, búsqueda, miniatura, stock, estado; acciones masivas. (Admin/Gerente)
- **Editor de producto** — pestañas: general, precios, impuestos, códigos de barras, empaques, imágenes, atributos, inventario/variantes. (Admin/Gerente)
- **Categorías** — árbol jerárquico editable. (Admin/Gerente)
- **Marcas**. (Admin/Gerente)
- **Unidades de medida**. (Admin/Gerente)
- **Impuestos** — tarifas con vigencia. (Admin/Contador)
- **Listas de precios** `[LISTAS_PRECIO]` — precios por producto, ajustes masivos por categoría. (Admin/Gerente)
- **Atributos personalizados** — definición por categoría. (Admin)
- **Importar/Exportar catálogo** — CSV/Excel, con vista previa y validación. (Admin)
- **Impresión de etiquetas/códigos de barras** (ZXing). (Admin/Gerente)

## Inventario — `[INVENTARIO]`
- **Stock actual** — por bodega, con filtros, bajo stock, por vencer. (Bodeguero/Gerente)
- **Kardex** — movimientos por producto. (Bodeguero/Gerente)
- **Lotes y vencimientos**. (Bodeguero)
- **Números de serie** — estado por unidad. (Bodeguero)
- **Transferencias** `[BODEGAS]` — lista + editor con estados. (Bodeguero/Gerente)
- **Tomas de inventario** — abrir conteo, capturar, calcular diferencias, aplicar. (Bodeguero/Gerente)
- **Ajustes de inventario** — entrada/salida con motivo. (Bodeguero/Supervisor)

## Bodegas — `[BODEGAS]`
- **Gestión de bodegas** — por sucursal, tipo, activo. (Admin)

## Compras — `[COMPRAS]`
- **Proveedores** — lista + editor con datos fiscales/retención. (Admin/Gerente)
- **Solicitudes de compra** — crear/aprobar. (Gerente)
- **Órdenes de compra** — lista + editor + aprobación + seguimiento de pendientes. (Gerente)
- **Recepciones** — desde OC, parcial/total, lote/vencimiento, actualiza costo PPP. (Bodeguero)
- **Facturas de proveedor** — registro, CxP, vencimientos. (Contador)
- **Cuentas por pagar** — saldos, pagos parciales con múltiples medios. (Contador)
- **Retenciones** — emisión del comprobante (IVA/Renta). (Contador)
- **Devoluciones a proveedor**. (Bodeguero/Contador)

## Ventas — `[POS]`
- **Consulta de ventas** — histórico con filtros, ver detalle, reimprimir RIDE, anular. (Supervisor/Gerente)
- **Pedidos** — tablero kanban por estado (recibido→entregado), conversión a venta. (Cajero/Gerente)
- **Devoluciones de venta** — gestión y notas de crédito. (Supervisor)
- **Sesiones de caja** — historial de turnos y arqueos, diferencias. (Supervisor/Gerente)

## Clientes — `[CLIENTES]`
- **Lista de clientes** + editor (datos fiscales, crédito). (Cajero/Gerente)
- **Ficha de cliente** — historial de compras, saldo, puntos, direcciones. (Cajero/Gerente)
- **Cuentas por cobrar** — estado de cuenta, abonos. (Contador/Gerente)
- **Fidelización** `[FIDELIZACION]` — saldo y movimientos de puntos, reglas. (Gerente)

## Promociones — `[PROMOCIONES]`
- **Promociones** — lista + editor por tipo, alcance, vigencia, prioridad. (Gerente)
- **Cupones** — generación y control de uso. (Gerente)

## Facturación — `[FACTURACION]`
- **Comprobantes emitidos** — factura, NC, ND, retención, guía, liquidación; estado y filtros. (Contador/Gerente)
- **Detalle de comprobante** — ver, reimprimir RIDE (PDF), descargar XML/XML firmado. (Contador)
- **Anulación de comprobante** — con motivo. (Gerente)
- **Certificados de firma** — cargar/administrar `.p12`. (Admin)
- **Configuración de emisor/ambiente** — Pruebas/Producción, series. (Admin)

## Reportes — `[REPORTES]`
- **Reportería** — selector por grupo: Ventas/POS, Inventario, Compras/Proveedores, Financiero, Impuestos/SRI, Operativo/Caja. (Gerente/Contador)
- **Visor de reporte** — parámetros (fecha, sucursal, categoría, cajero), gráficos + tabla, exportar PDF/XLSX/CSV, guardar configuración. (Gerente/Contador)

## Seguridad y administración
- **Usuarios** — CRUD, asignación de roles y sucursales. (Admin)
- **Roles y permisos** — matriz módulo × acción. (Admin)
- **Auditoría** `[AUDITORIA]` — log de acciones, antes/después, historial de precios/costos. (Admin)
- **Empresa / Sucursales** — datos, RUC, régimen, logo. (Admin)
- **Cajas** — terminales por sucursal. (Admin)
- **Medios de pago** — catálogo y mapeo SRI. (Admin)
- **Módulos y perfiles (feature flags)** — activar/desactivar módulos y aplicar perfiles Supermercado/Minimercado/Tienda. (Admin)
- **Importaciones masivas** — clientes y proveedores desde CSV/XLSX con vista previa, validación y plantillas; catálogo CSV/XLSX desde Productos. (Admin)
- **Migraciones de esquema** — estado de versiones aplicadas/pendientes y aplicación desde la UI. (Admin)
- **Dispositivos** — impresoras, lector de barras, balanza. (Admin)
- **Respaldo/restauración** — pg_dump/pg_restore, programación diaria, historial y restauración con confirmación. (Admin)
- **Migraciones de esquema** — estado de versión de la BD. (Admin)
