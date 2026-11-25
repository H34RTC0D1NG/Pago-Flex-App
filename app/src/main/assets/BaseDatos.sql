CREATE TABLE usuarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    rut TEXT NOT NULL UNIQUE,
    nombre TEXT NOT NULL,
    correo TEXT NOT NULL UNIQUE,
    comuna TEXT NOT NULL,
    telefono TEXT,
    pin_hash TEXT NOT NULL,
    rol TEXT NOT NULL,
    activo INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE compromisos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    folio TEXT NOT NULL UNIQUE,
    usuario_id INTEGER NOT NULL,
    empresa TEXT NOT NULL,
    tipo TEXT NOT NULL,
    concepto TEXT NOT NULL,
    cuota INTEGER NOT NULL,
    total_cuotas INTEGER NOT NULL,
    monto INTEGER NOT NULL,
    vencimiento TEXT NOT NULL,
    estado TEXT NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE comprobantes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero TEXT NOT NULL UNIQUE,
    compromiso_id INTEGER NOT NULL,
    folio TEXT NOT NULL,
    usuario_id INTEGER NOT NULL,
    empresa TEXT NOT NULL,
    concepto TEXT NOT NULL,
    monto INTEGER NOT NULL,
    fecha TEXT NOT NULL,
    hora TEXT NOT NULL,
    medio TEXT NOT NULL,
    canal TEXT NOT NULL,
    a_tiempo INTEGER NOT NULL,
    FOREIGN KEY (compromiso_id) REFERENCES compromisos(id)
);

CREATE TABLE reportes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero TEXT NOT NULL UNIQUE,
    compromiso_id INTEGER NOT NULL,
    folio TEXT NOT NULL,
    usuario_id INTEGER NOT NULL,
    motivo TEXT NOT NULL,
    comentario TEXT,
    estado TEXT NOT NULL,
    respuesta TEXT,
    fecha TEXT NOT NULL,
    FOREIGN KEY (compromiso_id) REFERENCES compromisos(id)
);

CREATE TABLE recomendaciones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero TEXT NOT NULL UNIQUE,
    usuario_id INTEGER NOT NULL,
    organizacion TEXT NOT NULL,
    rubro TEXT NOT NULL,
    comuna TEXT NOT NULL,
    contacto TEXT,
    estado TEXT NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

INSERT INTO usuarios (id, rut, nombre, correo, comuna, telefono, pin_hash, rol, activo) VALUES
(1, '11111111-1', 'Administrador PagoFlex', 'admin@pagoflex.cl', 'Santiago', '', '1f59ccb193c6c36958ab475f89159e3375c49ffc5d7c5a149f333b306e82e03c', 'ADMIN', 1),
(2, '15834207-3', 'Ana Rojas Pérez', 'ana.rojas@correo.cl', 'Independencia', '912345678', '1f59ccb193c6c36958ab475f89159e3375c49ffc5d7c5a149f333b306e82e03c', 'CLIENTE', 1),
(3, '12345678-5', 'Pedro Soto Muñoz', 'pedro.soto@correo.cl', 'Recoleta', '', '1f59ccb193c6c36958ab475f89159e3375c49ffc5d7c5a149f333b306e82e03c', 'CLIENTE', 1);

INSERT INTO compromisos (id, folio, usuario_id, empresa, tipo, concepto, cuota, total_cuotas, monto, vencimiento, estado) VALUES
(1, 'PF-1001', 2, 'Plaza Oriente', 'ARRIENDO', 'Arriendo depto', 2, 12, 420000, strftime('%d-%m-%Y', 'now', 'localtime', '-36 days'), 'PAGADO'),
(2, 'PF-1002', 2, 'Plaza Oriente', 'ARRIENDO', 'Arriendo depto', 3, 12, 420000, strftime('%d-%m-%Y', 'now', 'localtime', '-6 days'), 'PENDIENTE'),
(3, 'PF-1003', 2, 'Plaza Oriente', 'ARRIENDO', 'Arriendo depto', 4, 12, 420000, strftime('%d-%m-%Y', 'now', 'localtime', '+24 days'), 'PENDIENTE'),
(4, 'PF-1004', 2, 'Caja Los Andes', 'CREDITO', 'Crédito de consumo', 6, 24, 89900, strftime('%d-%m-%Y', 'now', 'localtime', '-28 days'), 'PAGADO'),
(5, 'PF-1005', 2, 'Caja Los Andes', 'CREDITO', 'Crédito de consumo', 7, 24, 89900, strftime('%d-%m-%Y', 'now', 'localtime', '+2 days'), 'PENDIENTE'),
(6, 'PF-1006', 2, 'Club Deportivo Las Mercedes', 'CLUB', 'Cuota social', 10, 12, 15000, strftime('%d-%m-%Y', 'now', 'localtime', '+9 days'), 'PENDIENTE'),
(7, 'PF-1007', 2, 'Fundación Las Rosas', 'FUNDACION', 'Aporte mensual', 0, 0, 10000, strftime('%d-%m-%Y', 'now', 'localtime', '+15 days'), 'PENDIENTE');

INSERT INTO comprobantes (id, numero, compromiso_id, folio, usuario_id, empresa, concepto, monto, fecha, hora, medio, canal, a_tiempo) VALUES
(1, 'C-5001', 1, 'PF-1001', 2, 'Plaza Oriente', 'Arriendo depto · Cuota 2/12', 420000, strftime('%d-%m-%Y', 'now', 'localtime', '-38 days'), '10:15', 'Transferencia', 'App', 1),
(2, 'C-5002', 4, 'PF-1004', 2, 'Caja Los Andes', 'Crédito de consumo · Cuota 6/24', 89900, strftime('%d-%m-%Y', 'now', 'localtime', '-25 days'), '10:15', 'Transferencia', 'Presencial', 0);

INSERT INTO recomendaciones (id, numero, usuario_id, organizacion, rubro, comuna, contacto, estado) VALUES
(1, 'REC-101', 2, 'Jardín Infantil Los Peques', 'Jardín infantil', 'Independencia', '', 'CONTACTADA');
