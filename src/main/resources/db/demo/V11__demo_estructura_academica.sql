-- Demo: estructura académica de la Facultad de Ingeniería según el esquema de la jefatura.
-- Matemáticas, Educación y Ciencias Computacionales ofrecen asignaturas que entran en la malla
-- de varios programas; Matemáticas no tiene programa propio.

UPDATE departments SET name = 'Departamento de Ciencias Computacionales', code = 'DCC',
    coordination_email = 'computacion@autonoma.edu.co'
WHERE id = '10000000-0000-0000-0000-000000000001';
UPDATE departments SET name = 'Departamento de Matemáticas', code = 'DMAT',
    coordination_email = 'matematicas@autonoma.edu.co'
WHERE id = '10000000-0000-0000-0000-000000000002';

INSERT INTO departments (id, name, code, faculty_id, coordination_email) VALUES
    ('10000000-0000-0000-0000-000000000003', 'Departamento de Electrónica y Automatización', 'DEA',
     (SELECT id FROM faculties WHERE code = 'FING'), 'electronica@autonoma.edu.co'),
    ('10000000-0000-0000-0000-000000000004', 'Departamento de Mecánica y Producción', 'DMP',
     (SELECT id FROM faculties WHERE code = 'FING'), 'mecanica@autonoma.edu.co'),
    ('10000000-0000-0000-0000-000000000005', 'Departamento de Educación', 'DEDU',
     (SELECT id FROM faculties WHERE code = 'FESE'), 'educacion@autonoma.edu.co'),
    ('10000000-0000-0000-0000-000000000006', 'Departamento de Administración y Economía', 'DAE',
     (SELECT id FROM faculties WHERE code = 'FESE'), NULL);

-- Los programas demo de Ingeniería de Software y Matemáticas Aplicadas pasan a ser Ingeniería
-- Electrónica e Ingeniería Biomédica (Matemáticas no tiene programa).
UPDATE programs SET name = 'Ingeniería Electrónica', code = 'IEL', department_id = '10000000-0000-0000-0000-000000000003'
WHERE id = '20000000-0000-0000-0000-000000000002';
UPDATE programs SET name = 'Ingeniería Biomédica', code = 'IBM', department_id = '10000000-0000-0000-0000-000000000003'
WHERE id = '20000000-0000-0000-0000-000000000003';
UPDATE programs SET faculty_id = (SELECT id FROM faculties WHERE code = 'FESE')
WHERE id = '20000000-0000-0000-0000-000000000005';

INSERT INTO programs (id, name, code, level, department_id) VALUES
    ('20000000-0000-0000-0000-000000000007', 'Ingeniería Mecánica', 'IME', 'PREGRADO', '10000000-0000-0000-0000-000000000004'),
    ('20000000-0000-0000-0000-000000000008', 'Ingeniería Industrial', 'IIN', 'PREGRADO', '10000000-0000-0000-0000-000000000004'),
    ('20000000-0000-0000-0000-000000000009', 'Administración de Empresas', 'ADE', 'PREGRADO', '10000000-0000-0000-0000-000000000006');

INSERT INTO subjects (id, name, code, credits, department_id, professor_id, hours_presencial, hours_dirigido,
                      hours_independiente, prerequisites) VALUES
    ('30000000-0000-0000-0000-000000000009', 'Matemáticas I', 'MAT100', 4, '10000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', 4, 2, 6, 'Ninguno'),
    ('30000000-0000-0000-0000-000000000010', 'Lógica de Programación', 'SIS100', 4, '10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 4, 2, 6, 'Ninguno'),
    ('30000000-0000-0000-0000-000000000011', 'Fundamentos de Ingeniería', 'ING100', 2, '10000000-0000-0000-0000-000000000001', NULL, 2, 1, 3, 'Ninguno'),
    ('30000000-0000-0000-0000-000000000012', 'Teoría General de Sistemas', 'SIS102', 3, '10000000-0000-0000-0000-000000000001', NULL, 3, 1, 5, 'Ninguno'),
    ('30000000-0000-0000-0000-000000000013', 'Ética y Competencias Ciudadanas', 'EDU100', 2, '10000000-0000-0000-0000-000000000005', NULL, 2, 1, 3, 'Ninguno'),
    ('30000000-0000-0000-0000-000000000014', 'Inglés I', 'LEN100', 2, '10000000-0000-0000-0000-000000000005', NULL, 3, 0, 3, 'Ninguno'),
    ('30000000-0000-0000-0000-000000000015', 'Física Mecánica', 'FIS100', 4, '10000000-0000-0000-0000-000000000002', NULL, 4, 2, 6, 'Matemáticas I'),
    ('30000000-0000-0000-0000-000000000016', 'Circuitos Eléctricos I', 'ELE200', 4, '10000000-0000-0000-0000-000000000003', NULL, 4, 2, 6, 'Física Mecánica');

-- Planes de estudio: el plan migrado de cada programa se renombra y se rehace su malla.
DELETE FROM study_plan_subjects;
DELETE FROM study_plans WHERE program_id IN ('20000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003');
UPDATE study_plans SET name = 'Plan 2020' WHERE program_id = '20000000-0000-0000-0000-000000000001';
UPDATE study_plans SET name = 'Plan 2024' WHERE program_id IN ('20000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000005');

INSERT INTO study_plans (id, program_id, name) VALUES
    ('60000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'Plan 2023'),
    ('60000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'Plan 2022'),
    ('60000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001', 'Plan 2026');
UPDATE study_plans SET is_active = FALSE WHERE program_id = '20000000-0000-0000-0000-000000000001' AND name = 'Plan 2020';

INSERT INTO study_plan_subjects (plan_id, subject_id, semester)
SELECT sp.id, m.subject_id::uuid, m.semester
FROM study_plans sp
JOIN (VALUES
    -- Ingeniería de Sistemas: el plan 2020 (anterior) y el 2026 (vigente).
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000009', 1),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000011', 1),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000010', 1),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000012', 1),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000013', 1),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000005', 2),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000006', 2),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000004', 2),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000014', 2),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000001', 4),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000002', 6),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000003', 6),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000009', 1),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000011', 1),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000010', 1),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000012', 1),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000013', 1),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000005', 2),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000006', 2),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000004', 2),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000014', 2),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000015', 3),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000001', 4),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000002', 6),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000003', 6),
    -- Ingeniería Electrónica e Ingeniería Biomédica comparten las asignaturas de ciencias básicas.
    ('20000000-0000-0000-0000-000000000002', 'Plan 2023', '30000000-0000-0000-0000-000000000009', 1),
    ('20000000-0000-0000-0000-000000000002', 'Plan 2023', '30000000-0000-0000-0000-000000000010', 1),
    ('20000000-0000-0000-0000-000000000002', 'Plan 2023', '30000000-0000-0000-0000-000000000013', 1),
    ('20000000-0000-0000-0000-000000000002', 'Plan 2023', '30000000-0000-0000-0000-000000000005', 2),
    ('20000000-0000-0000-0000-000000000002', 'Plan 2023', '30000000-0000-0000-0000-000000000015', 2),
    ('20000000-0000-0000-0000-000000000002', 'Plan 2023', '30000000-0000-0000-0000-000000000006', 3),
    ('20000000-0000-0000-0000-000000000002', 'Plan 2023', '30000000-0000-0000-0000-000000000016', 3),
    ('20000000-0000-0000-0000-000000000003', 'Plan 2022', '30000000-0000-0000-0000-000000000009', 1),
    ('20000000-0000-0000-0000-000000000003', 'Plan 2022', '30000000-0000-0000-0000-000000000013', 1),
    ('20000000-0000-0000-0000-000000000003', 'Plan 2022', '30000000-0000-0000-0000-000000000005', 2),
    ('20000000-0000-0000-0000-000000000003', 'Plan 2022', '30000000-0000-0000-0000-000000000015', 2),
    ('20000000-0000-0000-0000-000000000003', 'Plan 2022', '30000000-0000-0000-0000-000000000016', 3),
    ('20000000-0000-0000-0000-000000000004', 'Plan 2024', '30000000-0000-0000-0000-000000000007', 1),
    ('20000000-0000-0000-0000-000000000005', 'Plan 2024', '30000000-0000-0000-0000-000000000008', 1)
) AS m (program_id, plan, subject_id, semester)
    ON sp.program_id = m.program_id::uuid AND sp.name = m.plan;

-- Coordinador del Departamento de Matemáticas: firma los syllabus de las asignaturas de su
-- departamento, aunque se dicten en otros programas.
INSERT INTO users (id, email, password_hash, name, gender, phone, student_code, employee_code) VALUES
    ('40000000-0000-0000-0000-000000000007', 'coordinador.matematicas@autonoma.edu.co', crypt('Uam2026!', gen_salt('bf', 10)),
     'Jorge Iván Restrepo', 'MASCULINO', '3002223344', NULL, 'EMP-006');
INSERT INTO user_roles (user_id, role) VALUES ('40000000-0000-0000-0000-000000000007', 'COORDINADOR_DEPARTAMENTO');
INSERT INTO user_departments (user_id, department_id) VALUES
    ('40000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000002');

-- Certificados de firma de prueba (autofirmados, contraseña Uam2026!) registrados para los
-- firmantes demo. Los mismos archivos están en docs/certificados-demo/ para probar la carga.
INSERT INTO user_certificates (user_id, file, certificate_subject, certificate_issuer, certificate_serial, not_after)
SELECT u.id, decode(c.archivo, 'base64'), c.titular, c.titular, c.serie, c.vence::timestamptz
FROM users u
JOIN (VALUES
    ('coordinador@autonoma.edu.co',
     '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',
     'CN=María Fernanda López,OU=Certificado de prueba (demo),O=Universidad Autonoma de Manizales,C=CO', '4704DD5CDDEBAF51', '2036-10-09T19:05:56Z'),
    ('coordinador.matematicas@autonoma.edu.co',
     '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',
     'CN=Jorge Iván Restrepo,OU=Certificado de prueba (demo),O=Universidad Autonoma de Manizales,C=CO', 'E3AFC43CA883BF4C', '2036-10-09T19:05:56Z'),
    ('registro@autonoma.edu.co',
     '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',
     'CN=Roberto Registro Académico,OU=Certificado de prueba (demo),O=Universidad Autonoma de Manizales,C=CO', '44AA7D34395B0383', '2036-10-09T19:05:56Z'),
    ('coordinador.programa@autonoma.edu.co',
     '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',
     'CN=Coordinadora de Posgrado,OU=Certificado de prueba (demo),O=Universidad Autonoma de Manizales,C=CO', 'ECF71FB3B2A623C2', '2036-10-09T19:05:56Z')
) AS c (email, archivo, titular, serie, vence) ON c.email = u.email;

-- Syllabus 2026-01 de las asignaturas nuevas, para que la malla muestre todos los estados.
INSERT INTO syllabus (id, subject_id, academic_period, title, status, created_by, created_at, updated_at)
VALUES
    ('50000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000009', '2026-01', 'Syllabus Matemáticas I - 2026-01', 'ACTIVO', '40000000-0000-0000-0000-000000000007', '2026-01-08T08:00:00Z', '2026-01-08T08:00:00Z'),
    ('50000000-0000-0000-0000-000000000007', '30000000-0000-0000-0000-000000000010', '2026-01', 'Syllabus Lógica de Programación - 2026-01', 'ACTIVO', '40000000-0000-0000-0000-000000000002', '2026-01-09T08:00:00Z', '2026-01-09T08:00:00Z'),
    ('50000000-0000-0000-0000-000000000008', '30000000-0000-0000-0000-000000000013', '2026-01', 'Syllabus Ética y Competencias Ciudadanas - 2026-01', 'BORRADOR', '40000000-0000-0000-0000-000000000003', '2026-01-15T08:00:00Z', '2026-01-15T08:00:00Z');

INSERT INTO syllabus_sections (syllabus_id, section_type, title, content, evaluation, section_order)
SELECT s.id, t.section_type, t.title, t.content, CASE WHEN t.section_type = 'evaluation' THEN e.tabla END, t.section_order
FROM (SELECT '[{"corte":1,"label":"I","porcentaje":30,"tipos":[{"tipo":"Quiz","porcentaje":30},{"tipo":"Taller","porcentaje":30},{"tipo":"Parcial","porcentaje":40}]},{"corte":2,"label":"II","porcentaje":35,"tipos":[{"tipo":"Quiz","porcentaje":30},{"tipo":"Taller","porcentaje":30},{"tipo":"Parcial","porcentaje":40}]},{"corte":3,"label":"III","porcentaje":35,"tipos":[{"tipo":"Proyecto","porcentaje":40},{"tipo":"Examen final","porcentaje":60}]}]'::jsonb AS tabla) e,
     syllabus s
JOIN (VALUES
    ('50000000-0000-0000-0000-000000000006', 'identification', 'Identificación', '', 1),
    ('50000000-0000-0000-0000-000000000006', 'justification', 'Justificación',
     E'Las matemáticas son el lenguaje con el que la ingeniería describe, modela y resuelve problemas. Este curso nivela y fortalece el razonamiento algebraico, trigonométrico y funcional que exigen los cursos de cálculo, física y programación de los primeros semestres.\n\nPor ser una asignatura del Departamento de Matemáticas, se ofrece a todos los programas de ingeniería con los mismos resultados de aprendizaje.', 2),
    ('50000000-0000-0000-0000-000000000006', 'performance', 'Desempeño Esperado',
     E'RESULTADOS DE APRENDIZAJE\n- Resuelve ecuaciones e inecuaciones algebraicas y las aplica a problemas de ingeniería.\n- Interpreta funciones y sus gráficas para describir el comportamiento de un fenómeno.\n- Usa identidades trigonométricas para simplificar y resolver problemas geométricos.', 3),
    ('50000000-0000-0000-0000-000000000006', 'contents', 'Contenidos',
     E'UNIDAD 1. ÁLGEBRA\n- Números reales y sus propiedades\n- Expresiones algebraicas, factorización y fracciones\n- Ecuaciones e inecuaciones lineales y cuadráticas\n\nUNIDAD 2. FUNCIONES\n- Dominio, rango y gráfica\n- Funciones lineales, cuadráticas, polinómicas y racionales\n- Funciones exponenciales y logarítmicas\n\nUNIDAD 3. TRIGONOMETRÍA\n- Razones e identidades trigonométricas\n- Ley de senos y de cosenos', 4),
    ('50000000-0000-0000-0000-000000000006', 'teaching_strategies', 'Estrategias de Enseñanza',
     E'Clases magistrales con participación activa, talleres semanales de ejercicios en grupo y uso de GeoGebra para visualizar funciones. El estudiante prepara cada tema con las lecturas indicadas en la plataforma institucional.', 5),
    ('50000000-0000-0000-0000-000000000006', 'evaluation', 'Estrategia de Evaluación del Aprendizaje', NULL, 6),
    ('50000000-0000-0000-0000-000000000006', 'bibliography', 'Bibliografía',
     E'Requerida:\nStewart, J., Redlin, L. y Watson, S. (2017). *Precálculo: matemáticas para el cálculo* (7.a ed.). Cengage Learning.\nSugerida:\nSwokowski, E. y Cole, J. (2011). *Álgebra y trigonometría con geometría analítica* (13.a ed.). Cengage Learning.', 7),
    ('50000000-0000-0000-0000-000000000007', 'identification', 'Identificación', '', 1),
    ('50000000-0000-0000-0000-000000000007', 'justification', 'Justificación',
     E'La lógica de programación es la base para construir soluciones computacionales. El curso desarrolla el pensamiento algorítmico: descomponer un problema, representar su solución y traducirla a un lenguaje de programación.', 2),
    ('50000000-0000-0000-0000-000000000007', 'performance', 'Desempeño Esperado',
     E'RESULTADOS DE APRENDIZAJE\n- Diseña algoritmos con estructuras de control secuenciales, condicionales y repetitivas.\n- Implementa y prueba programas sencillos en Python.\n- Explica con claridad la solución que propone a un problema.', 3),
    ('50000000-0000-0000-0000-000000000007', 'contents', 'Contenidos',
     E'- Algoritmos, diagramas de flujo y pseudocódigo\n- Variables, tipos de datos y operadores\n- Estructuras condicionales\n- Ciclos\n- Funciones y modularidad\n- Listas y cadenas', 4),
    ('50000000-0000-0000-0000-000000000007', 'teaching_strategies', 'Estrategias de Enseñanza',
     E'Aprendizaje basado en problemas: cada semana se plantea un reto que los estudiantes resuelven en parejas en el laboratorio, con retroalimentación inmediata del docente.', 5),
    ('50000000-0000-0000-0000-000000000007', 'evaluation', 'Estrategia de Evaluación del Aprendizaje', NULL, 6),
    ('50000000-0000-0000-0000-000000000007', 'bibliography', 'Bibliografía',
     E'Requerida:\nJoyanes, L. (2008). *Fundamentos de programación* (4.a ed.). McGraw-Hill.\nSugerida:\nDowney, A. (2015). *Think Python* (2.a ed.). O''Reilly.', 7),
    ('50000000-0000-0000-0000-000000000008', 'identification', 'Identificación', '', 1),
    ('50000000-0000-0000-0000-000000000008', 'justification', 'Justificación',
     E'Formación en ética profesional y ciudadanía para estudiantes de primer semestre de todos los programas.', 2),
    ('50000000-0000-0000-0000-000000000008', 'contents', 'Contenidos', '', 3),
    ('50000000-0000-0000-0000-000000000008', 'evaluation', 'Estrategia de Evaluación del Aprendizaje', NULL, 4)
) AS t (syllabus_id, section_type, title, content, section_order) ON t.syllabus_id::uuid = s.id;

INSERT INTO syllabus_versions (syllabus_id, version_number, snapshot, change_description, modified_by, created_at)
SELECT s.id, 1,
       jsonb_build_object(
           'title', s.title, 'status', s.status, 'headerTitle', s.header_title,
           'documentCode', s.document_code, 'documentDate', s.document_date,
           'sections', (SELECT jsonb_agg(jsonb_build_object(
                                   'sectionType', sec.section_type, 'title', sec.title, 'content', sec.content,
                                   'evaluation', sec.evaluation, 'order', sec.section_order) ORDER BY sec.section_order)
                        FROM syllabus_sections sec WHERE sec.syllabus_id = s.id)),
       'Creación del syllabus', s.created_by, s.created_at
FROM syllabus s
WHERE s.id IN ('50000000-0000-0000-0000-000000000006', '50000000-0000-0000-0000-000000000007',
               '50000000-0000-0000-0000-000000000008');
