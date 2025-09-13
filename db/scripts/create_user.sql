-- USO:
-- 1) Reemplazar :USER y :PASS por el nombre y clave deseados
-- 2) Ejecutar con usuario root (o con permisos para CREATE USER/GRANT)
-- 3) La BD 'siga' debe existir (se crea con 001_init.sql)

CREATE USER ':USER'@'localhost' IDENTIFIED BY ':PASS';
GRANT ALL PRIVILEGES ON siga.* TO ':USER'@'localhost';
FLUSH PRIVILEGES;

-- Si tu MySQL usa caching_sha2_password y querés evitar allowPublicKeyRetrieval:
-- ALTER USER ':USER'@'localhost' IDENTIFIED WITH mysql_native_password BY ':PASS';
-- FLUSH PRIVILEGES;
