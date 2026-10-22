-- Carga inicial de datos: Bodegas y Productos
-- 2 bodegas registradas
INSERT INTO bodegas (id, nombre, ubicacion) VALUES (1, 'Bodega Central TechNova', 'Edificio Principal - Piso 1');
INSERT INTO bodegas (id, nombre, ubicacion) VALUES (2, 'Bodega Periféricos y Redes', 'Campus Tecnológico - Sala B');

-- 3 productos asociados a esas bodegas
INSERT INTO productos (id, nombre, stock, bodega_id) VALUES (1, 'Cable HDMI 4K 2m', 45, 1);
INSERT INTO productos (id, nombre, stock, bodega_id) VALUES (2, 'Teclado Mecánico USB', 20, 1);
INSERT INTO productos (id, nombre, stock, bodega_id) VALUES (3, 'Adaptador USB-C a Ethernet Gigabit', 35, 2);
