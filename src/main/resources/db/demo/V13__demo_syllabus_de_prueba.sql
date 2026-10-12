-- Demo: seis syllabus completos para probar el sistema con contenido realista (formato de texto
-- de las secciones: «- » viñeta, «  - » subviñeta, **negrita**, *cursiva*, MAYÚSCULAS = subtítulo).
-- Son ejemplos construidos para la demo, no syllabus oficiales de la UAM.

INSERT INTO subjects (id, name, code, credits, department_id, professor_id, modality, hours_presencial,
                      hours_dirigido, hours_independiente, prerequisites) VALUES
    ('30000000-0000-0000-0000-000000000017', 'Electricidad y Magnetismo', 'FIS200', 4,
     '10000000-0000-0000-0000-000000000002', NULL, 'Presencial', 4, 2, 6, 'Física Mecánica'),
    ('30000000-0000-0000-0000-000000000018', 'Programación Backend', 'SIS401', 4,
     '10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'Presencial', 4, 2, 6,
     'Base de Datos I'),
    ('30000000-0000-0000-0000-000000000019', 'Paradigmas de Lenguajes de Programación', 'SIS303', 3,
     '10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'Presencial', 3, 1, 5,
     'Algoritmos y Estructuras de Datos'),
    ('30000000-0000-0000-0000-000000000020', 'Filosofía de la Ciencia', 'HUM300', 2,
     '10000000-0000-0000-0000-000000000005', NULL, 'Híbrida', 2, 1, 3, 'Ninguno'),
    ('30000000-0000-0000-0000-000000000021', 'Métodos Numéricos', 'MAT300', 3,
     '10000000-0000-0000-0000-000000000002', NULL, 'Presencial', 3, 1, 5, 'Cálculo I, Álgebra Lineal'),
    ('30000000-0000-0000-0000-000000000022', 'Procesos de Investigación I', 'INV700', 2,
     '10000000-0000-0000-0000-000000000001', NULL, 'Presencial', 2, 1, 3, 'Ingeniería de Software');

INSERT INTO study_plan_subjects (plan_id, subject_id, semester)
SELECT sp.id, m.subject_id::uuid, m.semester
FROM study_plans sp
JOIN (VALUES
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000019', 3),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000017', 4),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000021', 4),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000018', 5),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000020', 5),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2026', '30000000-0000-0000-0000-000000000022', 7),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000019', 3),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000021', 4),
    ('20000000-0000-0000-0000-000000000001', 'Plan 2020', '30000000-0000-0000-0000-000000000018', 5),
    ('20000000-0000-0000-0000-000000000002', 'Plan 2023', '30000000-0000-0000-0000-000000000017', 4),
    ('20000000-0000-0000-0000-000000000002', 'Plan 2023', '30000000-0000-0000-0000-000000000021', 4),
    ('20000000-0000-0000-0000-000000000002', 'Plan 2023', '30000000-0000-0000-0000-000000000020', 5),
    ('20000000-0000-0000-0000-000000000003', 'Plan 2022', '30000000-0000-0000-0000-000000000017', 3),
    ('20000000-0000-0000-0000-000000000003', 'Plan 2022', '30000000-0000-0000-0000-000000000020', 5)
) AS m (program_id, plan, subject_id, semester)
    ON sp.program_id = m.program_id::uuid AND sp.name = m.plan;

INSERT INTO syllabus (id, subject_id, academic_period, title, status, created_by, created_at, updated_at) VALUES
    ('50000000-0000-0000-0000-000000000009', '30000000-0000-0000-0000-000000000017', '2026-01',
     'Syllabus Electricidad y Magnetismo - 2026-01', 'ACTIVO', '40000000-0000-0000-0000-000000000007',
     '2026-01-12T08:00:00Z', '2026-01-12T08:00:00Z'),
    ('50000000-0000-0000-0000-000000000010', '30000000-0000-0000-0000-000000000018', '2026-01',
     'Syllabus Programación Backend - 2026-01', 'ACTIVO', '40000000-0000-0000-0000-000000000001',
     '2026-01-13T08:00:00Z', '2026-01-13T08:00:00Z'),
    ('50000000-0000-0000-0000-000000000011', '30000000-0000-0000-0000-000000000019', '2026-01',
     'Syllabus Paradigmas de Lenguajes de Programación - 2026-01', 'ACTIVO',
     '40000000-0000-0000-0000-000000000001', '2026-01-13T09:00:00Z', '2026-01-13T09:00:00Z'),
    ('50000000-0000-0000-0000-000000000012', '30000000-0000-0000-0000-000000000020', '2026-01',
     'Syllabus Filosofía de la Ciencia - 2026-01', 'ACTIVO', '40000000-0000-0000-0000-000000000003',
     '2026-01-14T08:00:00Z', '2026-01-14T08:00:00Z'),
    ('50000000-0000-0000-0000-000000000013', '30000000-0000-0000-0000-000000000021', '2026-01',
     'Syllabus Métodos Numéricos - 2026-01', 'ACTIVO', '40000000-0000-0000-0000-000000000007',
     '2026-01-14T09:00:00Z', '2026-01-14T09:00:00Z'),
    ('50000000-0000-0000-0000-000000000014', '30000000-0000-0000-0000-000000000022', '2026-01',
     'Syllabus Procesos de Investigación I - 2026-01', 'ACTIVO', '40000000-0000-0000-0000-000000000002',
     '2026-01-15T08:00:00Z', '2026-01-15T08:00:00Z');

INSERT INTO syllabus_sections (syllabus_id, section_type, title, content, evaluation, section_order)
SELECT t.syllabus_id::uuid, t.section_type, t.title, t.content, t.evaluation::jsonb, t.section_order
FROM (VALUES
    -- ---------------------------------------------------------------- Electricidad y Magnetismo
    ('50000000-0000-0000-0000-000000000009', 'identification', 'Identificación', '', NULL, 1),
    ('50000000-0000-0000-0000-000000000009', 'justification', 'Justificación',
     E'La electricidad y el magnetismo explican el funcionamiento de los circuitos, los sensores, las máquinas eléctricas y las comunicaciones, y son la base física de la electrónica y de los dispositivos biomédicos. El curso desarrolla en el estudiante la capacidad de modelar fenómenos electromagnéticos con las leyes de Coulomb, Gauss, Ampère y Faraday, y de comprobar esos modelos en el laboratorio.\n\nLo ofrece el Departamento de Matemáticas a los programas de ingeniería, con los mismos resultados de aprendizaje para todos.', NULL, 2),
    ('50000000-0000-0000-0000-000000000009', 'performance', 'Desempeño Esperado',
     E'RESULTADOS DE APRENDIZAJE\n- Calcula campos y potenciales eléctricos de distribuciones de carga discretas y continuas.\n- Analiza circuitos de corriente directa y circuitos RC con las leyes de Ohm y Kirchhoff.\n- Determina campos magnéticos producidos por corrientes y fuerzas sobre cargas y conductores.\n- Explica la inducción electromagnética y sus aplicaciones en generadores y transformadores.\n- Diseña y reporta experimentos de laboratorio con análisis de incertidumbre.', NULL, 3),
    ('50000000-0000-0000-0000-000000000009', 'contents', 'Contenidos',
     E'UNIDAD 1. ELECTROSTÁTICA\n- Carga eléctrica y ley de Coulomb\n- Campo eléctrico y líneas de campo\n- Ley de Gauss\n  - Simetría esférica, cilíndrica y plana\n- Potencial eléctrico y energía potencial\n\nUNIDAD 2. CAPACITANCIA Y CORRIENTE\n- Capacitores y dieléctricos\n- Corriente, resistencia y ley de Ohm\n- Circuitos de corriente directa\n  - Leyes de Kirchhoff\n  - Circuitos RC\n\nUNIDAD 3. MAGNETISMO\n- Campo magnético y fuerza de Lorentz\n- Ley de Biot-Savart\n- Ley de Ampère\n\nUNIDAD 4. INDUCCIÓN ELECTROMAGNÉTICA\n- Ley de Faraday y ley de Lenz\n- Inductancia y circuitos RL\n- Introducción a las ecuaciones de Maxwell', NULL, 4),
    ('50000000-0000-0000-0000-000000000009', 'teaching_strategies', 'Estrategias de Enseñanza',
     E'Clases teórico-prácticas en las que el docente presenta cada ley con demostraciones y los estudiantes resuelven problemas en grupo. Cada dos semanas hay una práctica de laboratorio con un informe escrito en formato de artículo corto.\n\nSe usan simulaciones interactivas (PhET) para visualizar campos y se publican en el aula virtual las guías de laboratorio y los talleres.', NULL, 5),
    ('50000000-0000-0000-0000-000000000009', 'evaluation', 'Estrategia de Evaluación del Aprendizaje', NULL,
     '[{"corte":1,"label":"I","porcentaje":30,"tipos":[{"tipo":"Parcial","porcentaje":50},{"tipo":"Informes de laboratorio","porcentaje":30},{"tipo":"Talleres","porcentaje":20}]},{"corte":2,"label":"II","porcentaje":35,"tipos":[{"tipo":"Parcial","porcentaje":50},{"tipo":"Informes de laboratorio","porcentaje":30},{"tipo":"Talleres","porcentaje":20}]},{"corte":3,"label":"III","porcentaje":35,"tipos":[{"tipo":"Examen final","porcentaje":60},{"tipo":"Proyecto experimental","porcentaje":40}]}]', 6),
    ('50000000-0000-0000-0000-000000000009', 'bibliography', 'Bibliografía',
     E'Requerida:\nSerway, R. A. y Jewett, J. W. (2019). *Física para ciencias e ingeniería*, vol. 2 (10.a ed.). Cengage Learning.\nYoung, H. D. y Freedman, R. A. (2018). *Física universitaria con física moderna*, vol. 2 (14.a ed.). Pearson.\nSugerida:\nPurcell, E. M. y Morin, D. J. (2013). *Electricity and Magnetism* (3rd ed.). Cambridge University Press.', NULL, 7),

    -- ---------------------------------------------------------------- Programación Backend
    ('50000000-0000-0000-0000-000000000010', 'identification', 'Identificación', '', NULL, 1),
    ('50000000-0000-0000-0000-000000000010', 'justification', 'Justificación',
     E'Las aplicaciones actuales dependen de servicios en el servidor que guardan los datos, aplican las reglas del negocio y los exponen de forma segura a la web y a los dispositivos móviles. Este curso forma al estudiante en el diseño y la construcción de esos servicios: APIs REST, persistencia, seguridad, pruebas automatizadas y despliegue en contenedores, con las prácticas que exige la industria del software.', NULL, 2),
    ('50000000-0000-0000-0000-000000000010', 'performance', 'Desempeño Esperado',
     E'RESULTADOS DE APRENDIZAJE\n- Diseña APIs REST coherentes, documentadas y versionadas.\n- Implementa la persistencia de datos con un ORM y migraciones de esquema.\n- Protege los servicios con autenticación basada en tokens y autorización por roles.\n- Escribe pruebas unitarias y de integración que se ejecutan en integración continua.\n- Despliega una aplicación en contenedores con su configuración por variables de entorno.', NULL, 3),
    ('50000000-0000-0000-0000-000000000010', 'contents', 'Contenidos',
     E'UNIDAD 1. ARQUITECTURA DEL BACKEND\n- Cliente-servidor, HTTP y REST\n- Arquitectura por capas y por funcionalidad\n- Inyección de dependencias\n\nUNIDAD 2. SERVICIOS REST CON SPRING BOOT\n- Controladores, DTO y validación\n- Manejo de errores y códigos de estado\n- Documentación con OpenAPI\n\nUNIDAD 3. PERSISTENCIA\n- JPA y Hibernate\n  - Entidades, relaciones y consultas\n  - Transacciones\n- Migraciones con Flyway\n\nUNIDAD 4. SEGURIDAD\n- Autenticación con JWT\n- Autorización por roles y permisos\n- OWASP Top 10 para APIs\n\nUNIDAD 5. CALIDAD Y DESPLIEGUE\n- Pruebas con JUnit y Testcontainers\n- Docker y Docker Compose\n- Integración continua', NULL, 4),
    ('50000000-0000-0000-0000-000000000010', 'teaching_strategies', 'Estrategias de Enseñanza',
     E'Aprendizaje basado en proyectos: desde la segunda semana los equipos construyen un servicio real por entregas incrementales, con revisión de código entre pares y retroalimentación del docente en cada entrega.\n\nLas sesiones combinan explicación corta, programación en vivo y laboratorio guiado. El código se gestiona en un repositorio Git con integración continua.', NULL, 5),
    ('50000000-0000-0000-0000-000000000010', 'evaluation', 'Estrategia de Evaluación del Aprendizaje', NULL,
     '[{"corte":1,"label":"I","porcentaje":30,"tipos":[{"tipo":"Laboratorios","porcentaje":40},{"tipo":"Primera entrega del proyecto","porcentaje":60}]},{"corte":2,"label":"II","porcentaje":35,"tipos":[{"tipo":"Laboratorios","porcentaje":30},{"tipo":"Segunda entrega del proyecto","porcentaje":50},{"tipo":"Revisión de código entre pares","porcentaje":20}]},{"corte":3,"label":"III","porcentaje":35,"tipos":[{"tipo":"Proyecto final desplegado","porcentaje":70},{"tipo":"Sustentación","porcentaje":30}]}]', 6),
    ('50000000-0000-0000-0000-000000000010', 'bibliography', 'Bibliografía',
     E'Requerida:\nWalls, C. (2022). *Spring in Action* (6th ed.). Manning.\nFowler, M. (2002). *Patterns of Enterprise Application Architecture*. Addison-Wesley.\nSugerida:\nKleppmann, M. (2017). *Designing Data-Intensive Applications*. O''Reilly Media.\nRichardson, C. (2018). *Microservices Patterns*. Manning.', NULL, 7),

    -- ---------------------------------------------------------------- Paradigmas de Lenguajes
    ('50000000-0000-0000-0000-000000000011', 'identification', 'Identificación', '', NULL, 1),
    ('50000000-0000-0000-0000-000000000011', 'justification', 'Justificación',
     E'Un ingeniero que conoce varios paradigmas elige el lenguaje y el estilo adecuados para cada problema y aprende nuevos lenguajes con rapidez. El curso compara los paradigmas imperativo, orientado a objetos, funcional y lógico, y estudia los conceptos con los que se diseñan los lenguajes: sintaxis, semántica, tipos, alcance y manejo de memoria.', NULL, 2),
    ('50000000-0000-0000-0000-000000000011', 'performance', 'Desempeño Esperado',
     E'RESULTADOS DE APRENDIZAJE\n- Distingue los paradigmas de programación y sus mecanismos de abstracción.\n- Resuelve un mismo problema en lenguajes de distintos paradigmas y compara las soluciones.\n- Explica sistemas de tipos, alcance de variables y estrategias de evaluación.\n- Implementa un intérprete sencillo para un lenguaje de expresiones.', NULL, 3),
    ('50000000-0000-0000-0000-000000000011', 'contents', 'Contenidos',
     E'UNIDAD 1. FUNDAMENTOS DE LOS LENGUAJES\n- Sintaxis y gramáticas\n- Semántica operacional\n- Compilación e interpretación\n\nUNIDAD 2. PARADIGMA IMPERATIVO Y ORIENTADO A OBJETOS\n- Variables, alcance y tiempo de vida\n- Encapsulamiento, herencia y polimorfismo\n\nUNIDAD 3. PARADIGMA FUNCIONAL\n- Funciones de orden superior y clausuras\n- Inmutabilidad y recursión\n- Evaluación perezosa\n\nUNIDAD 4. PARADIGMA LÓGICO Y CONCURRENCIA\n- Programación lógica con Prolog\n- Modelos de concurrencia\n  - Hilos y memoria compartida\n  - Paso de mensajes', NULL, 4),
    ('50000000-0000-0000-0000-000000000011', 'teaching_strategies', 'Estrategias de Enseñanza',
     E'Cada paradigma se trabaja con un lenguaje representativo (Java, Haskell o Racket, y Prolog). Las clases alternan exposición de conceptos con ejercicios cortos en el computador, y cada unidad cierra con un taller que resuelve el mismo problema en dos paradigmas para comparar.', NULL, 5),
    ('50000000-0000-0000-0000-000000000011', 'evaluation', 'Estrategia de Evaluación del Aprendizaje', NULL,
     '[{"corte":1,"label":"I","porcentaje":30,"tipos":[{"tipo":"Quiz","porcentaje":30},{"tipo":"Talleres","porcentaje":30},{"tipo":"Parcial","porcentaje":40}]},{"corte":2,"label":"II","porcentaje":35,"tipos":[{"tipo":"Quiz","porcentaje":30},{"tipo":"Talleres","porcentaje":30},{"tipo":"Parcial","porcentaje":40}]},{"corte":3,"label":"III","porcentaje":35,"tipos":[{"tipo":"Intérprete (proyecto)","porcentaje":60},{"tipo":"Examen final","porcentaje":40}]}]', 6),
    ('50000000-0000-0000-0000-000000000011', 'bibliography', 'Bibliografía',
     E'Requerida:\nSebesta, R. W. (2019). *Concepts of Programming Languages* (12th ed.). Pearson.\nAbelson, H. y Sussman, G. J. (1996). *Structure and Interpretation of Computer Programs* (2nd ed.). MIT Press.\nSugerida:\nVan Roy, P. y Haridi, S. (2004). *Concepts, Techniques, and Models of Computer Programming*. MIT Press.\nScott, M. L. (2015). *Programming Language Pragmatics* (4th ed.). Morgan Kaufmann.', NULL, 7),

    -- ---------------------------------------------------------------- Filosofía de la Ciencia
    ('50000000-0000-0000-0000-000000000012', 'identification', 'Identificación', '', NULL, 1),
    ('50000000-0000-0000-0000-000000000012', 'justification', 'Justificación',
     E'La ingeniería y las ciencias de la salud se apoyan en conocimiento científico cuya validez conviene examinar. Este curso invita a reflexionar sobre qué distingue a la ciencia de otras formas de conocimiento, cómo cambian las teorías y qué responsabilidad ética tiene quien produce y aplica conocimiento técnico. Lo ofrece el Departamento de Educación a varios programas como parte de la formación humanística.', NULL, 2),
    ('50000000-0000-0000-0000-000000000012', 'performance', 'Desempeño Esperado',
     E'RESULTADOS DE APRENDIZAJE\n- Explica los principales problemas de la filosofía de la ciencia: demarcación, inducción y cambio teórico.\n- Contrasta las posturas de Popper, Kuhn y Lakatos sobre el progreso científico.\n- Argumenta por escrito una posición sobre un caso de ciencia y tecnología en la sociedad.', NULL, 3),
    ('50000000-0000-0000-0000-000000000012', 'contents', 'Contenidos',
     E'- El conocimiento científico y el sentido común\n- El problema de la inducción\n- Falsacionismo: Karl Popper\n- Paradigmas y revoluciones científicas: Thomas Kuhn\n- Programas de investigación: Imre Lakatos\n- Realismo y antirrealismo científico\n- Ciencia, tecnología y sociedad\n  - Ética de la investigación\n  - Responsabilidad social del ingeniero', NULL, 4),
    ('50000000-0000-0000-0000-000000000012', 'teaching_strategies', 'Estrategias de Enseñanza',
     E'Seminario: antes de cada sesión los estudiantes leen un texto corto y preparan preguntas; en clase se discute en pequeños grupos y luego en plenaria. La modalidad es híbrida: una de cada tres sesiones es virtual y se desarrolla con foros en el aula virtual.', NULL, 5),
    ('50000000-0000-0000-0000-000000000012', 'evaluation', 'Estrategia de Evaluación del Aprendizaje', NULL,
     '[{"corte":1,"label":"I","porcentaje":30,"tipos":[{"tipo":"Participación en seminario","porcentaje":40},{"tipo":"Reseña crítica","porcentaje":60}]},{"corte":2,"label":"II","porcentaje":35,"tipos":[{"tipo":"Participación en foros","porcentaje":40},{"tipo":"Reseña crítica","porcentaje":60}]},{"corte":3,"label":"III","porcentaje":35,"tipos":[{"tipo":"Ensayo argumentativo","porcentaje":70},{"tipo":"Exposición","porcentaje":30}]}]', 6),
    ('50000000-0000-0000-0000-000000000012', 'bibliography', 'Bibliografía',
     E'Requerida:\nChalmers, A. F. (2000). *¿Qué es esa cosa llamada ciencia?* (3.a ed.). Siglo XXI.\nKuhn, T. S. (2013). *La estructura de las revoluciones científicas* (4.a ed.). Fondo de Cultura Económica.\nSugerida:\nPopper, K. R. (1962). *La lógica de la investigación científica*. Tecnos.\nLakatos, I. (1983). *La metodología de los programas de investigación científica*. Alianza.\nBunge, M. (2000). *La investigación científica: su estrategia y su filosofía*. Siglo XXI.', NULL, 7),

    -- ---------------------------------------------------------------- Métodos Numéricos
    ('50000000-0000-0000-0000-000000000013', 'identification', 'Identificación', '', NULL, 1),
    ('50000000-0000-0000-0000-000000000013', 'justification', 'Justificación',
     E'Muchos problemas de ingeniería no tienen solución analítica o la tienen demasiado costosa: ecuaciones no lineales, grandes sistemas de ecuaciones, integrales y ecuaciones diferenciales de modelos reales. Los métodos numéricos permiten aproximar esas soluciones en el computador controlando el error. El curso combina el fundamento matemático de cada método con su implementación en Python.', NULL, 2),
    ('50000000-0000-0000-0000-000000000013', 'performance', 'Desempeño Esperado',
     E'RESULTADOS DE APRENDIZAJE\n- Estima y controla los errores de redondeo y de truncamiento de un cálculo numérico.\n- Aplica métodos de búsqueda de raíces y de solución de sistemas de ecuaciones lineales.\n- Aproxima funciones, derivadas e integrales con interpolación y cuadratura.\n- Resuelve ecuaciones diferenciales ordinarias con métodos de un paso.\n- Implementa y compara métodos en Python según su exactitud y su costo.', NULL, 3),
    ('50000000-0000-0000-0000-000000000013', 'contents', 'Contenidos',
     E'UNIDAD 1. ERRORES\n- Aritmética de punto flotante\n- Error absoluto, relativo y de truncamiento\n\nUNIDAD 2. ECUACIONES NO LINEALES\n- Bisección y falsa posición\n- Newton-Raphson y secante\n- Convergencia de los métodos\n\nUNIDAD 3. SISTEMAS DE ECUACIONES LINEALES\n- Eliminación de Gauss y factorización LU\n- Métodos iterativos\n  - Jacobi\n  - Gauss-Seidel\n\nUNIDAD 4. INTERPOLACIÓN E INTEGRACIÓN\n- Polinomios de Lagrange y de Newton\n- Trazadores cúbicos\n- Regla del trapecio y reglas de Simpson\n\nUNIDAD 5. ECUACIONES DIFERENCIALES ORDINARIAS\n- Método de Euler\n- Métodos de Runge-Kutta', NULL, 4),
    ('50000000-0000-0000-0000-000000000013', 'teaching_strategies', 'Estrategias de Enseñanza',
     E'Clase magistral para el desarrollo de cada método y laboratorio de cómputo para implementarlo en cuadernos de Python (NumPy y Matplotlib). Los estudiantes resuelven en parejas problemas tomados de su programa: circuitos, estructuras, señales o modelos biológicos.', NULL, 5),
    ('50000000-0000-0000-0000-000000000013', 'evaluation', 'Estrategia de Evaluación del Aprendizaje', NULL,
     '[{"corte":1,"label":"I","porcentaje":30,"tipos":[{"tipo":"Laboratorios en Python","porcentaje":40},{"tipo":"Parcial","porcentaje":60}]},{"corte":2,"label":"II","porcentaje":35,"tipos":[{"tipo":"Laboratorios en Python","porcentaje":40},{"tipo":"Parcial","porcentaje":60}]},{"corte":3,"label":"III","porcentaje":35,"tipos":[{"tipo":"Proyecto de aplicación","porcentaje":50},{"tipo":"Examen final","porcentaje":50}]}]', 6),
    ('50000000-0000-0000-0000-000000000013', 'bibliography', 'Bibliografía',
     E'Requerida:\nChapra, S. C. y Canale, R. P. (2015). *Métodos numéricos para ingenieros* (7.a ed.). McGraw-Hill.\nBurden, R. L., Faires, J. D. y Burden, A. M. (2017). *Análisis numérico* (10.a ed.). Cengage Learning.\nSugerida:\nSauer, T. (2018). *Numerical Analysis* (3rd ed.). Pearson.', NULL, 7),

    -- ---------------------------------------------------------------- Procesos de Investigación I
    ('50000000-0000-0000-0000-000000000014', 'identification', 'Identificación', '', NULL, 1),
    ('50000000-0000-0000-0000-000000000014', 'justification', 'Justificación',
     E'El trabajo de grado exige plantear un problema relevante, revisar con rigor lo que ya se sabe de él y proponer un camino metodológico para resolverlo. Este curso acompaña al estudiante en la formulación de su anteproyecto: desde la idea inicial hasta un documento con problema, objetivos, estado del arte y metodología, listo para evaluación. Continúa en Procesos de Investigación II con el desarrollo del proyecto.', NULL, 2),
    ('50000000-0000-0000-0000-000000000014', 'performance', 'Desempeño Esperado',
     E'RESULTADOS DE APRENDIZAJE\n- Formula un problema de investigación o de desarrollo tecnológico con su justificación.\n- Redacta objetivos general y específicos verificables.\n- Construye un estado del arte con una revisión sistemática de literatura.\n- Selecciona y justifica una metodología y un plan de trabajo.\n- Presenta el anteproyecto por escrito con normas de citación APA.', NULL, 3),
    ('50000000-0000-0000-0000-000000000014', 'contents', 'Contenidos',
     E'- La investigación en ingeniería: tipos y enfoques\n- Del tema al problema de investigación\n- Pregunta, hipótesis y objetivos\n- Revisión sistemática de literatura\n  - Bases de datos científicas\n  - Protocolo de búsqueda y selección\n- Marco teórico y estado del arte\n- Diseño metodológico\n  - Enfoques cuantitativo, cualitativo y mixto\n  - Metodologías de desarrollo de software en proyectos de grado\n- Cronograma, presupuesto y riesgos\n- Escritura académica y normas APA', NULL, 4),
    ('50000000-0000-0000-0000-000000000014', 'teaching_strategies', 'Estrategias de Enseñanza',
     E'Taller de escritura con asesoría: cada semana el estudiante entrega un avance de su anteproyecto y recibe retroalimentación escrita del docente y de un compañero. Se realizan sesiones prácticas en las bases de datos de la biblioteca y una presentación pública del anteproyecto al final del semestre.', NULL, 5),
    ('50000000-0000-0000-0000-000000000014', 'evaluation', 'Estrategia de Evaluación del Aprendizaje', NULL,
     '[{"corte":1,"label":"I","porcentaje":30,"tipos":[{"tipo":"Planteamiento del problema y objetivos","porcentaje":100}]},{"corte":2,"label":"II","porcentaje":35,"tipos":[{"tipo":"Revisión sistemática y estado del arte","porcentaje":70},{"tipo":"Retroalimentación entre pares","porcentaje":30}]},{"corte":3,"label":"III","porcentaje":35,"tipos":[{"tipo":"Documento de anteproyecto","porcentaje":60},{"tipo":"Sustentación","porcentaje":40}]}]', 6),
    ('50000000-0000-0000-0000-000000000014', 'bibliography', 'Bibliografía',
     E'Requerida:\nHernández-Sampieri, R. y Mendoza, C. P. (2018). *Metodología de la investigación: las rutas cuantitativa, cualitativa y mixta*. McGraw-Hill.\nKitchenham, B. y Charters, S. (2007). *Guidelines for Performing Systematic Literature Reviews in Software Engineering* (EBSE Technical Report). Keele University.\nSugerida:\nWohlin, C., Runeson, P., Höst, M., Ohlsson, M. C., Regnell, B. y Wesslén, A. (2012). *Experimentation in Software Engineering*. Springer.\nAmerican Psychological Association (2020). *Publication Manual of the American Psychological Association* (7th ed.).', NULL, 7)
) AS t (syllabus_id, section_type, title, content, evaluation, section_order);

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
WHERE s.id BETWEEN '50000000-0000-0000-0000-000000000009' AND '50000000-0000-0000-0000-000000000014';
