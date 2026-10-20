# Modelo de Datos — sf-jfx

Catálogo de entidades y atributos del core. Nombres de tablas y columnas en **español**, `snake_case`.
Motor: **PostgreSQL**. Acceso: **JDBC**.

**Convenciones generales**
- PK: `id BIGSERIAL`.
- FK: `<entidad>_id`, con `ON DELETE` explícito por relación.
- Columnas de auditoría comunes (salvo excepciones): `activo BOOLEAN DEFAULT TRUE`, `creado_en TIMESTAMP`, `actualizado_en TIMESTAMP`, `creado_por_id`, `actualizado_por_id`.
- Dinero: `NUMERIC(14,4)` (soporta costos con más precisión). Cantidades: `NUMERIC(14,4)`.
- Estados: `VARCHAR` con `CHECK` o catálogo. Enumeraciones simples → `VARCHAR` + validación.

---

## 1. Núcleo / Organización

### `instalacion`
`id`, `nombre`, `modo` (`UNICA_EMPRESA` | `MULTIEMPRESA`), `version_esquema`, `creado_en`,
`actualizado_en`. Metadatos de la instalación. Una base PostgreSQL por instalación; esta tabla
**no** convierte el modelo en multi-tenant compartido. La fila la crea el asistente de primer
arranque (`V8`).

### `empresa`
`id`, `ruc`, `razon_social`, `nombre_comercial`, `direccion_matriz`, `telefono`, `email`, `logo_path`, `contribuyente_especial` (bool), `obligado_llevar_contabilidad` (bool), `regimen` (GENERAL | RIMPE_EMPRENDEDOR | RIMPE_NEGOCIO_POPULAR), `moneda` (default `USD`), `activo`, auditoría.

### `sucursal`
`id`, `empresa_id`, `codigo`, `nombre`, `direccion`, `telefono`, `es_matriz` (bool), `activo`, auditoría.

### `bodega`
`id`, `sucursal_id`, `codigo`, `nombre`, `tipo` (VENTA | ALMACEN | TRANSITO | MERMA | CONSIGNA | PRODUCCION), `direccion`, `activo`, auditoría.

### `caja` (terminal POS)
`id`, `sucursal_id`, `codigo`, `nombre`, `estado` (DISPONIBLE | OCUPADA | MANTENIMIENTO), `activo`.

### `usuario`
`id`, `empresa_id` (nullable = global), `nombre_usuario` (único), `nombres`, `apellidos`, `email`, `telefono`, `password_hash`, `pin_hash` (autorizaciones rápidas de cajero), `es_superadmin` (bool), `activo`, `ultimo_acceso`, `intentos_fallidos`, `bloqueado`, auditoría.

### `rol`
`id`, `empresa_id` (nullable = del sistema), `nombre`, `descripcion`, `es_sistema` (bool), `activo`.

### `permiso`
`id`, `codigo` (ej. `productos.crear`), `modulo`, `accion` (VER | CREAR | EDITAR | ELIMINAR | APROBAR | EXPORTAR | ANULAR), `descripcion`.

### `rol_permiso`
`rol_id`, `permiso_id` (PK compuesta).

### `usuario_rol`
`usuario_id`, `rol_id` (PK compuesta).

### `usuario_sucursal`
`usuario_id`, `sucursal_id` (PK compuesta).

### `auditoria`
`id`, `empresa_id`, `sucursal_id`, `usuario_id`, `terminal`, `modulo`, `entidad`, `entidad_id`, `accion` (CREAR | EDITAR | ELIMINAR | ANULAR | AUTORIZAR), `valores_anteriores` (JSONB), `valores_nuevos` (JSONB), `ip`, `creado_en`.

### `configuracion` (parámetros clave/valor)
`id`, `empresa_id`, `clave` (único por empresa), `valor` (TEXT), `tipo` (TEXTO | NUMERO | BOOLEAN | JSON | FECHA), `grupo`, `descripcion`.

### `empresa_modulo` (feature flags)
`id`, `empresa_id`, `modulo_codigo` (PRODUCTOS | INVENTARIO | BODEGAS | POS | FACTURACION | COMPRAS | PROVEEDORES | CLIENTES | FIDELIZACION | PROMOCIONES | LISTAS_PRECIO | GRANEL | REPORTES | USUARIOS | AUDITORIA | CONFIGURACION), `activo`. Perfil de instalación en `configuracion` (`empresa.perfil`).

### `secuencia_documento` (consecutivos SRI)
`id`, `empresa_id`, `sucursal_id`, `tipo_comprobante`, `establecimiento` (3), `punto_emision` (3), `secuencial_actual` (9), `activo`. Único por (empresa, sucursal, tipo, establecimiento, punto_emision).

### `dispositivo`
`id`, `empresa_id`, `sucursal_id`, `caja_id` (nullable), `tipo` (IMPRESORA_TICKET | IMPRESORA_A4 | LECTOR_BARRAS | BALANZA | GAVETA), `nombre`, `configuracion` (JSONB), `activo`.

### `respaldo`
`id`, `empresa_id`, `ruta_archivo`, `tamano_bytes`, `tipo` (MANUAL | PROGRAMADO), `estado` (OK | ERROR), `usuario_id`, `creado_en`, `detalle` (resultado o error de la ejecución, `V6`).

### `reporte_guardado`
`id`, `empresa_id`, `usuario_id`, `codigo` (reporte), `nombre`, `desde`, `hasta`, `sucursal_id`, `categoria_id`, `filtro_usuario_id`, `creado_en` (`V5`). Configuraciones reutilizables del visor de reportes.

### `migracion_esquema`
`version`, `descripcion`, `aplicada_en`.

---

## 2. Catálogo

### `categoria`
`id`, `empresa_id`, `padre_id` (self, nullable), `codigo`, `nombre`, `descripcion`, `orden`, `activo`.

### `marca`
`id`, `empresa_id`, `nombre`, `descripcion`, `logo_path`, `activo`.

### `unidad_medida`
`id`, `codigo`, `nombre`, `abreviatura`, `tipo` (UNIDAD | PESO | VOLUMEN | LONGITUD), `permite_decimales` (bool), `activo`.

### `impuesto`
`id`, `empresa_id`, `codigo_sri` (ej. 2=IVA, 3=ICE, 5=IRBPNR), `nombre`, `tipo` (IVA | ICE | IRBPNR | OTRO), `porcentaje` (NUMERIC 5,2), `vigente_desde`, `vigente_hasta` (nullable), `es_retencion` (bool), `activo`.

### `producto`
`id`, `empresa_id`, `codigo` (SKU, único por empresa), `nombre`, `descripcion`, `categoria_id`, `marca_id`, `unidad_medida_id`, `tipo` (SIMPLE | VARIANTE_PADRE | VARIANTE | KIT | SERVICIO | GRANEL), `producto_padre_id` (nullable), `costo` (último), `costo_promedio` (PPP), `controla_lote` (bool), `controla_serie` (bool), `es_granel` (bool), `codigo_plu` (nullable), `peso_neto`, `volumen`, `ubicacion` (texto libre), `imagen_principal_id` (nullable), `activo`, auditoría.

### `producto_codigo_barra`
`id`, `producto_id`, `codigo` (único global), `tipo` (EAN13 | UPC_A | EAN8 | DUN14 | INTERNO | PLU), `es_principal` (bool), `empaque_id` (nullable).

### `producto_empaque`
`id`, `producto_id`, `nombre` (CAJA | DOCENA | PACK), `unidad_medida_id`, `factor` (NUMERIC, unidades base que contiene), `es_compra` (bool), `es_venta` (bool), `codigo_barra_id` (nullable), `activo`.

### `producto_impuesto`
`producto_id`, `impuesto_id` (PK compuesta). Producto puede tener varios (IVA + ICE + IRBPNR).

### `producto_imagen`
`id`, `producto_id`, `ruta`, `nombre_archivo`, `orden`, `es_principal` (bool).

### `producto_componente` (kits / armados)
`id`, `producto_kit_id`, `producto_componente_id`, `cantidad`. Al vender un KIT se descuenta stock de cada componente.

### `lista_precio`
`id`, `empresa_id`, `nombre`, `tipo` (PUBLICO | MAYORISTA | CORPORATIVO | COSTO), `moneda`, `incluye_iva` (bool), `es_default` (bool), `activo`.

### `precio_producto`
`id`, `lista_precio_id`, `producto_id`, `precio` (NUMERIC), `sucursal_id` (nullable, override), `vigente_desde`, `vigente_hasta` (nullable). Único por (lista, producto, sucursal, vigencia).

### `historial_precio` (`V3`)
`id`, `empresa_id`, `producto_id`, `lista_precio_id` (nullable), `tipo` (PRECIO | COSTO), `valor_anterior`, `valor_nuevo`, `sucursal_id`, `usuario_id`, `creado_en`. Trazabilidad de cambios de precios y costos.

### `atributo_definicion` (campo personalizado)
`id`, `empresa_id`, `categoria_id` (nullable), `codigo`, `nombre`, `tipo_dato` (TEXTO | NUMERO | DECIMAL | BOOLEAN | FECHA | LISTA), `opciones` (JSONB), `obligatorio` (bool), `orden`, `activo`.

### `producto_atributo`
`id`, `producto_id`, `atributo_definicion_id`, `valor` (TEXT).

---

## 3. Inventario y Bodegas

### `lote`
`id`, `producto_id`, `codigo`, `fecha_fabricacion` (nullable), `fecha_vencimiento` (nullable), `activo`.

### `serie`
`id`, `producto_id`, `numero_serie` (único), `estado` (DISPONIBLE | VENDIDO | BAJA | RESERVADO), `bodega_id`, `lote_id` (nullable), `venta_id` (nullable).

### `stock`
`id`, `bodega_id`, `producto_id`, `lote_id` (nullable), `cantidad` (NUMERIC), `cantidad_reservada` (NUMERIC), `actualizado_en`. Único por (bodega, producto, lote).

### `tipo_movimiento_inventario`
`id`, `codigo`, `nombre`, `signo` (ENTRADA | SALIDA | AJUSTE).

### `movimiento_inventario` (Kardex — fuente de verdad)
`id`, `empresa_id`, `sucursal_id`, `bodega_id`, `producto_id`, `lote_id` (nullable), `serie_id` (nullable), `tipo_movimiento_id`, `cantidad` (con signo), `costo_unitario`, `saldo_cantidad`, `saldo_costo`, `referencia_tipo` (VENTA | COMPRA | AJUSTE | TRANSFERENCIA | TOMA | DEVOLUCION_VENTA | DEVOLUCION_COMPRA | INICIAL), `referencia_id`, `usuario_id`, `nota`, `creado_en`.

### `transferencia`
`id`, `empresa_id`, `numero`, `sucursal_origen_id`, `bodega_origen_id`, `sucursal_destino_id`, `bodega_destino_id`, `estado` (BORRADOR | EN_TRANSITO | RECIBIDA | ANULADA), `fecha_envio`, `fecha_recepcion`, `usuario_envia_id`, `usuario_recibe_id`, `nota`.

### `transferencia_detalle`
`id`, `transferencia_id`, `producto_id`, `lote_id` (nullable), `cantidad_enviada`, `cantidad_recibida`.

### `toma_inventario`
`id`, `empresa_id`, `bodega_id`, `numero`, `estado` (ABIERTA | EN_CONTEO | CERRADA | APLICADA | ANULADA), `conteo_ciego` (bool), `fecha_inicio`, `fecha_cierre`, `usuario_id`, `observacion`.

### `toma_inventario_detalle`
`id`, `toma_id`, `producto_id`, `lote_id` (nullable), `cantidad_sistema`, `cantidad_contada`, `diferencia`, `costo_unitario`.

### `ajuste_inventario`
`id`, `empresa_id`, `bodega_id`, `numero`, `tipo` (ENTRADA | SALIDA), `motivo` (MERMA | DAÑO | ROBO | VENCIMIENTO | CORRECCION | INICIAL | OTRO), `estado` (BORRADOR | APLICADO | ANULADO), `usuario_id`, `fecha`, `nota`.

### `ajuste_inventario_detalle`
`id`, `ajuste_id`, `producto_id`, `lote_id` (nullable), `cantidad`, `costo_unitario`.

---

## 4. Compras y Proveedores

### `proveedor`
`id`, `empresa_id`, `tipo_identificacion` (RUC | CEDULA | PASAPORTE), `numero_identificacion`, `razon_social`, `nombre_comercial`, `direccion`, `telefono`, `email`, `tipo_contribuyente`, `dias_credito`, `aplica_retencion_iva` (bool), `aplica_retencion_renta` (bool), `activo`, `nota`.

### `proveedor_contacto`
`id`, `proveedor_id`, `nombre`, `cargo`, `telefono`, `email`.

### `producto_proveedor`
`id`, `producto_id`, `proveedor_id`, `codigo_proveedor`, `costo_ultimo`, `plazo_dias`, `es_preferido` (bool). Único por (producto, proveedor).

### `solicitud_compra`
`id`, `empresa_id`, `sucursal_id`, `numero`, `estado` (BORRADOR | APROBADA | RECHAZADA | CONVERTIDA), `usuario_solicita_id`, `usuario_aprueba_id`, `fecha`, `nota`.

### `solicitud_compra_detalle`
`id`, `solicitud_id`, `producto_id`, `cantidad`, `justificacion`.

### `orden_compra`
`id`, `empresa_id`, `sucursal_id`, `proveedor_id`, `numero`, `estado` (BORRADOR | APROBADA | RECEPCION_PARCIAL | RECIBIDA | ANULADA), `fecha_emision`, `fecha_entrega_esperada`, `condiciones_pago`, `subtotal`, `descuento`, `impuestos`, `total`, `usuario_id`, `nota`.

### `orden_compra_detalle`
`id`, `orden_compra_id`, `producto_id`, `descripcion`, `cantidad`, `cantidad_recibida`, `precio_unitario`, `impuesto_id` (nullable), `descuento`, `subtotal`, `total`.

### `recepcion_compra`
`id`, `empresa_id`, `orden_compra_id` (nullable), `numero`, `bodega_id`, `fecha`, `estado` (BORRADOR | CONFIRMADA | ANULADA), `usuario_id`, `nota`.

### `recepcion_compra_detalle`
`id`, `recepcion_id`, `orden_compra_detalle_id` (nullable), `producto_id`, `lote_id` (nullable), `lote_codigo`, `fecha_vencimiento`, `cantidad_recibida`, `costo_unitario`.

### `factura_proveedor`
`id`, `empresa_id`, `proveedor_id`, `orden_compra_id` (nullable), `recepcion_id` (nullable), `tipo_documento` (FACTURA | NOTA_CREDITO | LIQUIDACION), `numero`, `fecha_emision`, `fecha_vencimiento`, `subtotal`, `descuento`, `iva`, `total`, `saldo`, `estado_pago` (PENDIENTE | PARCIAL | PAGADA | ANULADA), `usuario_id`.

### `factura_proveedor_detalle`
`id`, `factura_proveedor_id`, `producto_id` (nullable), `descripcion`, `cantidad`, `precio_unitario`, `impuesto_id`, `total`.

### `pago_proveedor`
`id`, `empresa_id`, `proveedor_id`, `numero`, `fecha`, `total`, `medio_pago_id`, `referencia`, `usuario_id`, `nota`.

### `pago_proveedor_factura`
`id`, `pago_id`, `factura_proveedor_id`, `monto`.

### `retencion` (comprobante de retención 07)
`id`, `empresa_id`, `proveedor_id`, `factura_proveedor_id`, `comprobante_id` (referencia al documento SRI), `establecimiento`, `punto_emision`, `secuencial`, `fecha_emision`, `clave_acceso`, `base_iva`, `valor_iva`, `valor_retencion_iva`, `base_renta`, `valor_retencion_renta`, `total_retenido`, `estado`.

### `retencion_detalle`
`id`, `retencion_id`, `tipo_impuesto` (IVA | RENTA), `codigo_retencion`, `base_imponible`, `porcentaje`, `valor_retenido`.

### `devolucion_compra`
`id`, `empresa_id`, `proveedor_id`, `factura_proveedor_id` (nullable), `recepcion_id` (nullable), `bodega_id`, `numero`, `fecha`, `motivo` (DAÑO | VENCIMIENTO | ERROR | OTRO), `total`, `estado`.

### `devolucion_compra_detalle`
`id`, `devolucion_id`, `producto_id`, `lote_id` (nullable), `cantidad`, `costo_unitario`, `total`.

---

## 5. Clientes y Fidelización

### `cliente`
`id`, `empresa_id`, `tipo` (CONSUMIDOR_FINAL | PERSONA_NATURAL | EMPRESA), `tipo_identificacion` (CEDULA | RUC | PASAPORTE | CONSUMIDOR_FINAL), `numero_identificacion`, `nombres`, `apellidos`, `razon_social`, `email`, `telefono`, `tipo_contribuyente`, `direccion`, `es_credito` (bool), `limite_credito`, `dias_credito`, `saldo` (calculado/materializado), `activo`, auditoría.

### `cliente_direccion`
`id`, `cliente_id`, `alias`, `direccion`, `referencia`, `lat`, `lng`, `es_principal` (bool).

### `cliente_contacto`
`id`, `cliente_id`, `nombre`, `telefono`, `email`, `cargo`.

### `programa_fidelidad`
`id`, `empresa_id`, `nombre`, `activo`, `puntos_por_monto` (NUMERIC), `valor_punto` (equiv. canje), `vigencia_puntos_dias`.

### `cliente_puntos`
`id`, `cliente_id`, `saldo_puntos`. (una fila por cliente)

### `puntos_movimiento`
`id`, `cliente_id`, `tipo` (ACUMULA | CANJE | AJUSTE | VENCIMIENTO), `puntos`, `referencia_tipo` (VENTA | CANJE | AJUSTE), `referencia_id`, `saldo_resultante`, `fecha`, `nota`.

### `cupon`
`id`, `empresa_id`, `codigo` (único), `tipo` (PORCENTAJE | MONTO), `valor`, `vigente_desde`, `vigente_hasta`, `usos_maximos`, `usos_actuales`, `activo`.

### `cxc_movimiento` (`V4`)
`id`, `empresa_id`, `cliente_id`, `venta_id` (nullable), `tipo` (CARGO | ABONO | AJUSTE), `monto`, `saldo_resultante`, `medio_pago_id`, `usuario_id`, `nota`, `creado_en`. Estado de cuenta del cliente; el saldo vigente se mantiene en `cliente.saldo`.

---

## 6. Ventas y POS

### `cliente_direccion` — ver §5.

### `sesion_caja` (turno)
`id`, `empresa_id`, `caja_id`, `usuario_apertura_id`, `usuario_cierre_id`, `fecha_apertura`, `fecha_cierre`, `monto_inicial`, `monto_esperado_efectivo`, `monto_contado_efectivo`, `diferencia`, `estado` (ABIERTA | CERRADA), `observacion`.

### `sesion_caja_arqueo` (conteo por denominación)
`id`, `sesion_caja_id`, `denominacion` (NUMERIC), `cantidad`, `subtotal`.

### `medio_pago`
`id`, `empresa_id`, `codigo`, `nombre`, `tipo` (EFECTIVO | TARJETA_DEBITO | TARJETA_CREDITO | TRANSFERENCIA | CREDITO | VALE | PUNTOS), `requiere_referencia` (bool), `afecta_caja` (bool), `codigo_sri_forma_pago` (nullable), `activo`.

### `venta`
`id`, `empresa_id`, `sucursal_id`, `caja_id`, `sesion_caja_id`, `numero`, `cliente_id`, `usuario_id` (cajero), `tipo` (VENTA | PEDIDO), `estado` (ABIERTA | APARCADA | PAGADA | ANULADA), `pedido_id` (nullable), `fecha`, `subtotal`, `descuento_total`, `impuesto_total`, `propina`, `total`, `costo_total`, `utilidad`, `autorizado_por_id` (descuentos), `nota`.

### `venta_detalle`
`id`, `venta_id`, `producto_id`, `descripcion`, `cantidad`, `peso` (nullable, granel), `precio_unitario`, `descuento`, `impuesto_id`, `impuesto_valor`, `subtotal`, `total`, `costo_unitario`, `lote_id` (nullable), `serie_id` (nullable), `promocion_id` (nullable).

### `venta_pago`
`id`, `venta_id`, `medio_pago_id`, `monto`, `referencia`, `cambio` (vuelto), `usuario_id`.

### `pedido`
`id`, `empresa_id`, `sucursal_id`, `cliente_id`, `numero`, `tipo_entrega` (PICKUP | DELIVERY), `estado` (RECIBIDO | EN_PREPARACION | LISTO | EN_REPARTO | ENTREGADO | CANCELADO), `direccion_entrega_id` (nullable), `fecha`, `fecha_entrega_estimada`, `subtotal`, `descuento`, `total`, `venta_id` (nullable), `repartidor_usuario_id` (nullable), `nota`.

### `pedido_detalle`
`id`, `pedido_id`, `producto_id`, `cantidad`, `precio_unitario`, `total`.

### `devolucion_venta`
`id`, `empresa_id`, `venta_id`, `cliente_id`, `numero`, `fecha`, `motivo`, `total`, `bodega_id`, `usuario_id`, `estado`, `comprobante_id` (nota de crédito generada).

### `devolucion_venta_detalle`
`id`, `devolucion_id`, `producto_id`, `cantidad`, `precio_unitario`, `total`, `reingresa_stock` (bool).

---

## 7. Promociones

### `promocion`
`id`, `empresa_id`, `nombre`, `descripcion`, `tipo` (PORCENTAJE | MONTO | NXM | PRECIO_FIJO | COMBO), `valor`, `cantidad_requerida` (NxM), `cantidad_pagada` (NxM), `prioridad` (int), `combinable` (bool, default false), `vigente_desde`, `vigente_hasta`, `estado` (BORRADOR | ACTIVA | INACTIVA), `activo`.

### `promocion_alcance`
`id`, `promocion_id`, `tipo_alcance` (PRODUCTO | CATEGORIA | MARCA | SUCURSAL | TIPO_CLIENTE), `referencia_id`.

### `promocion_detalle` (componentes de combo)
`id`, `promocion_id`, `producto_id`, `cantidad`.

---

## 8. Facturación / SRI

### `certificado_firma`
`id`, `empresa_id`, `alias`, `ruta_p12`, `password_cifrada`, `fecha_desde`, `fecha_hasta`, `activo`.

### `comprobante` (documento electrónico genérico)
`id`, `empresa_id`, `sucursal_id`, `tipo` (FACTURA | NOTA_CREDITO | NOTA_DEBITO | RETENCION | GUIA_REMISION | LIQUIDACION_COMPRA), `establecimiento`, `punto_emision`, `secuencial`, `numero_completo`, `clave_acceso` (49), `fecha_emision`, `ambiente` (PRUEBAS | PRODUCCION), `tipo_emision` (NORMAL | CONTINGENCIA), `emisor_ruc`, `emisor_razon_social`, `emisor_direccion`, `receptor_tipo_id`, `receptor_numero_id`, `receptor_razon_social`, `receptor_email`, `subtotal`, `descuento`, `iva`, `ice`, `irbpnr`, `propina`, `total`, `estado` (BORRADOR | EMITIDO | PENDIENTE_AUTORIZACION | AUTORIZADO | NO_AUTORIZADO | ANULADO), `venta_id` (nullable), `factura_proveedor_id` (nullable), `motivo`, `xml_path`, `xml_firmado_path`, `pdf_path`, `creado_en`, `actualizado_en`.

### `comprobante_detalle`
`id`, `comprobante_id`, `codigo_principal`, `codigo_auxiliar`, `descripcion`, `cantidad`, `precio_unitario`, `descuento`, `subtotal`, `impuesto_total`, `total`.

### `comprobante_impuesto`
`id`, `comprobante_id`, `comprobante_detalle_id` (nullable = a nivel documento), `codigo_impuesto` (2 IVA, 3 ICE, 5 IRBPNR), `codigo_porcentaje`, `tarifa`, `base_imponible`, `valor`.

### `comprobante_pago` (formas de pago SRI)
`id`, `comprobante_id`, `forma_pago_codigo` (01, 19, 20, 21…), `total`, `plazo`, `unidad_tiempo`.

### `comprobante_relacionado`
`id`, `comprobante_id`, `tipo_relacionado`, `comprobante_relacionado_id`.

### `guia_remision_datos`
`id`, `comprobante_id`, `transportista_ruc`, `transportista_razon_social`, `placa`, `punto_partida`, `punto_llegada`, `fecha_inicio_transporte`, `fecha_fin_transporte`.

### `guia_remision_detalle`
`id`, `comprobante_id`, `producto_id`, `cantidad`, `descripcion`.

---

## 9. Migraciones y rendimiento

| Versión | Contenido |
|---------|-----------|
| `V1` | Esquema inicial completo. |
| `V2` | Datos base: permisos, roles, unidades, impuestos, medios de pago y tipos de movimiento. |
| `V3` | `historial_precio`. |
| `V4` | `cxc_movimiento`. |
| `V5` | `reporte_guardado`. |
| `V6` | `respaldo.detalle`. |
| `V7` | Índices de rendimiento para ventas, inventario, CxC/CxP, comprobantes, auditoría y paginación. |
| `V8` | `instalacion` (metadatos del primer arranque). |

## 10. Relación rápida de cardinalidades clave
- Empresa 1—N Sucursal 1—N Bodega; Sucursal 1—N Caja.
- Producto N—N Impuesto (`producto_impuesto`); Producto N—N Lista (`precio_producto`); Producto N—N Proveedor (`producto_proveedor`).
- Stock por (Bodega, Producto, Lote). Movimiento inventario es el kardex.
- Venta 1—N Detalle; Venta N—N MedioPago (vía `venta_pago`).
- Comprobante 1—N Detalle / Impuesto / Pago; comprobante puede nacer de una Venta o de una FacturaProveedor.
