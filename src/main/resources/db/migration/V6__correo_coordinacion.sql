-- Correo de la coordinación del programa. Si existe, el syllabus exportado deja vacío «Docente (s)» y
-- muestra este correo; los programas sin coordinación (p. ej. posgrados) muestran el docente asignado.
ALTER TABLE programs ADD COLUMN coordination_email VARCHAR(150);
