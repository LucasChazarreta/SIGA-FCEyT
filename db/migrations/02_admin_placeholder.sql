USE siga;

-- Verificación rápida
SELECT id, username, password_hash, estado, activo, rol_id FROM usuario;
SELECT id, nro, asunto, tipo, estado, fecha FROM tramite ORDER BY fecha DESC LIMIT 10;
SELECT id, tipo, cantidad, fecha, estado, activo FROM movimiento ORDER BY fecha DESC LIMIT 10;

-- Si necesitás limpiar el admin inserto por CreateAdminUser,
-- primero “desvinculá” movimientos y luego borrá el usuario:
-- UPDATE movimiento SET usuario_id = NULL WHERE usuario_id = (SELECT id FROM usuario WHERE username='admin');
-- DELETE FROM usuario WHERE username='admin';
