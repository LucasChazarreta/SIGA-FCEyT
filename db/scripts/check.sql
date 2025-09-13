USE siga;
SHOW TABLES;

SELECT 'categoria' AS tabla, COUNT(*) AS filas FROM categoria
UNION ALL
SELECT 'rol', COUNT(*) FROM rol
UNION ALL
SELECT 'usuario', COUNT(*) FROM usuario
UNION ALL
SELECT 'insumo', COUNT(*) FROM insumo
UNION ALL
SELECT 'movimiento', COUNT(*) FROM movimiento;
