-- =============================================================================
-- V1 — Esquema inicial de sf-jfx
-- Sistema de facturacion para supermercados. PostgreSQL.
-- Nomenclatura en espanol, snake_case.
-- La tabla de control (migracion_esquema) la crea el migrador.
-- =============================================================================

-- =============================================================================
-- 1. NUCLEO / ORGANIZACION
-- =============================================================================

CREATE TABLE empresa (
    id                            BIGSERIAL PRIMARY KEY,
    ruc                           VARCHAR(13)  NOT NULL UNIQUE,
    razon_social                  VARCHAR(255) NOT NULL,
    nombre_comercial              VARCHAR(255),
    direccion_matriz              VARCHAR(255),
    telefono                      VARCHAR(30),
    email                         VARCHAR(120),
    logo_path                     VARCHAR(255),
    contribuyente_especial        BOOLEAN NOT NULL DEFAULT FALSE,
    obligado_llevar_contabilidad  BOOLEAN NOT NULL DEFAULT FALSE,
    regimen                       VARCHAR(30) NOT NULL DEFAULT 'GENERAL'
                                  CHECK (regimen IN ('GENERAL','RIMPE_EMPRENDEDOR','RIMPE_NEGOCIO_POPULAR')),
    moneda                        VARCHAR(3) NOT NULL DEFAULT 'USD',
    activo                        BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en                     TIMESTAMP NOT NULL DEFAULT now(),
    actualizado_en                TIMESTAMP
);

CREATE TABLE sucursal (
    id             BIGSERIAL PRIMARY KEY,
    empresa_id     BIGINT NOT NULL REFERENCES empresa(id),
    codigo         VARCHAR(20) NOT NULL,
    nombre         VARCHAR(120) NOT NULL,
    direccion      VARCHAR(255),
    telefono       VARCHAR(30),
    es_matriz      BOOLEAN NOT NULL DEFAULT FALSE,
    activo         BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMP NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMP,
    UNIQUE (empresa_id, codigo)
);

CREATE TABLE bodega (
    id             BIGSERIAL PRIMARY KEY,
    sucursal_id    BIGINT NOT NULL REFERENCES sucursal(id),
    codigo         VARCHAR(20) NOT NULL,
    nombre         VARCHAR(120) NOT NULL,
    tipo           VARCHAR(20) NOT NULL DEFAULT 'ALMACEN'
                   CHECK (tipo IN ('VENTA','ALMACEN','TRANSITO','MERMA','CONSIGNA','PRODUCCION')),
    direccion      VARCHAR(255),
    activo         BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMP NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMP,
    UNIQUE (sucursal_id, codigo)
);

CREATE TABLE caja (
    id          BIGSERIAL PRIMARY KEY,
    sucursal_id BIGINT NOT NULL REFERENCES sucursal(id),
    codigo      VARCHAR(20) NOT NULL,
    nombre      VARCHAR(120) NOT NULL,
    estado      VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE'
                CHECK (estado IN ('DISPONIBLE','OCUPADA','MANTENIMIENTO')),
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (sucursal_id, codigo)
);

CREATE TABLE usuario (
    id               BIGSERIAL PRIMARY KEY,
    empresa_id       BIGINT REFERENCES empresa(id),
    nombre_usuario   VARCHAR(60) NOT NULL UNIQUE,
    nombres          VARCHAR(120) NOT NULL,
    apellidos        VARCHAR(120),
    email            VARCHAR(120),
    telefono         VARCHAR(30),
    password_hash    VARCHAR(255) NOT NULL,
    pin_hash         VARCHAR(255),
    es_superadmin    BOOLEAN NOT NULL DEFAULT FALSE,
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_acceso    TIMESTAMP,
    intentos_fallidos INT NOT NULL DEFAULT 0,
    bloqueado        BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en        TIMESTAMP NOT NULL DEFAULT now(),
    actualizado_en   TIMESTAMP
);

CREATE TABLE rol (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT REFERENCES empresa(id),
    nombre      VARCHAR(80) NOT NULL,
    descripcion VARCHAR(255),
    es_sistema  BOOLEAN NOT NULL DEFAULT FALSE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (empresa_id, nombre)
);

CREATE TABLE permiso (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(80) NOT NULL UNIQUE,
    modulo      VARCHAR(40) NOT NULL,
    accion      VARCHAR(20) NOT NULL
                CHECK (accion IN ('VER','CREAR','EDITAR','ELIMINAR','APROBAR','EXPORTAR','ANULAR')),
    descripcion VARCHAR(255)
);

CREATE TABLE rol_permiso (
    rol_id     BIGINT NOT NULL REFERENCES rol(id) ON DELETE CASCADE,
    permiso_id BIGINT NOT NULL REFERENCES permiso(id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE usuario_rol (
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    rol_id     BIGINT NOT NULL REFERENCES rol(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE usuario_sucursal (
    usuario_id  BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    sucursal_id BIGINT NOT NULL REFERENCES sucursal(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, sucursal_id)
);

CREATE TABLE auditoria (
    id                BIGSERIAL PRIMARY KEY,
    empresa_id        BIGINT REFERENCES empresa(id),
    sucursal_id       BIGINT REFERENCES sucursal(id),
    usuario_id        BIGINT REFERENCES usuario(id),
    terminal          VARCHAR(60),
    modulo            VARCHAR(40) NOT NULL,
    entidad           VARCHAR(60) NOT NULL,
    entidad_id        BIGINT,
    accion            VARCHAR(20) NOT NULL
                      CHECK (accion IN ('CREAR','EDITAR','ELIMINAR','ANULAR','AUTORIZAR')),
    valores_anteriores JSONB,
    valores_nuevos    JSONB,
    ip                VARCHAR(45),
    creado_en         TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_auditoria_entidad ON auditoria(entidad, entidad_id);
CREATE INDEX idx_auditoria_fecha ON auditoria(creado_en);

CREATE TABLE configuracion (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT REFERENCES empresa(id),
    clave       VARCHAR(80) NOT NULL,
    valor       TEXT,
    tipo        VARCHAR(20) NOT NULL DEFAULT 'TEXTO'
                CHECK (tipo IN ('TEXTO','NUMERO','BOOLEAN','JSON','FECHA')),
    grupo       VARCHAR(40),
    descripcion VARCHAR(255),
    UNIQUE (empresa_id, clave)
);

CREATE TABLE empresa_modulo (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT NOT NULL REFERENCES empresa(id),
    modulo_codigo VARCHAR(30) NOT NULL,
    activo        BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (empresa_id, modulo_codigo)
);

CREATE TABLE secuencia_documento (
    id                BIGSERIAL PRIMARY KEY,
    empresa_id        BIGINT NOT NULL REFERENCES empresa(id),
    sucursal_id       BIGINT NOT NULL REFERENCES sucursal(id),
    tipo_comprobante  VARCHAR(30) NOT NULL,
    establecimiento   CHAR(3) NOT NULL,
    punto_emision     CHAR(3) NOT NULL,
    secuencial_actual BIGINT NOT NULL DEFAULT 0,
    activo            BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (empresa_id, sucursal_id, tipo_comprobante, establecimiento, punto_emision)
);

CREATE TABLE medio_pago (
    id                   BIGSERIAL PRIMARY KEY,
    empresa_id           BIGINT REFERENCES empresa(id),
    codigo               VARCHAR(20) NOT NULL,
    nombre               VARCHAR(60) NOT NULL,
    tipo                 VARCHAR(20) NOT NULL
                         CHECK (tipo IN ('EFECTIVO','TARJETA_DEBITO','TARJETA_CREDITO','TRANSFERENCIA','CREDITO','VALE','PUNTOS')),
    requiere_referencia  BOOLEAN NOT NULL DEFAULT FALSE,
    afecta_caja          BOOLEAN NOT NULL DEFAULT TRUE,
    codigo_sri_forma_pago VARCHAR(2),
    activo               BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (empresa_id, codigo)
);

CREATE TABLE dispositivo (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT NOT NULL REFERENCES empresa(id),
    sucursal_id   BIGINT REFERENCES sucursal(id),
    caja_id       BIGINT REFERENCES caja(id),
    tipo          VARCHAR(20) NOT NULL
                  CHECK (tipo IN ('IMPRESORA_TICKET','IMPRESORA_A4','LECTOR_BARRAS','BALANZA','GAVETA')),
    nombre        VARCHAR(80) NOT NULL,
    configuracion JSONB,
    activo        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE respaldo (
    id           BIGSERIAL PRIMARY KEY,
    empresa_id   BIGINT REFERENCES empresa(id),
    ruta_archivo VARCHAR(255) NOT NULL,
    tamano_bytes BIGINT,
    tipo         VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK (tipo IN ('MANUAL','PROGRAMADO')),
    estado       VARCHAR(20) NOT NULL DEFAULT 'OK' CHECK (estado IN ('OK','ERROR')),
    usuario_id   BIGINT REFERENCES usuario(id),
    creado_en    TIMESTAMP NOT NULL DEFAULT now()
);

-- =============================================================================
-- 2. CATALOGO
-- =============================================================================

CREATE TABLE categoria (
    id             BIGSERIAL PRIMARY KEY,
    empresa_id     BIGINT NOT NULL REFERENCES empresa(id),
    padre_id       BIGINT REFERENCES categoria(id),
    codigo         VARCHAR(30),
    nombre         VARCHAR(120) NOT NULL,
    descripcion    VARCHAR(255),
    orden          INT NOT NULL DEFAULT 0,
    activo         BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMP NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMP,
    UNIQUE (empresa_id, codigo)
);

CREATE TABLE marca (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT NOT NULL REFERENCES empresa(id),
    nombre      VARCHAR(120) NOT NULL,
    descripcion VARCHAR(255),
    logo_path   VARCHAR(255),
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (empresa_id, nombre)
);

CREATE TABLE unidad_medida (
    id                BIGSERIAL PRIMARY KEY,
    codigo            VARCHAR(10) NOT NULL UNIQUE,
    nombre            VARCHAR(60) NOT NULL,
    abreviatura       VARCHAR(10) NOT NULL,
    tipo              VARCHAR(20) NOT NULL
                      CHECK (tipo IN ('UNIDAD','PESO','VOLUMEN','LONGITUD')),
    permite_decimales BOOLEAN NOT NULL DEFAULT FALSE,
    activo            BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE impuesto (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT REFERENCES empresa(id),
    codigo_sri    VARCHAR(5),
    nombre        VARCHAR(80) NOT NULL,
    tipo          VARCHAR(20) NOT NULL CHECK (tipo IN ('IVA','ICE','IRBPNR','OTRO')),
    porcentaje    NUMERIC(6,2) NOT NULL DEFAULT 0,
    vigente_desde DATE NOT NULL DEFAULT CURRENT_DATE,
    vigente_hasta DATE,
    es_retencion  BOOLEAN NOT NULL DEFAULT FALSE,
    activo        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE producto (
    id                   BIGSERIAL PRIMARY KEY,
    empresa_id           BIGINT NOT NULL REFERENCES empresa(id),
    codigo               VARCHAR(40) NOT NULL,
    nombre               VARCHAR(200) NOT NULL,
    descripcion          TEXT,
    categoria_id         BIGINT REFERENCES categoria(id),
    marca_id             BIGINT REFERENCES marca(id),
    unidad_medida_id     BIGINT REFERENCES unidad_medida(id),
    tipo                 VARCHAR(20) NOT NULL DEFAULT 'SIMPLE'
                         CHECK (tipo IN ('SIMPLE','VARIANTE_PADRE','VARIANTE','KIT','SERVICIO','GRANEL')),
    producto_padre_id    BIGINT REFERENCES producto(id),
    costo                NUMERIC(14,4) NOT NULL DEFAULT 0,
    costo_promedio       NUMERIC(14,4) NOT NULL DEFAULT 0,
    controla_lote        BOOLEAN NOT NULL DEFAULT FALSE,
    controla_serie       BOOLEAN NOT NULL DEFAULT FALSE,
    es_granel            BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_plu           VARCHAR(20),
    peso_neto            NUMERIC(14,4),
    volumen              NUMERIC(14,4),
    ubicacion            VARCHAR(60),
    imagen_principal_id  BIGINT,             -- FK diferida (circular con producto_imagen)
    activo               BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en            TIMESTAMP NOT NULL DEFAULT now(),
    actualizado_en       TIMESTAMP,
    UNIQUE (empresa_id, codigo)
);
CREATE INDEX idx_producto_nombre ON producto(empresa_id, nombre);
CREATE INDEX idx_producto_categoria ON producto(categoria_id);
CREATE INDEX idx_producto_codigo_plu ON producto(codigo_plu);

CREATE TABLE producto_codigo_barra (
    id          BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    codigo      VARCHAR(30) NOT NULL UNIQUE,
    tipo        VARCHAR(12) NOT NULL
                CHECK (tipo IN ('EAN13','UPC_A','EAN8','DUN14','INTERNO','PLU')),
    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
    empaque_id  BIGINT              -- FK diferida (circular con producto_empaque)
);

CREATE TABLE producto_empaque (
    id              BIGSERIAL PRIMARY KEY,
    producto_id     BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    nombre          VARCHAR(40) NOT NULL,
    unidad_medida_id BIGINT REFERENCES unidad_medida(id),
    factor          NUMERIC(14,4) NOT NULL DEFAULT 1,
    es_compra       BOOLEAN NOT NULL DEFAULT FALSE,
    es_venta        BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_barra_id BIGINT REFERENCES producto_codigo_barra(id),
    activo          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE producto_impuesto (
    producto_id BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    impuesto_id BIGINT NOT NULL REFERENCES impuesto(id) ON DELETE CASCADE,
    PRIMARY KEY (producto_id, impuesto_id)
);

CREATE TABLE producto_imagen (
    id             BIGSERIAL PRIMARY KEY,
    producto_id    BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    ruta           VARCHAR(255) NOT NULL,
    nombre_archivo VARCHAR(120),
    orden          INT NOT NULL DEFAULT 0,
    es_principal   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE producto_componente (
    id                    BIGSERIAL PRIMARY KEY,
    producto_kit_id       BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    producto_componente_id BIGINT NOT NULL REFERENCES producto(id),
    cantidad              NUMERIC(14,4) NOT NULL DEFAULT 1,
    UNIQUE (producto_kit_id, producto_componente_id)
);

CREATE TABLE lista_precio (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT NOT NULL REFERENCES empresa(id),
    nombre      VARCHAR(80) NOT NULL,
    tipo        VARCHAR(20) NOT NULL DEFAULT 'PUBLICO'
                CHECK (tipo IN ('PUBLICO','MAYORISTA','CORPORATIVO','COSTO')),
    moneda      VARCHAR(3) NOT NULL DEFAULT 'USD',
    incluye_iva BOOLEAN NOT NULL DEFAULT FALSE,
    es_default  BOOLEAN NOT NULL DEFAULT FALSE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (empresa_id, nombre)
);

CREATE TABLE precio_producto (
    id              BIGSERIAL PRIMARY KEY,
    lista_precio_id BIGINT NOT NULL REFERENCES lista_precio(id) ON DELETE CASCADE,
    producto_id     BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    precio          NUMERIC(14,4) NOT NULL,
    sucursal_id     BIGINT REFERENCES sucursal(id),
    vigente_desde   DATE NOT NULL DEFAULT CURRENT_DATE,
    vigente_hasta   DATE
);
CREATE INDEX idx_precio_producto ON precio_producto(producto_id, lista_precio_id);

CREATE TABLE atributo_definicion (
    id           BIGSERIAL PRIMARY KEY,
    empresa_id   BIGINT NOT NULL REFERENCES empresa(id),
    categoria_id BIGINT REFERENCES categoria(id),
    codigo       VARCHAR(40) NOT NULL,
    nombre       VARCHAR(80) NOT NULL,
    tipo_dato    VARCHAR(20) NOT NULL
                 CHECK (tipo_dato IN ('TEXTO','NUMERO','DECIMAL','BOOLEAN','FECHA','LISTA')),
    opciones     JSONB,
    obligatorio  BOOLEAN NOT NULL DEFAULT FALSE,
    orden        INT NOT NULL DEFAULT 0,
    activo       BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (empresa_id, codigo)
);

CREATE TABLE producto_atributo (
    id                    BIGSERIAL PRIMARY KEY,
    producto_id           BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    atributo_definicion_id BIGINT NOT NULL REFERENCES atributo_definicion(id) ON DELETE CASCADE,
    valor                 TEXT,
    UNIQUE (producto_id, atributo_definicion_id)
);

-- =============================================================================
-- 3. INVENTARIO Y BODEGAS
-- =============================================================================

CREATE TABLE lote (
    id                 BIGSERIAL PRIMARY KEY,
    producto_id        BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    codigo             VARCHAR(60) NOT NULL,
    fecha_fabricacion  DATE,
    fecha_vencimiento  DATE,
    activo             BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (producto_id, codigo)
);
CREATE INDEX idx_lote_vencimiento ON lote(fecha_vencimiento);

CREATE TABLE serie (
    id           BIGSERIAL PRIMARY KEY,
    producto_id  BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    numero_serie VARCHAR(80) NOT NULL UNIQUE,
    estado       VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE'
                 CHECK (estado IN ('DISPONIBLE','VENDIDO','BAJA','RESERVADO')),
    bodega_id    BIGINT REFERENCES bodega(id),
    lote_id      BIGINT REFERENCES lote(id),
    venta_id     BIGINT              -- FK diferida (venta se crea mas abajo)
);

CREATE TABLE stock (
    id                 BIGSERIAL PRIMARY KEY,
    bodega_id          BIGINT NOT NULL REFERENCES bodega(id),
    producto_id        BIGINT NOT NULL REFERENCES producto(id),
    lote_id            BIGINT REFERENCES lote(id),
    cantidad           NUMERIC(14,4) NOT NULL DEFAULT 0,
    cantidad_reservada NUMERIC(14,4) NOT NULL DEFAULT 0,
    actualizado_en     TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_stock ON stock (bodega_id, producto_id, COALESCE(lote_id, 0));
CREATE INDEX idx_stock_producto ON stock(producto_id);

CREATE TABLE tipo_movimiento_inventario (
    id     BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL,
    signo  VARCHAR(10) NOT NULL CHECK (signo IN ('ENTRADA','SALIDA','AJUSTE'))
);

CREATE TABLE movimiento_inventario (
    id                 BIGSERIAL PRIMARY KEY,
    empresa_id         BIGINT NOT NULL REFERENCES empresa(id),
    sucursal_id        BIGINT REFERENCES sucursal(id),
    bodega_id          BIGINT NOT NULL REFERENCES bodega(id),
    producto_id        BIGINT NOT NULL REFERENCES producto(id),
    lote_id            BIGINT REFERENCES lote(id),
    serie_id           BIGINT REFERENCES serie(id),
    tipo_movimiento_id BIGINT NOT NULL REFERENCES tipo_movimiento_inventario(id),
    cantidad           NUMERIC(14,4) NOT NULL,
    costo_unitario     NUMERIC(14,4) NOT NULL DEFAULT 0,
    saldo_cantidad     NUMERIC(14,4),
    saldo_costo        NUMERIC(14,4),
    referencia_tipo    VARCHAR(25)
                       CHECK (referencia_tipo IN ('VENTA','COMPRA','AJUSTE','TRANSFERENCIA','TOMA',
                                                  'DEVOLUCION_VENTA','DEVOLUCION_COMPRA','INICIAL')),
    referencia_id      BIGINT,
    usuario_id         BIGINT REFERENCES usuario(id),
    nota               VARCHAR(255),
    creado_en          TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_mov_inv_producto ON movimiento_inventario(producto_id, creado_en);
CREATE INDEX idx_mov_inv_referencia ON movimiento_inventario(referencia_tipo, referencia_id);

CREATE TABLE transferencia (
    id                 BIGSERIAL PRIMARY KEY,
    empresa_id         BIGINT NOT NULL REFERENCES empresa(id),
    numero             VARCHAR(30) NOT NULL,
    sucursal_origen_id BIGINT NOT NULL REFERENCES sucursal(id),
    bodega_origen_id   BIGINT NOT NULL REFERENCES bodega(id),
    sucursal_destino_id BIGINT NOT NULL REFERENCES sucursal(id),
    bodega_destino_id  BIGINT NOT NULL REFERENCES bodega(id),
    estado             VARCHAR(20) NOT NULL DEFAULT 'BORRADOR'
                       CHECK (estado IN ('BORRADOR','EN_TRANSITO','RECIBIDA','ANULADA')),
    fecha_envio        TIMESTAMP,
    fecha_recepcion    TIMESTAMP,
    usuario_envia_id   BIGINT REFERENCES usuario(id),
    usuario_recibe_id  BIGINT REFERENCES usuario(id),
    nota               VARCHAR(255),
    creado_en          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE transferencia_detalle (
    id                BIGSERIAL PRIMARY KEY,
    transferencia_id  BIGINT NOT NULL REFERENCES transferencia(id) ON DELETE CASCADE,
    producto_id       BIGINT NOT NULL REFERENCES producto(id),
    lote_id           BIGINT REFERENCES lote(id),
    cantidad_enviada  NUMERIC(14,4) NOT NULL DEFAULT 0,
    cantidad_recibida NUMERIC(14,4) NOT NULL DEFAULT 0
);

CREATE TABLE toma_inventario (
    id           BIGSERIAL PRIMARY KEY,
    empresa_id   BIGINT NOT NULL REFERENCES empresa(id),
    bodega_id    BIGINT NOT NULL REFERENCES bodega(id),
    numero       VARCHAR(30) NOT NULL,
    estado       VARCHAR(20) NOT NULL DEFAULT 'ABIERTA'
                 CHECK (estado IN ('ABIERTA','EN_CONTEO','CERRADA','APLICADA','ANULADA')),
    conteo_ciego BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_inicio TIMESTAMP NOT NULL DEFAULT now(),
    fecha_cierre TIMESTAMP,
    usuario_id   BIGINT REFERENCES usuario(id),
    observacion  VARCHAR(255)
);

CREATE TABLE toma_inventario_detalle (
    id                BIGSERIAL PRIMARY KEY,
    toma_id           BIGINT NOT NULL REFERENCES toma_inventario(id) ON DELETE CASCADE,
    producto_id       BIGINT NOT NULL REFERENCES producto(id),
    lote_id           BIGINT REFERENCES lote(id),
    cantidad_sistema  NUMERIC(14,4) NOT NULL DEFAULT 0,
    cantidad_contada  NUMERIC(14,4) NOT NULL DEFAULT 0,
    diferencia        NUMERIC(14,4) NOT NULL DEFAULT 0,
    costo_unitario    NUMERIC(14,4) NOT NULL DEFAULT 0
);

CREATE TABLE ajuste_inventario (
    id         BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    bodega_id  BIGINT NOT NULL REFERENCES bodega(id),
    numero     VARCHAR(30) NOT NULL,
    tipo       VARCHAR(10) NOT NULL CHECK (tipo IN ('ENTRADA','SALIDA')),
    motivo     VARCHAR(20) NOT NULL
               CHECK (motivo IN ('MERMA','DANO','ROBO','VENCIMIENTO','CORRECCION','INICIAL','OTRO')),
    estado     VARCHAR(15) NOT NULL DEFAULT 'BORRADOR'
               CHECK (estado IN ('BORRADOR','APLICADO','ANULADO')),
    usuario_id BIGINT REFERENCES usuario(id),
    fecha      TIMESTAMP NOT NULL DEFAULT now(),
    nota       VARCHAR(255)
);

CREATE TABLE ajuste_inventario_detalle (
    id            BIGSERIAL PRIMARY KEY,
    ajuste_id     BIGINT NOT NULL REFERENCES ajuste_inventario(id) ON DELETE CASCADE,
    producto_id   BIGINT NOT NULL REFERENCES producto(id),
    lote_id       BIGINT REFERENCES lote(id),
    cantidad      NUMERIC(14,4) NOT NULL,
    costo_unitario NUMERIC(14,4) NOT NULL DEFAULT 0
);

-- =============================================================================
-- 4. COMPRAS Y PROVEEDORES
-- =============================================================================

CREATE TABLE proveedor (
    id                    BIGSERIAL PRIMARY KEY,
    empresa_id            BIGINT NOT NULL REFERENCES empresa(id),
    tipo_identificacion   VARCHAR(12) NOT NULL DEFAULT 'RUC'
                          CHECK (tipo_identificacion IN ('RUC','CEDULA','PASAPORTE')),
    numero_identificacion VARCHAR(20) NOT NULL,
    razon_social          VARCHAR(255) NOT NULL,
    nombre_comercial      VARCHAR(255),
    direccion             VARCHAR(255),
    telefono              VARCHAR(30),
    email                 VARCHAR(120),
    tipo_contribuyente    VARCHAR(40),
    dias_credito          INT NOT NULL DEFAULT 0,
    aplica_retencion_iva  BOOLEAN NOT NULL DEFAULT FALSE,
    aplica_retencion_renta BOOLEAN NOT NULL DEFAULT FALSE,
    activo                BOOLEAN NOT NULL DEFAULT TRUE,
    nota                  VARCHAR(255),
    creado_en             TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (empresa_id, numero_identificacion)
);

CREATE TABLE proveedor_contacto (
    id           BIGSERIAL PRIMARY KEY,
    proveedor_id BIGINT NOT NULL REFERENCES proveedor(id) ON DELETE CASCADE,
    nombre       VARCHAR(120) NOT NULL,
    cargo        VARCHAR(80),
    telefono     VARCHAR(30),
    email        VARCHAR(120)
);

CREATE TABLE producto_proveedor (
    id               BIGSERIAL PRIMARY KEY,
    producto_id      BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    proveedor_id     BIGINT NOT NULL REFERENCES proveedor(id) ON DELETE CASCADE,
    codigo_proveedor VARCHAR(60),
    costo_ultimo     NUMERIC(14,4),
    plazo_dias       INT,
    es_preferido     BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (producto_id, proveedor_id)
);

CREATE TABLE solicitud_compra (
    id                  BIGSERIAL PRIMARY KEY,
    empresa_id          BIGINT NOT NULL REFERENCES empresa(id),
    sucursal_id         BIGINT NOT NULL REFERENCES sucursal(id),
    numero              VARCHAR(30) NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'BORRADOR'
                        CHECK (estado IN ('BORRADOR','APROBADA','RECHAZADA','CONVERTIDA')),
    usuario_solicita_id BIGINT REFERENCES usuario(id),
    usuario_aprueba_id  BIGINT REFERENCES usuario(id),
    fecha               TIMESTAMP NOT NULL DEFAULT now(),
    nota                VARCHAR(255)
);

CREATE TABLE solicitud_compra_detalle (
    id            BIGSERIAL PRIMARY KEY,
    solicitud_id  BIGINT NOT NULL REFERENCES solicitud_compra(id) ON DELETE CASCADE,
    producto_id   BIGINT NOT NULL REFERENCES producto(id),
    cantidad      NUMERIC(14,4) NOT NULL,
    justificacion VARCHAR(255)
);

CREATE TABLE orden_compra (
    id                     BIGSERIAL PRIMARY KEY,
    empresa_id             BIGINT NOT NULL REFERENCES empresa(id),
    sucursal_id            BIGINT NOT NULL REFERENCES sucursal(id),
    proveedor_id           BIGINT NOT NULL REFERENCES proveedor(id),
    numero                 VARCHAR(30) NOT NULL,
    estado                 VARCHAR(20) NOT NULL DEFAULT 'BORRADOR'
                           CHECK (estado IN ('BORRADOR','APROBADA','RECEPCION_PARCIAL','RECIBIDA','ANULADA')),
    fecha_emision          TIMESTAMP NOT NULL DEFAULT now(),
    fecha_entrega_esperada DATE,
    condiciones_pago       VARCHAR(120),
    subtotal               NUMERIC(14,4) NOT NULL DEFAULT 0,
    descuento              NUMERIC(14,4) NOT NULL DEFAULT 0,
    impuestos              NUMERIC(14,4) NOT NULL DEFAULT 0,
    total                  NUMERIC(14,4) NOT NULL DEFAULT 0,
    usuario_id             BIGINT REFERENCES usuario(id),
    nota                   VARCHAR(255)
);

CREATE TABLE orden_compra_detalle (
    id                BIGSERIAL PRIMARY KEY,
    orden_compra_id   BIGINT NOT NULL REFERENCES orden_compra(id) ON DELETE CASCADE,
    producto_id       BIGINT NOT NULL REFERENCES producto(id),
    descripcion       VARCHAR(255),
    cantidad          NUMERIC(14,4) NOT NULL,
    cantidad_recibida NUMERIC(14,4) NOT NULL DEFAULT 0,
    precio_unitario   NUMERIC(14,4) NOT NULL DEFAULT 0,
    impuesto_id       BIGINT REFERENCES impuesto(id),
    descuento         NUMERIC(14,4) NOT NULL DEFAULT 0,
    subtotal          NUMERIC(14,4) NOT NULL DEFAULT 0,
    total             NUMERIC(14,4) NOT NULL DEFAULT 0
);

CREATE TABLE recepcion_compra (
    id              BIGSERIAL PRIMARY KEY,
    empresa_id      BIGINT NOT NULL REFERENCES empresa(id),
    orden_compra_id BIGINT REFERENCES orden_compra(id),
    numero          VARCHAR(30) NOT NULL,
    bodega_id       BIGINT NOT NULL REFERENCES bodega(id),
    fecha           TIMESTAMP NOT NULL DEFAULT now(),
    estado          VARCHAR(15) NOT NULL DEFAULT 'BORRADOR'
                    CHECK (estado IN ('BORRADOR','CONFIRMADA','ANULADA')),
    usuario_id      BIGINT REFERENCES usuario(id),
    nota            VARCHAR(255)
);

CREATE TABLE recepcion_compra_detalle (
    id                    BIGSERIAL PRIMARY KEY,
    recepcion_id          BIGINT NOT NULL REFERENCES recepcion_compra(id) ON DELETE CASCADE,
    orden_compra_detalle_id BIGINT REFERENCES orden_compra_detalle(id),
    producto_id           BIGINT NOT NULL REFERENCES producto(id),
    lote_id               BIGINT REFERENCES lote(id),
    lote_codigo           VARCHAR(60),
    fecha_vencimiento     DATE,
    cantidad_recibida     NUMERIC(14,4) NOT NULL DEFAULT 0,
    costo_unitario        NUMERIC(14,4) NOT NULL DEFAULT 0
);

CREATE TABLE factura_proveedor (
    id              BIGSERIAL PRIMARY KEY,
    empresa_id      BIGINT NOT NULL REFERENCES empresa(id),
    proveedor_id    BIGINT NOT NULL REFERENCES proveedor(id),
    orden_compra_id BIGINT REFERENCES orden_compra(id),
    recepcion_id    BIGINT REFERENCES recepcion_compra(id),
    tipo_documento  VARCHAR(15) NOT NULL DEFAULT 'FACTURA'
                    CHECK (tipo_documento IN ('FACTURA','NOTA_CREDITO','LIQUIDACION')),
    numero          VARCHAR(30) NOT NULL,
    fecha_emision   DATE NOT NULL,
    fecha_vencimiento DATE,
    subtotal        NUMERIC(14,4) NOT NULL DEFAULT 0,
    descuento       NUMERIC(14,4) NOT NULL DEFAULT 0,
    iva             NUMERIC(14,4) NOT NULL DEFAULT 0,
    total           NUMERIC(14,4) NOT NULL DEFAULT 0,
    saldo           NUMERIC(14,4) NOT NULL DEFAULT 0,
    estado_pago     VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE'
                    CHECK (estado_pago IN ('PENDIENTE','PARCIAL','PAGADA','ANULADA')),
    usuario_id      BIGINT REFERENCES usuario(id),
    UNIQUE (empresa_id, proveedor_id, numero)
);

CREATE TABLE factura_proveedor_detalle (
    id                  BIGSERIAL PRIMARY KEY,
    factura_proveedor_id BIGINT NOT NULL REFERENCES factura_proveedor(id) ON DELETE CASCADE,
    producto_id         BIGINT REFERENCES producto(id),
    descripcion         VARCHAR(255),
    cantidad            NUMERIC(14,4) NOT NULL DEFAULT 0,
    precio_unitario     NUMERIC(14,4) NOT NULL DEFAULT 0,
    impuesto_id         BIGINT REFERENCES impuesto(id),
    total               NUMERIC(14,4) NOT NULL DEFAULT 0
);

CREATE TABLE pago_proveedor (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT NOT NULL REFERENCES empresa(id),
    proveedor_id  BIGINT NOT NULL REFERENCES proveedor(id),
    numero        VARCHAR(30) NOT NULL,
    fecha         TIMESTAMP NOT NULL DEFAULT now(),
    total         NUMERIC(14,4) NOT NULL DEFAULT 0,
    medio_pago_id BIGINT REFERENCES medio_pago(id),
    referencia    VARCHAR(80),
    usuario_id    BIGINT REFERENCES usuario(id),
    nota          VARCHAR(255)
);

CREATE TABLE pago_proveedor_factura (
    id                   BIGSERIAL PRIMARY KEY,
    pago_id              BIGINT NOT NULL REFERENCES pago_proveedor(id) ON DELETE CASCADE,
    factura_proveedor_id BIGINT NOT NULL REFERENCES factura_proveedor(id),
    monto                NUMERIC(14,4) NOT NULL DEFAULT 0
);

CREATE TABLE retencion (
    id                    BIGSERIAL PRIMARY KEY,
    empresa_id            BIGINT NOT NULL REFERENCES empresa(id),
    proveedor_id          BIGINT NOT NULL REFERENCES proveedor(id),
    factura_proveedor_id  BIGINT REFERENCES factura_proveedor(id),
    comprobante_id        BIGINT,           -- FK diferida (comprobante se crea en seccion 8)
    establecimiento       CHAR(3) NOT NULL DEFAULT '001',
    punto_emision         CHAR(3) NOT NULL DEFAULT '001',
    secuencial            VARCHAR(9) NOT NULL,
    fecha_emision         DATE NOT NULL,
    clave_acceso          VARCHAR(49),
    base_iva              NUMERIC(14,4) NOT NULL DEFAULT 0,
    valor_iva             NUMERIC(14,4) NOT NULL DEFAULT 0,
    valor_retencion_iva   NUMERIC(14,4) NOT NULL DEFAULT 0,
    base_renta            NUMERIC(14,4) NOT NULL DEFAULT 0,
    valor_retencion_renta NUMERIC(14,4) NOT NULL DEFAULT 0,
    total_retenido        NUMERIC(14,4) NOT NULL DEFAULT 0,
    estado                VARCHAR(15) NOT NULL DEFAULT 'BORRADOR'
                          CHECK (estado IN ('BORRADOR','EMITIDO','ANULADO'))
);

CREATE TABLE retencion_detalle (
    id               BIGSERIAL PRIMARY KEY,
    retencion_id     BIGINT NOT NULL REFERENCES retencion(id) ON DELETE CASCADE,
    tipo_impuesto    VARCHAR(10) NOT NULL CHECK (tipo_impuesto IN ('IVA','RENTA')),
    codigo_retencion VARCHAR(10),
    base_imponible   NUMERIC(14,4) NOT NULL DEFAULT 0,
    porcentaje       NUMERIC(6,2) NOT NULL DEFAULT 0,
    valor_retenido   NUMERIC(14,4) NOT NULL DEFAULT 0
);

CREATE TABLE devolucion_compra (
    id                   BIGSERIAL PRIMARY KEY,
    empresa_id           BIGINT NOT NULL REFERENCES empresa(id),
    proveedor_id         BIGINT NOT NULL REFERENCES proveedor(id),
    factura_proveedor_id BIGINT REFERENCES factura_proveedor(id),
    recepcion_id         BIGINT REFERENCES recepcion_compra(id),
    bodega_id            BIGINT NOT NULL REFERENCES bodega(id),
    numero               VARCHAR(30) NOT NULL,
    fecha                TIMESTAMP NOT NULL DEFAULT now(),
    motivo               VARCHAR(20) NOT NULL DEFAULT 'OTRO'
                         CHECK (motivo IN ('DANO','VENCIMIENTO','ERROR','OTRO')),
    total                NUMERIC(14,4) NOT NULL DEFAULT 0,
    estado               VARCHAR(15) NOT NULL DEFAULT 'BORRADOR'
                         CHECK (estado IN ('BORRADOR','APLICADA','ANULADA'))
);

CREATE TABLE devolucion_compra_detalle (
    id             BIGSERIAL PRIMARY KEY,
    devolucion_id  BIGINT NOT NULL REFERENCES devolucion_compra(id) ON DELETE CASCADE,
    producto_id    BIGINT NOT NULL REFERENCES producto(id),
    lote_id        BIGINT REFERENCES lote(id),
    cantidad       NUMERIC(14,4) NOT NULL DEFAULT 0,
    costo_unitario NUMERIC(14,4) NOT NULL DEFAULT 0,
    total          NUMERIC(14,4) NOT NULL DEFAULT 0
);

-- =============================================================================
-- 5. CLIENTES Y FIDELIZACION
-- =============================================================================

CREATE TABLE cliente (
    id                    BIGSERIAL PRIMARY KEY,
    empresa_id            BIGINT NOT NULL REFERENCES empresa(id),
    tipo                  VARCHAR(20) NOT NULL DEFAULT 'CONSUMIDOR_FINAL'
                          CHECK (tipo IN ('CONSUMIDOR_FINAL','PERSONA_NATURAL','EMPRESA')),
    tipo_identificacion   VARCHAR(20) NOT NULL DEFAULT 'CONSUMIDOR_FINAL'
                          CHECK (tipo_identificacion IN ('CEDULA','RUC','PASAPORTE','CONSUMIDOR_FINAL')),
    numero_identificacion VARCHAR(20),
    nombres               VARCHAR(120),
    apellidos             VARCHAR(120),
    razon_social          VARCHAR(255),
    email                 VARCHAR(120),
    telefono              VARCHAR(30),
    tipo_contribuyente    VARCHAR(40),
    direccion             VARCHAR(255),
    es_credito            BOOLEAN NOT NULL DEFAULT FALSE,
    limite_credito        NUMERIC(14,4) NOT NULL DEFAULT 0,
    dias_credito          INT NOT NULL DEFAULT 0,
    saldo                 NUMERIC(14,4) NOT NULL DEFAULT 0,
    activo                BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en             TIMESTAMP NOT NULL DEFAULT now(),
    actualizado_en        TIMESTAMP,
    UNIQUE (empresa_id, numero_identificacion)
);

CREATE TABLE cliente_direccion (
    id          BIGSERIAL PRIMARY KEY,
    cliente_id  BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    alias       VARCHAR(60),
    direccion   VARCHAR(255) NOT NULL,
    referencia  VARCHAR(255),
    lat         NUMERIC(10,7),
    lng         NUMERIC(10,7),
    es_principal BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE cliente_contacto (
    id         BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    nombre     VARCHAR(120) NOT NULL,
    telefono   VARCHAR(30),
    email      VARCHAR(120),
    cargo      VARCHAR(80)
);

CREATE TABLE programa_fidelidad (
    id                   BIGSERIAL PRIMARY KEY,
    empresa_id           BIGINT NOT NULL REFERENCES empresa(id),
    nombre               VARCHAR(80) NOT NULL,
    activo               BOOLEAN NOT NULL DEFAULT TRUE,
    puntos_por_monto     NUMERIC(10,4) NOT NULL DEFAULT 1,
    valor_punto          NUMERIC(10,4) NOT NULL DEFAULT 0.01,
    vigencia_puntos_dias INT
);

CREATE TABLE cliente_puntos (
    id           BIGSERIAL PRIMARY KEY,
    cliente_id   BIGINT NOT NULL UNIQUE REFERENCES cliente(id) ON DELETE CASCADE,
    saldo_puntos NUMERIC(14,2) NOT NULL DEFAULT 0
);

CREATE TABLE puntos_movimiento (
    id               BIGSERIAL PRIMARY KEY,
    cliente_id       BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    tipo             VARCHAR(15) NOT NULL
                     CHECK (tipo IN ('ACUMULA','CANJE','AJUSTE','VENCIMIENTO')),
    puntos           NUMERIC(14,2) NOT NULL,
    referencia_tipo  VARCHAR(20),
    referencia_id    BIGINT,
    saldo_resultante NUMERIC(14,2),
    fecha            TIMESTAMP NOT NULL DEFAULT now(),
    nota             VARCHAR(255)
);

CREATE TABLE cupon (
    id             BIGSERIAL PRIMARY KEY,
    empresa_id     BIGINT NOT NULL REFERENCES empresa(id),
    codigo         VARCHAR(40) NOT NULL UNIQUE,
    tipo           VARCHAR(15) NOT NULL CHECK (tipo IN ('PORCENTAJE','MONTO')),
    valor          NUMERIC(14,4) NOT NULL DEFAULT 0,
    vigente_desde  DATE,
    vigente_hasta  DATE,
    usos_maximos   INT,
    usos_actuales  INT NOT NULL DEFAULT 0,
    activo         BOOLEAN NOT NULL DEFAULT TRUE
);

-- =============================================================================
-- 6. VENTAS Y POS
-- =============================================================================

CREATE TABLE sesion_caja (
    id                    BIGSERIAL PRIMARY KEY,
    empresa_id            BIGINT NOT NULL REFERENCES empresa(id),
    caja_id               BIGINT NOT NULL REFERENCES caja(id),
    usuario_apertura_id   BIGINT REFERENCES usuario(id),
    usuario_cierre_id     BIGINT REFERENCES usuario(id),
    fecha_apertura        TIMESTAMP NOT NULL DEFAULT now(),
    fecha_cierre          TIMESTAMP,
    monto_inicial         NUMERIC(14,4) NOT NULL DEFAULT 0,
    monto_esperado_efectivo NUMERIC(14,4),
    monto_contado_efectivo  NUMERIC(14,4),
    diferencia            NUMERIC(14,4),
    estado                VARCHAR(12) NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA','CERRADA')),
    observacion           VARCHAR(255)
);

CREATE TABLE sesion_caja_arqueo (
    id            BIGSERIAL PRIMARY KEY,
    sesion_caja_id BIGINT NOT NULL REFERENCES sesion_caja(id) ON DELETE CASCADE,
    denominacion  NUMERIC(10,2) NOT NULL,
    cantidad      INT NOT NULL DEFAULT 0,
    subtotal      NUMERIC(14,4) NOT NULL DEFAULT 0
);

CREATE TABLE venta (
    id               BIGSERIAL PRIMARY KEY,
    empresa_id       BIGINT NOT NULL REFERENCES empresa(id),
    sucursal_id      BIGINT NOT NULL REFERENCES sucursal(id),
    caja_id          BIGINT REFERENCES caja(id),
    sesion_caja_id   BIGINT REFERENCES sesion_caja(id),
    numero           VARCHAR(30) NOT NULL,
    cliente_id       BIGINT REFERENCES cliente(id),
    usuario_id       BIGINT REFERENCES usuario(id),
    tipo             VARCHAR(10) NOT NULL DEFAULT 'VENTA' CHECK (tipo IN ('VENTA','PEDIDO')),
    estado           VARCHAR(12) NOT NULL DEFAULT 'ABIERTA'
                     CHECK (estado IN ('ABIERTA','APARCADA','PAGADA','ANULADA')),
    pedido_id        BIGINT,       -- FK diferida (circular con pedido)
    fecha            TIMESTAMP NOT NULL DEFAULT now(),
    subtotal         NUMERIC(14,4) NOT NULL DEFAULT 0,
    descuento_total  NUMERIC(14,4) NOT NULL DEFAULT 0,
    impuesto_total   NUMERIC(14,4) NOT NULL DEFAULT 0,
    propina          NUMERIC(14,4) NOT NULL DEFAULT 0,
    total            NUMERIC(14,4) NOT NULL DEFAULT 0,
    costo_total      NUMERIC(14,4) NOT NULL DEFAULT 0,
    utilidad         NUMERIC(14,4) NOT NULL DEFAULT 0,
    autorizado_por_id BIGINT REFERENCES usuario(id),
    nota             VARCHAR(255)
);
CREATE INDEX idx_venta_fecha ON venta(fecha);
CREATE INDEX idx_venta_cliente ON venta(cliente_id);

CREATE TABLE venta_detalle (
    id              BIGSERIAL PRIMARY KEY,
    venta_id        BIGINT NOT NULL REFERENCES venta(id) ON DELETE CASCADE,
    producto_id     BIGINT REFERENCES producto(id),
    descripcion     VARCHAR(255),
    cantidad        NUMERIC(14,4) NOT NULL DEFAULT 0,
    peso            NUMERIC(14,4),
    precio_unitario NUMERIC(14,4) NOT NULL DEFAULT 0,
    descuento       NUMERIC(14,4) NOT NULL DEFAULT 0,
    impuesto_id     BIGINT REFERENCES impuesto(id),
    impuesto_valor  NUMERIC(14,4) NOT NULL DEFAULT 0,
    subtotal        NUMERIC(14,4) NOT NULL DEFAULT 0,
    total           NUMERIC(14,4) NOT NULL DEFAULT 0,
    costo_unitario  NUMERIC(14,4) NOT NULL DEFAULT 0,
    lote_id         BIGINT REFERENCES lote(id),
    serie_id        BIGINT REFERENCES serie(id),
    promocion_id    BIGINT          -- FK diferida (promocion se crea luego)
);
CREATE INDEX idx_venta_detalle_venta ON venta_detalle(venta_id);

CREATE TABLE venta_pago (
    id            BIGSERIAL PRIMARY KEY,
    venta_id      BIGINT NOT NULL REFERENCES venta(id) ON DELETE CASCADE,
    medio_pago_id BIGINT NOT NULL REFERENCES medio_pago(id),
    monto         NUMERIC(14,4) NOT NULL DEFAULT 0,
    referencia    VARCHAR(80),
    cambio        NUMERIC(14,4) NOT NULL DEFAULT 0,
    usuario_id    BIGINT REFERENCES usuario(id)
);

CREATE TABLE pedido (
    id                     BIGSERIAL PRIMARY KEY,
    empresa_id             BIGINT NOT NULL REFERENCES empresa(id),
    sucursal_id            BIGINT NOT NULL REFERENCES sucursal(id),
    cliente_id             BIGINT REFERENCES cliente(id),
    numero                 VARCHAR(30) NOT NULL,
    tipo_entrega           VARCHAR(10) NOT NULL DEFAULT 'PICKUP' CHECK (tipo_entrega IN ('PICKUP','DELIVERY')),
    estado                 VARCHAR(20) NOT NULL DEFAULT 'RECIBIDO'
                           CHECK (estado IN ('RECIBIDO','EN_PREPARACION','LISTO','EN_REPARTO','ENTREGADO','CANCELADO')),
    direccion_entrega_id   BIGINT REFERENCES cliente_direccion(id),
    fecha                  TIMESTAMP NOT NULL DEFAULT now(),
    fecha_entrega_estimada TIMESTAMP,
    subtotal               NUMERIC(14,4) NOT NULL DEFAULT 0,
    descuento              NUMERIC(14,4) NOT NULL DEFAULT 0,
    total                  NUMERIC(14,4) NOT NULL DEFAULT 0,
    venta_id               BIGINT REFERENCES venta(id),
    repartidor_usuario_id  BIGINT REFERENCES usuario(id),
    nota                   VARCHAR(255)
);

CREATE TABLE pedido_detalle (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
    producto_id     BIGINT REFERENCES producto(id),
    cantidad        NUMERIC(14,4) NOT NULL DEFAULT 0,
    precio_unitario NUMERIC(14,4) NOT NULL DEFAULT 0,
    total           NUMERIC(14,4) NOT NULL DEFAULT 0
);

CREATE TABLE devolucion_venta (
    id             BIGSERIAL PRIMARY KEY,
    empresa_id     BIGINT NOT NULL REFERENCES empresa(id),
    venta_id       BIGINT NOT NULL REFERENCES venta(id),
    cliente_id     BIGINT REFERENCES cliente(id),
    numero         VARCHAR(30) NOT NULL,
    fecha          TIMESTAMP NOT NULL DEFAULT now(),
    motivo         VARCHAR(255),
    total          NUMERIC(14,4) NOT NULL DEFAULT 0,
    bodega_id      BIGINT REFERENCES bodega(id),
    usuario_id     BIGINT REFERENCES usuario(id),
    estado         VARCHAR(15) NOT NULL DEFAULT 'BORRADOR'
                   CHECK (estado IN ('BORRADOR','APLICADA','ANULADA')),
    comprobante_id BIGINT           -- FK diferida (comprobante se crea luego)
);

CREATE TABLE devolucion_venta_detalle (
    id              BIGSERIAL PRIMARY KEY,
    devolucion_id   BIGINT NOT NULL REFERENCES devolucion_venta(id) ON DELETE CASCADE,
    producto_id     BIGINT REFERENCES producto(id),
    cantidad        NUMERIC(14,4) NOT NULL DEFAULT 0,
    precio_unitario NUMERIC(14,4) NOT NULL DEFAULT 0,
    total           NUMERIC(14,4) NOT NULL DEFAULT 0,
    reingresa_stock BOOLEAN NOT NULL DEFAULT TRUE
);

-- =============================================================================
-- 7. PROMOCIONES
-- =============================================================================

CREATE TABLE promocion (
    id                BIGSERIAL PRIMARY KEY,
    empresa_id        BIGINT NOT NULL REFERENCES empresa(id),
    nombre            VARCHAR(120) NOT NULL,
    descripcion       VARCHAR(255),
    tipo              VARCHAR(15) NOT NULL
                      CHECK (tipo IN ('PORCENTAJE','MONTO','NXM','PRECIO_FIJO','COMBO')),
    valor             NUMERIC(14,4) NOT NULL DEFAULT 0,
    cantidad_requerida INT,
    cantidad_pagada   INT,
    prioridad         INT NOT NULL DEFAULT 0,
    combinable        BOOLEAN NOT NULL DEFAULT FALSE,
    vigente_desde     DATE,
    vigente_hasta     DATE,
    estado            VARCHAR(12) NOT NULL DEFAULT 'BORRADOR'
                      CHECK (estado IN ('BORRADOR','ACTIVA','INACTIVA')),
    activo            BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE promocion_alcance (
    id            BIGSERIAL PRIMARY KEY,
    promocion_id  BIGINT NOT NULL REFERENCES promocion(id) ON DELETE CASCADE,
    tipo_alcance  VARCHAR(20) NOT NULL
                  CHECK (tipo_alcance IN ('PRODUCTO','CATEGORIA','MARCA','SUCURSAL','TIPO_CLIENTE')),
    referencia_id BIGINT
);

CREATE TABLE promocion_detalle (
    id           BIGSERIAL PRIMARY KEY,
    promocion_id BIGINT NOT NULL REFERENCES promocion(id) ON DELETE CASCADE,
    producto_id  BIGINT NOT NULL REFERENCES producto(id),
    cantidad     NUMERIC(14,4) NOT NULL DEFAULT 1
);

-- =============================================================================
-- 8. FACTURACION / SRI
-- =============================================================================

CREATE TABLE certificado_firma (
    id               BIGSERIAL PRIMARY KEY,
    empresa_id       BIGINT NOT NULL REFERENCES empresa(id),
    alias            VARCHAR(80) NOT NULL,
    ruta_p12         VARCHAR(255) NOT NULL,
    password_cifrada VARCHAR(255),
    fecha_desde      DATE,
    fecha_hasta      DATE,
    activo           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE comprobante (
    id                    BIGSERIAL PRIMARY KEY,
    empresa_id            BIGINT NOT NULL REFERENCES empresa(id),
    sucursal_id           BIGINT NOT NULL REFERENCES sucursal(id),
    tipo                  VARCHAR(20) NOT NULL
                          CHECK (tipo IN ('FACTURA','NOTA_CREDITO','NOTA_DEBITO','RETENCION',
                                          'GUIA_REMISION','LIQUIDACION_COMPRA')),
    establecimiento       CHAR(3) NOT NULL DEFAULT '001',
    punto_emision         CHAR(3) NOT NULL DEFAULT '001',
    secuencial            VARCHAR(9) NOT NULL,
    numero_completo       VARCHAR(17),
    clave_acceso          VARCHAR(49),
    fecha_emision         DATE NOT NULL,
    ambiente              VARCHAR(10) NOT NULL DEFAULT 'PRUEBAS' CHECK (ambiente IN ('PRUEBAS','PRODUCCION')),
    tipo_emision          VARCHAR(12) NOT NULL DEFAULT 'NORMAL' CHECK (tipo_emision IN ('NORMAL','CONTINGENCIA')),
    emisor_ruc            VARCHAR(13),
    emisor_razon_social   VARCHAR(255),
    emisor_direccion      VARCHAR(255),
    receptor_tipo_id      VARCHAR(20),
    receptor_numero_id    VARCHAR(20),
    receptor_razon_social VARCHAR(255),
    receptor_email        VARCHAR(120),
    subtotal              NUMERIC(14,4) NOT NULL DEFAULT 0,
    descuento             NUMERIC(14,4) NOT NULL DEFAULT 0,
    iva                   NUMERIC(14,4) NOT NULL DEFAULT 0,
    ice                   NUMERIC(14,4) NOT NULL DEFAULT 0,
    irbpnr                NUMERIC(14,4) NOT NULL DEFAULT 0,
    propina               NUMERIC(14,4) NOT NULL DEFAULT 0,
    total                 NUMERIC(14,4) NOT NULL DEFAULT 0,
    estado                VARCHAR(25) NOT NULL DEFAULT 'BORRADOR'
                          CHECK (estado IN ('BORRADOR','EMITIDO','PENDIENTE_AUTORIZACION',
                                            'AUTORIZADO','NO_AUTORIZADO','ANULADO')),
    venta_id              BIGINT REFERENCES venta(id),
    factura_proveedor_id  BIGINT REFERENCES factura_proveedor(id),
    motivo                VARCHAR(255),
    xml_path              VARCHAR(255),
    xml_firmado_path      VARCHAR(255),
    pdf_path              VARCHAR(255),
    creado_en             TIMESTAMP NOT NULL DEFAULT now(),
    actualizado_en        TIMESTAMP,
    UNIQUE (empresa_id, sucursal_id, tipo, establecimiento, punto_emision, secuencial)
);
CREATE INDEX idx_comprobante_fecha ON comprobante(fecha_emision);
CREATE INDEX idx_comprobante_clave ON comprobante(clave_acceso);

CREATE TABLE comprobante_detalle (
    id               BIGSERIAL PRIMARY KEY,
    comprobante_id   BIGINT NOT NULL REFERENCES comprobante(id) ON DELETE CASCADE,
    codigo_principal VARCHAR(40),
    codigo_auxiliar  VARCHAR(40),
    descripcion      VARCHAR(255),
    cantidad         NUMERIC(14,4) NOT NULL DEFAULT 0,
    precio_unitario  NUMERIC(14,4) NOT NULL DEFAULT 0,
    descuento        NUMERIC(14,4) NOT NULL DEFAULT 0,
    subtotal         NUMERIC(14,4) NOT NULL DEFAULT 0,
    impuesto_total   NUMERIC(14,4) NOT NULL DEFAULT 0,
    total            NUMERIC(14,4) NOT NULL DEFAULT 0
);

CREATE TABLE comprobante_impuesto (
    id                    BIGSERIAL PRIMARY KEY,
    comprobante_id        BIGINT NOT NULL REFERENCES comprobante(id) ON DELETE CASCADE,
    comprobante_detalle_id BIGINT REFERENCES comprobante_detalle(id) ON DELETE CASCADE,
    codigo_impuesto       VARCHAR(5),
    codigo_porcentaje     VARCHAR(10),
    tarifa                NUMERIC(6,2) NOT NULL DEFAULT 0,
    base_imponible        NUMERIC(14,4) NOT NULL DEFAULT 0,
    valor                 NUMERIC(14,4) NOT NULL DEFAULT 0
);

CREATE TABLE comprobante_pago (
    id               BIGSERIAL PRIMARY KEY,
    comprobante_id   BIGINT NOT NULL REFERENCES comprobante(id) ON DELETE CASCADE,
    forma_pago_codigo VARCHAR(2),
    total            NUMERIC(14,4) NOT NULL DEFAULT 0,
    plazo            INT,
    unidad_tiempo    VARCHAR(10)
);

CREATE TABLE comprobante_relacionado (
    id                          BIGSERIAL PRIMARY KEY,
    comprobante_id              BIGINT NOT NULL REFERENCES comprobante(id) ON DELETE CASCADE,
    tipo_relacionado            VARCHAR(20),
    comprobante_relacionado_id  BIGINT REFERENCES comprobante(id)
);

CREATE TABLE guia_remision_datos (
    id                        BIGSERIAL PRIMARY KEY,
    comprobante_id            BIGINT NOT NULL UNIQUE REFERENCES comprobante(id) ON DELETE CASCADE,
    transportista_ruc         VARCHAR(13),
    transportista_razon_social VARCHAR(255),
    placa                     VARCHAR(15),
    punto_partida             VARCHAR(255),
    punto_llegada             VARCHAR(255),
    fecha_inicio_transporte   DATE,
    fecha_fin_transporte      DATE
);

CREATE TABLE guia_remision_detalle (
    id             BIGSERIAL PRIMARY KEY,
    comprobante_id BIGINT NOT NULL REFERENCES comprobante(id) ON DELETE CASCADE,
    producto_id    BIGINT REFERENCES producto(id),
    cantidad       NUMERIC(14,4) NOT NULL DEFAULT 0,
    descripcion    VARCHAR(255)
);

-- =============================================================================
-- 9. CLAVES FORANEAS DIFERIDAS (relaciones circulares)
-- =============================================================================

ALTER TABLE producto
    ADD CONSTRAINT fk_producto_imagen_principal
    FOREIGN KEY (imagen_principal_id) REFERENCES producto_imagen(id);

ALTER TABLE producto_codigo_barra
    ADD CONSTRAINT fk_codigo_barra_empaque
    FOREIGN KEY (empaque_id) REFERENCES producto_empaque(id);

ALTER TABLE serie
    ADD CONSTRAINT fk_serie_venta
    FOREIGN KEY (venta_id) REFERENCES venta(id);

ALTER TABLE venta
    ADD CONSTRAINT fk_venta_pedido
    FOREIGN KEY (pedido_id) REFERENCES pedido(id);

ALTER TABLE venta_detalle
    ADD CONSTRAINT fk_venta_detalle_promocion
    FOREIGN KEY (promocion_id) REFERENCES promocion(id);

ALTER TABLE retencion
    ADD CONSTRAINT fk_retencion_comprobante
    FOREIGN KEY (comprobante_id) REFERENCES comprobante(id);

ALTER TABLE devolucion_venta
    ADD CONSTRAINT fk_devolucion_venta_comprobante
    FOREIGN KEY (comprobante_id) REFERENCES comprobante(id);
