-- Datos demo (solo desarrollo). Mismos usuarios de prueba del proyecto anterior.
-- Contraseña de todos los usuarios demo: Uam2026!
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO departments (id, name, code) VALUES
    ('10000000-0000-0000-0000-000000000001', 'Departamento de Ingeniería de Sistemas', 'DIS'),
    ('10000000-0000-0000-0000-000000000002', 'Departamento de Ciencias Básicas', 'DCB');

INSERT INTO programs (id, name, code, level, department_id) VALUES
    ('20000000-0000-0000-0000-000000000001', 'Ingeniería de Sistemas', 'IS', 'PREGRADO', '10000000-0000-0000-0000-000000000001'),
    ('20000000-0000-0000-0000-000000000002', 'Ingeniería de Software', 'ISW', 'PREGRADO', '10000000-0000-0000-0000-000000000001'),
    ('20000000-0000-0000-0000-000000000003', 'Matemáticas Aplicadas', 'MA', 'PREGRADO', '10000000-0000-0000-0000-000000000002'),
    ('20000000-0000-0000-0000-000000000004', 'Maestría en Inteligencia Artificial', 'MIA', 'MAESTRIA', NULL),
    ('20000000-0000-0000-0000-000000000005', 'Especialización en Gerencia de Proyectos', 'EGP', 'ESPECIALIZACION', NULL),
    ('20000000-0000-0000-0000-000000000006', 'Doctorado en Ingeniería', 'DING', 'DOCTORADO', NULL);

INSERT INTO users (id, email, password_hash, name, gender, phone, student_code, employee_code) VALUES
    ('40000000-0000-0000-0000-000000000001', 'profesor@autonoma.edu.co', crypt('Uam2026!', gen_salt('bf', 10)), 'Carlos Andrés Martínez', 'MASCULINO', '3001234567', NULL, 'EMP-001'),
    ('40000000-0000-0000-0000-000000000002', 'coordinador@autonoma.edu.co', crypt('Uam2026!', gen_salt('bf', 10)), 'María Fernanda López', 'FEMENINO', '3009876543', NULL, 'EMP-002'),
    ('40000000-0000-0000-0000-000000000003', 'admin@autonoma.edu.co', crypt('Uam2026!', gen_salt('bf', 10)), 'Administrador General', 'MASCULINO', '3005551234', NULL, 'EMP-003'),
    ('40000000-0000-0000-0000-000000000004', 'registro@autonoma.edu.co', crypt('Uam2026!', gen_salt('bf', 10)), 'Roberto Registro Académico', 'MASCULINO', '3001112233', NULL, 'EMP-004'),
    ('40000000-0000-0000-0000-000000000005', 'estudiante@autonoma.edu.co', crypt('Uam2026!', gen_salt('bf', 10)), 'Ana María Estudiante', 'FEMENINO', '3004445566', 'EST-2025-001', NULL),
    ('40000000-0000-0000-0000-000000000006', 'coordinador.programa@autonoma.edu.co', crypt('Uam2026!', gen_salt('bf', 10)), 'Coordinadora de Posgrado', 'FEMENINO', '3007778899', NULL, 'EMP-005');

INSERT INTO user_roles (user_id, role) VALUES
    ('40000000-0000-0000-0000-000000000001', 'PROFESOR'),
    ('40000000-0000-0000-0000-000000000001', 'COORDINADOR_PROGRAMA'),
    ('40000000-0000-0000-0000-000000000002', 'COORDINADOR_DEPARTAMENTO'),
    ('40000000-0000-0000-0000-000000000002', 'COORDINADOR_PROGRAMA'),
    ('40000000-0000-0000-0000-000000000003', 'ADMINISTRADOR'),
    ('40000000-0000-0000-0000-000000000004', 'ADM_REGISTRO'),
    ('40000000-0000-0000-0000-000000000005', 'ESTUDIANTE'),
    ('40000000-0000-0000-0000-000000000006', 'COORDINADOR_PROGRAMA');

INSERT INTO user_departments (user_id, department_id) VALUES
    ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002'),
    ('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002'),
    ('40000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001');

INSERT INTO user_programs (user_id, program_id) VALUES
    ('40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002'),
    ('40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000004'),
    ('40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000005'),
    ('40000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000002'),
    ('40000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000004'),
    ('40000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000005');

-- El profesor demo tenía otorgada la exportación por lote en el proyecto anterior.
INSERT INTO user_permission_overrides (user_id, action, resource, allow, created_by) VALUES
    ('40000000-0000-0000-0000-000000000001', 'batch', 'export', TRUE, '40000000-0000-0000-0000-000000000004');

INSERT INTO subjects (id, name, code, credits, semester, program_id, department_id, professor_id,
                      hours_presencial, hours_dirigido, hours_independiente, prerequisites) VALUES
    ('30000000-0000-0000-0000-000000000001', 'Base de Datos I', 'SIS201', 4, 4, '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 4, 2, 3, 'Algoritmos y Estructuras de Datos'),
    ('30000000-0000-0000-0000-000000000002', 'Ingeniería de Software', 'SIS301', 4, 6, '20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 4, 2, 3, 'Ingeniería de Software I'),
    ('30000000-0000-0000-0000-000000000003', 'Redes de Computadores', 'SIS302', 3, 6, '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', NULL, 3, 1, 2, 'Sistemas Operativos'),
    ('30000000-0000-0000-0000-000000000004', 'Algoritmos y Estructuras de Datos', 'SIS101', 4, 2, '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 4, 2, 3, 'Programación I'),
    ('30000000-0000-0000-0000-000000000005', 'Cálculo I', 'MAT101', 4, 1, '20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', NULL, 4, 2, 3, 'Ninguno'),
    ('30000000-0000-0000-0000-000000000006', 'Álgebra Lineal', 'MAT201', 3, 3, '20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', NULL, 3, 1, 2, 'Cálculo I'),
    ('30000000-0000-0000-0000-000000000007', 'Aprendizaje Automático Avanzado', 'MIA501', 3, 1, '20000000-0000-0000-0000-000000000004', NULL, '40000000-0000-0000-0000-000000000001', 3, 1, 3, 'Estadística aplicada y programación en Python'),
    ('30000000-0000-0000-0000-000000000008', 'Gestión de Proyectos de Innovación', 'EGP201', 3, 1, '20000000-0000-0000-0000-000000000005', NULL, '40000000-0000-0000-0000-000000000001', 3, 1, 2, 'Ninguno');

INSERT INTO syllabus (id, subject_id, academic_period, title, header_title, document_code, document_date, status, created_by, created_at, updated_at) VALUES
    ('50000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '2026-01', 'Syllabus Base de Datos I - 2026-01', 'SYLLABUS DE LA ASIGNATURA', 'GFR-FOR-005', '01/02/2026', 'ACTIVO', '40000000-0000-0000-0000-000000000002', '2026-01-10T08:00:00Z', '2026-02-15T10:30:00Z'),
    ('50000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', '2026-01', 'Syllabus Ingeniería de Software - 2026-01', 'SYLLABUS DE LA ASIGNATURA', 'GFR-FOR-005', '01/02/2026', 'ACTIVO', '40000000-0000-0000-0000-000000000002', '2026-01-12T08:00:00Z', '2026-02-20T14:00:00Z'),
    ('50000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000004', '2025-02', 'Syllabus Algoritmos - 2025-02', NULL, NULL, NULL, 'INACTIVO', '40000000-0000-0000-0000-000000000002', '2025-08-10T08:00:00Z', '2025-12-15T10:00:00Z'),
    ('50000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000004', '2026-01', 'Syllabus Algoritmos - 2026-01', NULL, NULL, NULL, 'BORRADOR', '40000000-0000-0000-0000-000000000001', '2026-01-20T08:00:00Z', '2026-02-01T09:00:00Z'),
    ('50000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000007', '2026-01', 'Syllabus Aprendizaje Automático Avanzado - 2026-01', 'SYLLABUS DE LA ASIGNATURA', 'GFR-FOR-005', '01/03/2026', 'ACTIVO', '40000000-0000-0000-0000-000000000006', '2026-01-30T08:00:00Z', '2026-02-28T09:00:00Z');

-- Estrategia de evaluación (UC20) de los syllabus demo de pregrado: los cortes de la plantilla
-- institucional con sus tipos y porcentajes parciales.
WITH evaluacion AS (
    SELECT '[{"corte":1,"label":"PRIMER CORTE","porcentaje":30,"tipos":[{"tipo":"Quiz","porcentaje":30},{"tipo":"Talleres","porcentaje":30},{"tipo":"Avance proyecto","porcentaje":40}]},{"corte":2,"label":"SEGUNDO CORTE","porcentaje":35,"tipos":[{"tipo":"Quiz","porcentaje":30},{"tipo":"Talleres","porcentaje":30},{"tipo":"Avance proyecto","porcentaje":40}]},{"corte":3,"label":"TERCER CORTE","porcentaje":35,"tipos":[{"tipo":"Quiz","porcentaje":40},{"tipo":"Entrega final proyecto","porcentaje":60}]}]'::jsonb AS tabla
)
INSERT INTO syllabus_sections (syllabus_id, section_type, title, content, evaluation, section_order)
SELECT '50000000-0000-0000-0000-000000000001'::uuid, t.section_type, t.title, t.content, t.evaluation, t.section_order
FROM evaluacion, LATERAL (VALUES
    ('identification', 'Identificación de la Asignatura', E'Universidad Autónoma de Manizales\nFacultad de Ingeniería\nDepartamento de Ingeniería de Sistemas\n\nAsignatura: Base de Datos I\nCódigo: SIS201\nCréditos: 4\nSemestre: 4\nPrograma: Ingeniería de Sistemas', NULL::jsonb, 1),
    ('justification', 'Justificación', 'La asignatura de Base de Datos I permite al estudiante comprender los fundamentos teóricos y prácticos del diseño, implementación y administración de bases de datos relacionales. En el contexto actual, donde la información es un activo estratégico, el conocimiento de sistemas de gestión de bases de datos es fundamental para cualquier profesional de la ingeniería de sistemas.', NULL, 2),
    ('performance', 'Desempeño Esperado', E'RESULTADOS DE APRENDIZAJE:\n\nSO2 – Capacidad de aplicar el diseño de ingeniería para producir soluciones que satisfagan necesidades específicas teniendo en cuenta la salud pública, la seguridad y el bienestar, así como factores globales, culturales, sociales, ambientales y económicos.\n\nINDICADOR DE DESEMPEÑO:\n\nPI1: Identifica necesidades, requerimientos y criterios específicos para abordar diferentes problemas de ingeniería considerando la salud, seguridad y bienestar público.\nPI2: Diseña e implementa esquemas de almacenamiento que satisfacen los requisitos de las necesidades identificadas en el contexto.', NULL, 3),
    ('contents', 'Contenidos Programáticos', E'1. Introducción a los sistemas de gestión de bases de datos (SGDB)\n   - Conceptos fundamentales\n   - Arquitectura de un SGDB\n   - Modelos de datos\n\n2. Modelo relacional\n   - Tablas, filas y columnas\n   - Claves primarias y foráneas\n   - Integridad referencial\n\n3. Lenguaje SQL\n   - DDL: CREATE, ALTER, DROP\n   - DML: INSERT, UPDATE, DELETE\n   - DQL: SELECT, JOIN, subconsultas\n   - Funciones de agregación\n\n4. Diseño de bases de datos\n   - Diagrama Entidad-Relación\n   - Normalización (1FN, 2FN, 3FN)\n   - Metodología de diseño', NULL, 4),
    ('teaching_strategies', 'Estrategias de Enseñanza', E'En cada sesión del curso se discutirán los conceptos teóricos de cada tema, que los estudiantes han preparado previamente, y se desarrollarán talleres individuales o en grupos con ejercicios de aplicación. La preparación de cada tema incluye la lectura del mismo, identificar las ideas más importantes y establecer los puntos que necesitan explicación por parte del docente.\n\nSe establecerán espacios de asesoría para resolver dudas y se hará seguimiento al desarrollo de los talleres y del proyecto. Se contará con un aula virtual en la plataforma institucional donde se publicará el material del curso y las fechas de las diferentes actividades.', NULL, 5),
    ('evaluation', 'Estrategia de Evaluación del Aprendizaje', NULL, tabla, 6),
    ('bibliography', 'Bibliografía', E'Requerida:\n- Elmasri, R. & Navathe, S. (2016). Fundamentals of Database Systems (7th ed.). Pearson. [005.1 E48f]\n- Silberschatz, A., Korth, H. & Sudarshan, S. (2019). Database System Concepts (7th ed.). McGraw-Hill. [005.1 S582]\n\nSugerida:\n- Date, C.J. (2003). An Introduction to Database Systems (8th ed.). Addison-Wesley.\n- Coronel, C. & Morris, S. (2019). Database Systems: Design, Implementation, and Management (13th ed.). Cengage Learning.', NULL, 7)
) AS t (section_type, title, content, evaluation, section_order)
UNION ALL
SELECT '50000000-0000-0000-0000-000000000002'::uuid, t.section_type, t.title, t.content, t.evaluation, t.section_order
FROM evaluacion, LATERAL (VALUES
    ('identification', 'Identificación de la Asignatura', E'Universidad Autónoma de Manizales\nFacultad de Ingeniería\nDepartamento de Ingeniería de Sistemas\n\nAsignatura: Ingeniería de Software\nCódigo: SIS301\nCréditos: 4\nSemestre: 6\nPrograma: Ingeniería de Software', NULL::jsonb, 1),
    ('justification', 'Justificación', 'La Ingeniería de Software es una disciplina que se encarga del desarrollo sistemático de software de alta calidad. Esta asignatura proporciona las herramientas metodológicas y técnicas necesarias para el ciclo de vida del desarrollo de software.', NULL, 2),
    ('performance', 'Desempeño Esperado', E'RESULTADOS DE APRENDIZAJE:\n\nSO2 – Capacidad de aplicar el diseño de ingeniería para producir soluciones que satisfagan necesidades específicas teniendo en cuenta la salud pública, la seguridad y el bienestar, así como factores globales, culturales, sociales, ambientales y económicos.\n\nINDICADOR DE DESEMPEÑO:\n\nPI1: Identifica necesidades, requerimientos y criterios específicos para abordar diferentes problemas de ingeniería considerando la salud, seguridad y bienestar público, así como factores globales, culturales, sociales, ambientales y económicos.\n\nPI2: Diseñar un sistema informático que satisfaga los requisitos correspondientes a las necesidades identificadas en el contexto en que surge.', NULL, 3),
    ('contents', 'Contenidos Programáticos', E'Gestión y proceso de desarrollo\n- Metodología Scrum\n- Valores y pilares del marco (Transparencia, inspección, adaptación)\n- Roles: Product Owner, Scrum Master, Equipo de Desarrollo\n- Artefactos: Product Backlog, Sprint Backlog, Incremento\n- Eventos: Sprint, Planning, Daily, Review, Retrospectiva\n- Historias de usuario, criterios de aceptación y definición "Done"\n- Métricas: burndown, velocidad, gestión visual del tablero\n\nPrincipios SOLID\n- Responsabilidad Única (SRP)\n- Abierto/Cerrado (OCP)\n- Sustitución de Liskov (LSP)\n- Segregación de Interfaces (ISP)\n- Inversión de Dependencias (DIP)\n- Detección de violaciones en código existente y su corrección\n\nDiseño evolutivo del software\n- Refactorización y Clean Code\n- Deuda técnica y code smells\n- Patrones de Diseño\n- Técnicas de estimación\n- Pruebas de código', NULL, 4),
    ('teaching_strategies', 'Estrategias de Enseñanza', E'En cada sesión del curso se discutirán los conceptos teóricos de cada tema, que los estudiantes han preparado previamente, y se desarrollarán talleres individuales o en grupos, con ejercicios de aplicación. La preparación de cada tema incluye la lectura del mismo, identificar las ideas más importantes y sobre todo establecer los puntos que necesitan explicación por parte del docente. Algunas lecturas y actividades serán en idioma inglés, considerando que gran parte de la producción en el tema es en dicho idioma.\n\nSe establecerán espacios para asesorías, donde se resolverán dudas relacionadas con los temas de la materia y se realizará seguimiento al desarrollo de los talleres y del proyecto. Se contará con un aula virtual –en la plataforma institucional UAMVirtual– donde se publicará material del curso (estándares, talleres, lecturas) y las fechas para las diferentes actividades.\n\nAdemás del estudio individual de cada tema, los estudiantes deberán desarrollar un proyecto de software en equipo, donde se apliquen los conceptos vistos en el curso, junto con temas de investigación. El proyecto se gestionará mediante sprints y se versionará en un repositorio remoto siguiendo el modelo de ramificación estudiado.', NULL, 5),
    ('evaluation', 'Estrategia de Evaluación del Aprendizaje', NULL, tabla, 6),
    ('bibliography', 'Bibliografía', E'Requerida:\n- PRESSMAN, Roger. Ingeniería del software: Un enfoque práctico, Séptima Edición. McGraw Hill Interamericana, 2010. [005.1 P737]\n- SOMMERVILLE, Ian. Software Engineering, Décima Edición. Pearson, 2015. [005.1 S688]\n\nSugerida:\n- FOWLER, Martin. Refactoring: Improving the Design of Existing Code, 2 ed. Addison-Wesley, 2018.\n- GAMMA, Erich et al. Design Patterns: Elements of Reusable Object-Oriented Software. Addison-Wesley Publishing Company, 1995. [005.12 G155]\n- MARTIN, Robert C. Clean Code: A Handbook of Agile Software Craftsmanship. Prentice Hall, 2008.\n- MARTIN, Robert C. Clean Architecture: A Craftsman''s Guide to Software Structure and Design. Prentice Hall, 2017.', NULL, 7)
) AS t (section_type, title, content, evaluation, section_order);

INSERT INTO syllabus_sections (syllabus_id, section_type, title, content, evaluation, section_order) VALUES
    ('50000000-0000-0000-0000-000000000003', 'identification', 'Identificación de la Asignatura', 'Algoritmos y Estructuras de Datos - SIS101', NULL, 1),
    ('50000000-0000-0000-0000-000000000003', 'contents', 'Contenidos', E'1. Introducción a algoritmos\n2. Estructuras de datos básicas\n3. Recursión\n4. Ordenamiento y búsqueda\n5. Grafos y árboles', NULL, 2),
    ('50000000-0000-0000-0000-000000000004', 'identification', 'Identificación de la Asignatura', E'Algoritmos y Estructuras de Datos - SIS101\nVersión borrador 2026', NULL, 1),
    ('50000000-0000-0000-0000-000000000005', 'identification', 'Identificación de la Asignatura', E'Universidad Autónoma de Manizales\nFacultad de Ingeniería\nPosgrado: Maestría en Inteligencia Artificial\n\nAsignatura: Aprendizaje Automático Avanzado\nCódigo: MIA501\nCréditos: 3\nSemestre: 1\nPrograma: Maestría en Inteligencia Artificial (Posgrado)', NULL, 1),
    ('50000000-0000-0000-0000-000000000005', 'justification', 'Justificación', 'El aprendizaje automático avanzado aporta al estudiante de maestría las herramientas teóricas y prácticas para diseñar, evaluar y desplegar modelos de aprendizaje supervisado y no supervisado sobre datos a gran escala, habilitándolo para liderar proyectos de inteligencia artificial en la industria y la academia.', NULL, 2),
    ('50000000-0000-0000-0000-000000000005', 'contents', 'Contenidos Programáticos', E'1. Fundamentos de aprendizaje estadístico\n   - Sesgo y varianza\n   - Regularización (L1, L2)\n   - Validación cruzada\n\n2. Modelos supervisados avanzados\n   - Bosques aleatorios y Gradient Boosting\n   - Máquinas de vectores de soporte\n   - Redes neuronales profundas\n\n3. Modelos no supervisados\n   - Clustering y reducción de dimensionalidad\n   - Detección de anomalías\n\n4. Despliegue e IA responsable\n   - MLOps y monitoreo de modelos\n   - Explicabilidad, ética y sesgo algorítmico', NULL, 3),
    ('50000000-0000-0000-0000-000000000005', 'evaluation', 'Estrategia de Evaluación del Aprendizaje', NULL,
     '[{"corte":1,"label":"PRIMER CORTE","porcentaje":30,"tipos":[{"tipo":"Talleres de modelado","porcentaje":30},{"tipo":"Parcial teórico-práctico","porcentaje":30},{"tipo":"Avance proyecto aplicado","porcentaje":40}]},{"corte":2,"label":"SEGUNDO CORTE","porcentaje":35,"tipos":[{"tipo":"Talleres de modelado","porcentaje":30},{"tipo":"Parcial teórico-práctico","porcentaje":30},{"tipo":"Avance proyecto aplicado","porcentaje":40}]},{"corte":3,"label":"TERCER CORTE","porcentaje":35,"tipos":[{"tipo":"Entrega final proyecto aplicado","porcentaje":100}]}]', 4),
    ('50000000-0000-0000-0000-000000000005', 'bibliography', 'Bibliografía', E'Requerida:\n- Bishop, C. (2006). Pattern Recognition and Machine Learning. Springer.\n- Geron, A. (2022). Hands-On Machine Learning with Scikit-Learn, Keras, and TensorFlow (3rd ed.). O''Reilly.\n\nSugerida:\n- Goodfellow, I., Bengio, Y. & Courville, A. (2016). Deep Learning. MIT Press.\n- Hastie, T., Tibshirani, R. & Friedman, J. (2009). The Elements of Statistical Learning (2nd ed.). Springer.', NULL, 5);

-- Historial (UC22): snapshot con la misma forma que genera el backend. La versión 1 de Base de
-- Datos I no tenía el módulo 4 ni la bibliografía de Coronel, para mostrar la comparación (UC14).
INSERT INTO syllabus_versions (syllabus_id, version_number, snapshot, change_description, modified_by, created_at)
SELECT s.id, v.version_number,
       jsonb_build_object(
           'title', s.title,
           'status', v.status,
           'headerTitle', s.header_title,
           'documentCode', s.document_code,
           'documentDate', s.document_date,
           'sections', (SELECT jsonb_agg(jsonb_build_object(
                                   'sectionType', sec.section_type,
                                   'title', sec.title,
                                   'content', CASE
                                       WHEN v.anterior AND sec.section_type = 'contents'
                                           THEN regexp_replace(sec.content, '\n\n4\. Diseño.*$', '')
                                       WHEN v.anterior AND sec.section_type = 'bibliography'
                                           THEN regexp_replace(sec.content, '\n- Coronel.*?Learning\.', '')
                                       ELSE sec.content END,
                                   'evaluation', sec.evaluation,
                                   'order', sec.section_order) ORDER BY sec.section_order)
                        FROM syllabus_sections sec WHERE sec.syllabus_id = s.id)),
       v.change_description, v.modified_by, v.created_at
FROM syllabus s
JOIN (VALUES
    ('50000000-0000-0000-0000-000000000001'::uuid, 1, 'ACTIVO', TRUE, 'Creación del syllabus', '40000000-0000-0000-0000-000000000002'::uuid, '2026-01-10T08:00:00Z'::timestamptz),
    ('50000000-0000-0000-0000-000000000001'::uuid, 2, 'ACTIVO', FALSE, 'Actualización de bibliografía y contenidos del módulo 4', '40000000-0000-0000-0000-000000000001'::uuid, '2026-02-15T10:30:00Z'::timestamptz),
    ('50000000-0000-0000-0000-000000000002'::uuid, 1, 'ACTIVO', FALSE, 'Creación del syllabus', '40000000-0000-0000-0000-000000000002'::uuid, '2026-01-12T08:00:00Z'::timestamptz),
    ('50000000-0000-0000-0000-000000000003'::uuid, 1, 'ACTIVO', FALSE, 'Creación del syllabus', '40000000-0000-0000-0000-000000000002'::uuid, '2025-08-10T08:00:00Z'::timestamptz),
    ('50000000-0000-0000-0000-000000000003'::uuid, 2, 'INACTIVO', FALSE, 'Syllabus desactivado', '40000000-0000-0000-0000-000000000002'::uuid, '2025-12-15T10:00:00Z'::timestamptz),
    ('50000000-0000-0000-0000-000000000004'::uuid, 1, 'BORRADOR', FALSE, 'Creación del syllabus', '40000000-0000-0000-0000-000000000001'::uuid, '2026-01-20T08:00:00Z'::timestamptz),
    ('50000000-0000-0000-0000-000000000005'::uuid, 1, 'ACTIVO', FALSE, 'Creación del syllabus', '40000000-0000-0000-0000-000000000006'::uuid, '2026-01-30T08:00:00Z'::timestamptz)
) AS v (syllabus_id, version_number, status, anterior, change_description, modified_by, created_at)
    ON v.syllabus_id = s.id;
