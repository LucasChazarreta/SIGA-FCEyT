-- Crear base de datos SIGA (si no existe)
CREATE DATABASE IF NOT EXISTS siga DEFAULT CHARACTER SET utf8mb4;
USE siga;

-- Tabla de roles
CREATE TABLE rol (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(30) UNIQUE NOT NULL
);

-- Tabla de usuarios
CREATE TABLE usuario (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(60) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(120),
  estado ENUM('ACTIVO','INACTIVO') DEFAULT 'ACTIVO',
  rol_id INT NOT NULL,
  FOREIGN KEY (rol_id) REFERENCES rol(id)
);

-- Tabla de categorías
CREATE TABLE categoria (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(60) UNIQUE NOT NULL
);

-- Tabla de insumos
CREATE TABLE insumo (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(50) UNIQUE NOT NULL,
  descripcion VARCHAR(255) NOT NULL,
  categoria_id INT NOT NULL,
  stock_minimo INT NOT NULL DEFAULT 0,
  ubicacion VARCHAR(120),
  estado ENUM('ACTIVO','INACTIVO') DEFAULT 'ACTIVO',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (categoria_id) REFERENCES categoria(id)
);

-- Tabla de movimientos
CREATE TABLE movimiento (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  insumo_id BIGINT NOT NULL,
  tipo ENUM('ENTRADA','SALIDA') NOT NULL,
  cantidad INT NOT NULL,
  destino_fuente VARCHAR(120),
  fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  usuario_id BIGINT,
  FOREIGN KEY (insumo_id) REFERENCES insumo(id),
  FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE tramite (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nro VARCHAR(50) UNIQUE NOT NULL,
  asunto VARCHAR(255) NOT NULL,
  estado VARCHAR(30) NOT NULL,
  fecha DATETIME NOT NULL,
  solicitante VARCHAR(120),
  descripcion TEXT NULL,
  destino VARCHAR(120));

-- Datos iniciales
INSERT INTO rol(nombre) VALUES ('ADMIN'), ('INVENTARIO');
INSERT INTO categoria(nombre) VALUES ('Oficina'), ('Limpieza'), ('Eléctrico'), ('Bien mueble');
