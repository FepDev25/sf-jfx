# Registro de Decisiones — sf-jfx

Bitácora de decisiones de diseño (ADR ligero). Cada decisión: opción elegida y notas.
Estados: `DECIDIDO` · `PENDIENTE` · `REEVALUABLE`

---

## Fase 0 — Stack y fundamentos técnicos

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| 0.1 | Build tool: **Maven** multi-módulo | DECIDIDO | |
| 0.2 | Backend: **Java core + JDBC puro** | DECIDIDO | Sin ORM (no JPA/Hibernate). SQL con control total. |
| 0.3 | Base de datos: **PostgreSQL** | DECIDIDO | Driver JDBC directo. |
| 0.4 | Frontend: **JavaFX** | DECIDIDO | UI/UX pulida; dirección de arte a definir en Fase 13. |
| 0.5 | Librerías permitidas: **ZXing**, **iText/PDF** | DECIDIDO | Solo las estrictamente necesarias. |
| 0.6 | Arquitectura: **multi-módulo `core` / `ui`** | DECIDIDO | Dominio/infra separado de presentación. |
| 0.7 | **Java 21 (LTS)** | DECIDIDO | Confirmado en entorno. |
| 0.8 | Excel (XLSX): **Apache POI** | DECIDIDO | Requerido por la exportación de reportes (Hito 9). Suma a las librerías de 0.5. |

---

## Fase 1 — Modelo de negocio y alcance transversal

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| 1.1 | Estructura: **Multi-empresa → Sucursal → Bodega** | DECIDIDO | Entidad `empresa` raíz. Minimercado = 1 empresa + 1 sucursal. |
| 1.2 | Normativa fiscal: **Ecuador — SRI** | DECIDIDO | IVA/ICE/IRBPNR, RUC, clave de acceso 49 dígitos, XML firmado. Detalle en Fase 7. |
| 1.3 | Idioma: **Español en todo** | DECIDIDO | Tablas, columnas, entidades, clases y UI. |
| 1.4 | Operación: **Online-only (MVP)** preparado para offline | DECIDIDO | Se aísla acceso a datos para permitir cache/cola offline luego. |
| 1.5 | Opcionalidad: **Feature flags en BD + perfiles** | DECIDIDO | Perfiles: Supermercado / Minimercado / Tienda. Módulos activables; UI y campos se adaptan. |

---

## Fase 2 — Alcance y módulos

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| 2.1 | Alcance total: **14 módulos** | DECIDIDO | Productos, Inventario, Bodegas, Ventas/POS/Caja, Facturación SRI, Compras/Proveedores, Clientes, Usuarios/Roles/Auditoría, Fidelización, Reportes, Listas de precios, Promociones, Balanza/Granel (PLU), Administración. |
| 2.2 | Estrategia: **proyecto completo, sin prisa** | DECIDIDO | No se sacrifica calidad por velocidad; todos los módulos se abordan. |
| 2.3 | Facturación electrónica: **simulada inicialmente** | DECIDIDO | Flujo completo + XML/PDF, **sin envío real al SRI** en la primera etapa. Enchufable después. |
| 2.4 | Pedidos: **compra (proveedores) + venta a domicilio** | DECIDIDO | Orden de compra/recepción y pedidos de cliente (pickup/entrega). |
| 2.5 | Productos: **simple + variantes opcionales + kits** | DECIDIDO | Simple por defecto; padre-hijo para tallas/presentaciones; **kits con componentes** (al vender descuenta stock de todos los componentes). |

---

## Fase 3 — Catálogo de productos

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| 3.1 | Códigos de barras: **múltiples por producto, tipificados, con código interno y de empaque** | DECIDIDO | Soporta EAN-13/UPC-A/EAN-8/DUN-14. Código interno autogenerado para productos sin fabricante. |
| 3.2 | Categorías: **árbol jerárquico multi-nivel** | DECIDIDO | Niveles ilimitados. |
| 3.3 | Unidades: **unidad base + empaques con factor de conversión** | DECIDIDO | Compra por caja, venta por unidad. |
| 3.4 | Impuestos: **múltiples por producto, con vigencia** | DECIDIDO | Catálogo de impuestos (IVA 15%/0%, ICE, IRBPNR). Histórico por vigencia. |
| 3.5 | Precios: **costo + listas de precio (Público, Mayorista…), con IVA** | DECIDIDO | Costo + pertenencia a listas. Precio con/sin IVA. |
| 3.6 | Fotos: **varias, archivo en disco + ruta en BD** | DECIDIDO | Galería + miniatura; respaldo de archivos aparte. |
| 3.7 | Atributos: **campos personalizados definidos por categoría** | DECIDIDO | Extensible sin recompilar. |
| 3.8 | Marcas: **entidad propia** | DECIDIDO | Catálogo de marcas/fabricantes. |

## Fase 4 — Inventario y bodegas

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| 4.1 | Costeo: **Promedio Ponderado (PPP)** | DECIDIDO | Se actualiza al recibir compras; base para costo de ventas y utilidad. |
| 4.2 | Lotes/vencimientos: **opcional por producto** | DECIDIDO | Flag `controla_lote` por producto. |
| 4.3 | Números de serie: **opcional por producto** | DECIDIDO | Flag `controla_serie` (electrónica, etc.). |
| 4.4 | Stock negativo: **permitido con alerta** | DECIDIDO | No bloquea la venta; genera alerta y se corrige con recepción/ajuste. |
| 4.5 | Transferencias: **con documento y estados** | DECIDIDO | Borrador → En tránsito → Recibida; stock en tránsito trazable. |
| 4.6 | Corrección de existencias: **toma física con ajustes auditados** | DECIDIDO | Conteo → diferencias → ajuste aprobado. **Conteo ciego configurable** por toma (el operador no ve el saldo del sistema). |
| 4.7 | Reservas de stock: **sí** | DECIDIDO | Pedidos/ventas en curso reservan; evita sobreventa. |
| 4.8 | Kardex: **movimientos como fuente de verdad** | DECIDIDO | Todo cambio de stock queda como movimiento tipificado (entrada/salida/ajuste/transferencia/venta/compra). |

## Fase 5 — Compras y proveedores

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| 5.1 | Ciclo de compras: **completo y configurable** | DECIDIDO | Solicitud (opcional) → OC (aprobación) → Recepción → Documento proveedor → CxP → Pago. Módulos internos activables. |
| 5.2 | Recepciones: **parciales y múltiples** | DECIDIDO | Control de cantidad pendiente por línea de OC. |
| 5.3 | Retenciones: **sí (SRI, IVA y Renta)** | DECIDIDO | Comprobante de retención al registrar factura de proveedor. |
| 5.4 | Cuentas por pagar: **saldos, vencimientos y pagos parciales** | DECIDIDO | Pagos con múltiples medios. |
| 5.5 | Producto–proveedor: **multi-proveedor con histórico de costos** | DECIDIDO | Proveedor preferido + historial de precios de compra. |
| 5.6 | Devoluciones a proveedor: **sí** | DECIDIDO | Sale stock, ajusta CxP. |
| 5.7 | Proveedores: entidad con datos fiscales (RUC, tipo contribuyente, retenciones) | DECIDIDO | Requerido para retenciones y CxP. |

## Fase 6 — Ventas, caja y POS

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| 6.1 | Medios de pago: **efectivo, tarjeta, transferencia, crédito, vales/cupones/puntos** | DECIDIDO | Con soporte de **pago mixto**. |
| 6.2 | Caja: **turnos con apertura, arqueo por denominación y cierre** | DECIDIDO | Diferencias y trazabilidad por cajero. Múltiples cajas/terminales por sucursal. |
| 6.3 | Descuentos: **por línea y global** | DECIDIDO | Autorización por permiso/PIN para descuentos manuales (ver Fase 9). |
| 6.4 | Devoluciones: **parcial/total con Nota de Crédito** | DECIDIDO | Reintegro por medio original o efectivo. |
| 6.5 | POS avanzado: **búsqueda por nombre, atajos de teclado, aparcar ventas, ventas en paralelo, venta rápida 1-ítem** | DECIDIDO | UX de cajero prioritaria (velocidad + teclado). |
| 6.6 | Pedidos de venta: **flujo de estados** | DECIDIDO | Recibido → En preparación → Listo → En reparto → Entregado; se convierte en venta/factura. |
| 6.7 | Propina: **configurable (ej. 10% servicio)** | DECIDIDO | Aplicable al POS; se refleja en el comprobante. |

## Fase 7 — Facturación Ecuador / SRI

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| 7.1 | Comprobantes: **Factura (01), Nota de Crédito (04), Nota de Débito (05), Retención (07), Guía de Remisión (06), Liquidación de Compra (03)** | DECIDIDO | Todos tipificados con su estructura. |
| 7.2 | Generación: **XML del SRI completo + firma XAdES-BES (.p12)** | DECIDIDO | Listo para envío real; en esta etapa **no se envía**. |
| 7.3 | Representación impresa: **RIDE fiel al formato SRI (PDF con iText)** | DECIDIDO | Incluye clave de acceso y código de barras. |
| 7.4 | Numeración: **Establecimiento(3)+PuntoEmisión(3)+Secuencial(9)** | DECIDIDO | Por tipo de comprobante y por sucursal, con control de consecutivos. |
| 7.5 | Emisor: **ficha completa + RIMPE + ambiente/tipo de emisión + clave de acceso 49 dígitos** | DECIDIDO | Requerido por el alcance XML+firma. Ambiente Pruebas/Producción. |
| 7.6 | Comprador: **consumidor final por defecto** | DECIDIDO | Identificación opcional (cédula/RUC/pasaporte) + razón social. |
| 7.7 | Estados del comprobante: **borrador → emitido → (anulado)** | DECIDIDO | Con espacio para estado de autorización SRI futuro. |

## Fase 8 — Clientes y fidelización

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| 8.1 | Clientes: **consumidor final, persona natural, empresa** | DECIDIDO | Identificación: cédula/RUC/pasaporte. |
| 8.2 | Crédito: **límite, plazo, abonos parciales, bloqueo por exceso** | DECIDIDO | Cuentas por cobrar con estado de cuenta. |
| 8.3 | Fidelización: **solo puntos por compra (regla configurable) + canje como medio de pago** | DECIDIDO | Sin niveles de cliente. |
| 8.4 | Direcciones: **múltiples por cliente** | DECIDIDO | Para pedidos a domicilio. |
| 8.5 | Ficha cliente: **historial de compras, frecuencia, ticket promedio, favoritos** | DECIDIDO | |

## Fase 9 — Usuarios, roles y auditoría

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| 9.1 | Roles: **base (Admin, Gerente, Supervisor, Cajero, Bodeguero, Contador) + personalizados** | DECIDIDO | Editables y ampliables. |
| 9.2 | Permisos: **por módulo + acción (ver/crear/editar/eliminar/aprobar/exportar)** | DECIDIDO | Los roles agrupan permisos. Ej: cajero no anula ventas. |
| 9.3 | Autenticación: **contraseña hasheada (BCrypt/Argon2) + PIN de cajero + bloqueo por inactividad + sesión única por caja** | DECIDIDO | PIN para autorizaciones rápidas (descuentos, anulaciones). |
| 9.4 | Auditoría: **acciones sensibles + antes/después + historial de precios/costos** | DECIDIDO | Almacena usuario, fecha, terminal. |
| 9.5 | Alcance: **acceso por sucursal** | DECIDIDO | Usuario limitado a una o varias sucursales. |

## Fase 10 — Reportes y analítica

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| 10.1 | Grupos: **Ventas/POS, Inventario, Compras/Proveedores, Financiero, Impuestos/SRI, Operativo/Caja** | DECIDIDO | Cobertura completa del negocio. |
| 10.2 | Dashboard: **KPIs en vivo + gráficos** | DECIDIDO | Pantalla de inicio. |
| 10.3 | Exportación: **PDF, Excel (XLSX), CSV** | DECIDIDO | |
| 10.4 | Reportes: **parametrizables y guardables** | DECIDIDO | Filtros por fecha/sucursal/categoría/cajero. |
| 10.5 | Gráficos: **JavaFX nativos** | DECIDIDO | Barras, líneas, pastel. |

## Fase 11 — Listas de precios y promociones

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| 11.1 | Listas de precio: **precio por producto+lista con vigencia; lista por tipo de cliente; override por sucursal; ajuste masivo por categoría** | DECIDIDO | Precio base global por lista, con override puntual por sucursal. |
| 11.2 | Promociones: **% , monto fijo, NxM/2da unidad, precio fijo, combos/packs** | DECIDIDO | |
| 11.3 | Delimitación: **fechas, sucursal, categoría/marca/producto, tipo de cliente** | DECIDIDO | |
| 11.4 | Combinación: **prioridad configurable, sin acumular** | DECIDIDO | Se aplica la mejor oferta o la de mayor prioridad. |
| 11.5 | Cupones: **sí, con código y uso controlado** | DECIDIDO | |

## Fase 12 — Administración, configuración y respaldo

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| 12.1 | Gestión en UI: **empresa/sucursales/bodegas, feature flags y perfiles, parámetros del negocio** | DECIDIDO | Todo configurable sin tocar BD. |
| 12.2 | Respaldo: **integrado (pg_dump/pg_restore) con programación y registro** | DECIDIDO | |
| 12.3 | Esquema: **migraciones versionadas automáticas al iniciar** | DECIDIDO | Control de versión de esquema en BD. |
| 12.4 | Carga masiva: **importar y exportar catálogo, clientes y proveedores (CSV/Excel)** | DECIDIDO | Con vista previa y validación. Catálogo y terceros aceptan CSV/XLSX. |
| 12.5 | Dispositivos: **impresora térmica 80mm, A4, lector de barras (HID), balanza y gaveta de efectivo** | DECIDIDO | Todo soportado en software; sin hardware inicial para lector/balanza/A4/gaveta. |

## Fase 13 — Vistas de UI y dirección de arte

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| 13.1 | Navegación: **ventana única + sidebar + Modo Caja dedicado** | DECIDIDO | Modo Caja a pantalla completa. |
| 13.2 | Arte: **moderno claro (SaaS)** | DECIDIDO | Tarjetas, acentos, sombras suaves, buen espaciado. |
| 13.3 | Tema: **único (claro)** | DECIDIDO | Oscuro no se implementa. |
| 13.4 | Caja: **POS fullscreen de alta velocidad** | DECIDIDO | Foco en teclado, totales grandes, atajos F-keys. |
| 13.5 | Tipografía/iconos: **Inter/Roboto + Ikonli** | DECIDIDO | |
| 13.6 | Proceso: **mockups antes de código** | DECIDIDO | Validar look & feel primero. |

---

## Estado del brainstorming

**Alcance congelado: sin decisiones pendientes.** Todas las fases (0–13) cerradas.
Los ítems antes marcados `REEVALUABLE` quedaron resueltos (ver Fase 2.5, 4.6, 6.7, 8.3, 9.3, 11.1, 12.4, 12.5, 13.3).
Cambios futuros se registran aquí como nuevas filas o una nueva fase.
