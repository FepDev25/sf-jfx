-- =============================================================================
-- V2 — Datos base del sistema
-- Catalogos globales: permisos, roles, unidades de medida, impuestos,
-- medios de pago y tipos de movimiento de inventario.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Permisos: producto cartesiano modulo x accion
-- ---------------------------------------------------------------------------
INSERT INTO permiso (codigo, modulo, accion, descripcion)
SELECT lower(m.modulo) || '.' || lower(a.accion),
       m.modulo,
       a.accion,
       m.etiqueta || ' - ' || a.etiqueta
FROM (VALUES
        ('PRODUCTOS','Catalogo'),
        ('INVENTARIO','Inventario'),
        ('BODEGAS','Bodegas'),
        ('POS','Ventas/POS'),
        ('FACTURACION','Facturacion'),
        ('COMPRAS','Compras'),
        ('PROVEEDORES','Proveedores'),
        ('CLIENTES','Clientes'),
        ('FIDELIZACION','Fidelizacion'),
        ('REPORTES','Reportes'),
        ('LISTAS_PRECIO','Listas de precio'),
        ('PROMOCIONES','Promociones'),
        ('GRANEL','Granel/PLU'),
        ('USUARIOS','Usuarios'),
        ('ROLES','Roles'),
        ('AUDITORIA','Auditoria'),
        ('CONFIGURACION','Configuracion'),
        ('EMPRESA','Empresa'),
        ('SUCURSALES','Sucursales'),
        ('CAJAS','Cajas'),
        ('MEDIOS_PAGO','Medios de pago'),
        ('DISPOSITIVOS','Dispositivos'),
        ('RESPALDO','Respaldo')
     ) AS m(modulo, etiqueta)
CROSS JOIN (VALUES
        ('VER','Ver'),
        ('CREAR','Crear'),
        ('EDITAR','Editar'),
        ('ELIMINAR','Eliminar'),
        ('APROBAR','Aprobar'),
        ('EXPORTAR','Exportar'),
        ('ANULAR','Anular')
     ) AS a(accion, etiqueta);

-- ---------------------------------------------------------------------------
-- Roles base del sistema (empresa_id NULL = globales)
-- ---------------------------------------------------------------------------
INSERT INTO rol (empresa_id, nombre, descripcion, es_sistema) VALUES
    (NULL, 'Administrador', 'Acceso total al sistema', TRUE),
    (NULL, 'Gerente',       'Operacion y analisis completos', TRUE),
    (NULL, 'Supervisor',    'Supervisa caja, inventario y promociones', TRUE),
    (NULL, 'Cajero',        'Opera el punto de venta', TRUE),
    (NULL, 'Bodeguero',     'Inventario, bodegas y recepciones', TRUE),
    (NULL, 'Contador',      'Facturacion, compras y reportes', TRUE);

-- ---------------------------------------------------------------------------
-- Asignacion de permisos por rol
-- ---------------------------------------------------------------------------

-- Administrador: todos
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r CROSS JOIN permiso p
WHERE r.nombre = 'Administrador';

-- Gerente: modulos operativos y de analisis, todas las acciones
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r CROSS JOIN permiso p
WHERE r.nombre = 'Gerente'
  AND p.modulo IN ('PRODUCTOS','INVENTARIO','BODEGAS','POS','FACTURACION','COMPRAS',
                   'PROVEEDORES','CLIENTES','FIDELIZACION','REPORTES','LISTAS_PRECIO',
                   'PROMOCIONES','GRANEL');

-- Supervisor
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r CROSS JOIN permiso p
WHERE r.nombre = 'Supervisor'
  AND (
        (p.modulo IN ('POS','CLIENTES','PRODUCTOS','INVENTARIO')
            AND p.accion IN ('VER','CREAR','EDITAR','ANULAR'))
     OR (p.modulo IN ('REPORTES','FACTURACION') AND p.accion IN ('VER','EXPORTAR'))
     OR (p.modulo = 'PROMOCIONES' AND p.accion = 'VER')
      );

-- Cajero
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r CROSS JOIN permiso p
WHERE r.nombre = 'Cajero'
  AND (
        (p.modulo = 'POS' AND p.accion IN ('VER','CREAR'))
     OR (p.modulo = 'CLIENTES' AND p.accion IN ('VER','CREAR','EDITAR'))
     OR (p.modulo = 'PRODUCTOS' AND p.accion = 'VER')
     OR (p.modulo = 'FACTURACION' AND p.accion = 'VER')
     OR (p.modulo = 'PROMOCIONES' AND p.accion = 'VER')
      );

-- Bodeguero
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r CROSS JOIN permiso p
WHERE r.nombre = 'Bodeguero'
  AND (
        (p.modulo IN ('INVENTARIO','BODEGAS') AND p.accion IN ('VER','CREAR','EDITAR'))
     OR (p.modulo = 'PRODUCTOS' AND p.accion = 'VER')
     OR (p.modulo = 'COMPRAS' AND p.accion IN ('VER','CREAR'))
     OR (p.modulo = 'PROVEEDORES' AND p.accion = 'VER')
      );

-- Contador
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r CROSS JOIN permiso p
WHERE r.nombre = 'Contador'
  AND (
        (p.modulo IN ('FACTURACION','COMPRAS','PROVEEDORES')
            AND p.accion IN ('VER','CREAR','EDITAR','ANULAR','EXPORTAR'))
     OR (p.modulo IN ('CLIENTES','REPORTES') AND p.accion IN ('VER','EXPORTAR'))
      );

-- ---------------------------------------------------------------------------
-- Unidades de medida
-- ---------------------------------------------------------------------------
INSERT INTO unidad_medida (codigo, nombre, abreviatura, tipo, permite_decimales) VALUES
    ('UNIDAD', 'Unidad',  'u',   'UNIDAD',  FALSE),
    ('CAJA',   'Caja',    'cja', 'UNIDAD',  FALSE),
    ('DOCENA', 'Docena',  'doc', 'UNIDAD',  FALSE),
    ('PACK',   'Pack',    'pk',  'UNIDAD',  FALSE),
    ('KG',     'Kilogramo','kg', 'PESO',    TRUE),
    ('GR',     'Gramo',   'g',   'PESO',    TRUE),
    ('LB',     'Libra',   'lb',  'PESO',    TRUE),
    ('LT',     'Litro',   'l',   'VOLUMEN', TRUE),
    ('ML',     'Mililitro','ml', 'VOLUMEN', TRUE),
    ('MT',     'Metro',   'm',   'LONGITUD',TRUE);

-- ---------------------------------------------------------------------------
-- Impuestos (Ecuador - SRI)
-- ---------------------------------------------------------------------------
INSERT INTO impuesto (empresa_id, codigo_sri, nombre, tipo, porcentaje, es_retencion) VALUES
    (NULL, '2', 'IVA 15%',  'IVA',     15.00, FALSE),
    (NULL, '2', 'IVA 0%',   'IVA',      0.00, FALSE),
    (NULL, '2', 'No objeto de IVA', 'IVA', 0.00, FALSE),
    (NULL, '3', 'ICE',      'ICE',      0.00, FALSE),
    (NULL, '5', 'IRBPNR',   'IRBPNR',   0.00, FALSE);

-- ---------------------------------------------------------------------------
-- Medios de pago (con mapeo a forma de pago del SRI)
-- ---------------------------------------------------------------------------
INSERT INTO medio_pago (empresa_id, codigo, nombre, tipo, requiere_referencia, afecta_caja, codigo_sri_forma_pago) VALUES
    (NULL, 'EFECTIVO',    'Efectivo',            'EFECTIVO',        FALSE, TRUE,  '01'),
    (NULL, 'DEBITO',      'Tarjeta de debito',   'TARJETA_DEBITO',  TRUE,  TRUE,  '19'),
    (NULL, 'CREDITO_TAR', 'Tarjeta de credito',  'TARJETA_CREDITO', TRUE,  TRUE,  '19'),
    (NULL, 'TRANSF',      'Transferencia',       'TRANSFERENCIA',   TRUE,  TRUE,  '20'),
    (NULL, 'CREDITO',     'Credito (a cuenta)',  'CREDITO',         FALSE, FALSE, '21'),
    (NULL, 'VALE',        'Vale / cupon',        'VALE',            TRUE,  FALSE, NULL),
    (NULL, 'PUNTOS',      'Puntos de fidelidad', 'PUNTOS',          FALSE, FALSE, NULL);

-- ---------------------------------------------------------------------------
-- Tipos de movimiento de inventario
-- ---------------------------------------------------------------------------
INSERT INTO tipo_movimiento_inventario (codigo, nombre, signo) VALUES
    ('INICIAL',              'Carga inicial',            'ENTRADA'),
    ('COMPRA',               'Recepcion de compra',      'ENTRADA'),
    ('VENTA',                'Venta',                    'SALIDA'),
    ('AJUSTE_ENTRADA',       'Ajuste de entrada',        'ENTRADA'),
    ('AJUSTE_SALIDA',        'Ajuste de salida',         'SALIDA'),
    ('TRANSFERENCIA_SALIDA', 'Transferencia (salida)',   'SALIDA'),
    ('TRANSFERENCIA_ENTRADA','Transferencia (entrada)',  'ENTRADA'),
    ('DEVOLUCION_VENTA',     'Devolucion de venta',      'ENTRADA'),
    ('DEVOLUCION_COMPRA',    'Devolucion a proveedor',   'SALIDA'),
    ('TOMA',                 'Toma de inventario',       'AJUSTE');
