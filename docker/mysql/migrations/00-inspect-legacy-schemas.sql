-- T09.16 - FuelBridge AV4
-- Inspeccion previa a la migracion de datos
-- Solo lectura: no modifica ni elimina registros.

-- 1. Verificar la existencia de las tablas de origen
-- y destino.

SELECT
    m.entity_name AS entidad,
    m.source_schema AS base_origen,
    IF(src.table_name IS NULL, 'NO', 'SI')
        AS existe_origen,
    m.target_schema AS base_destino,
    IF(dst.table_name IS NULL, 'NO', 'SI')
        AS existe_destino
FROM (
    SELECT
        'equipment' AS entity_name,
        'fuelbridge_platform' AS source_schema,
        'fuelbridge_equipment' AS target_schema
    UNION ALL
    SELECT
        'fuel_products',
        'fuelbridge_platform',
        'fuelbridge_inventory'
) m
LEFT JOIN information_schema.tables src
    ON src.table_schema = m.source_schema
    AND src.table_name = m.entity_name
LEFT JOIN information_schema.tables dst
    ON dst.table_schema = m.target_schema
    AND dst.table_name = m.entity_name
ORDER BY m.entity_name;

-- 2. Inspeccionar las columnas existentes.

SELECT
    table_schema,
    table_name,
    column_name,
    column_type,
    is_nullable,
    column_default,
    extra
FROM information_schema.columns
WHERE
    (table_schema = 'fuelbridge_platform'
        AND table_name IN ('equipment', 'fuel_products'))
    OR
    (table_schema = 'fuelbridge_equipment'
        AND table_name = 'equipment')
    OR
    (table_schema = 'fuelbridge_inventory'
        AND table_name = 'fuel_products')
ORDER BY table_schema, table_name, ordinal_position;
