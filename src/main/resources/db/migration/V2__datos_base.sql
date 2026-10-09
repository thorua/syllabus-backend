-- Configuración institucional por defecto (UC18–UC20).
INSERT INTO institutional_config (key, value, description) VALUES
    ('universityName',    'Universidad Autónoma de Manizales', 'Nombre de la institución'),
    ('universityLogoUrl', '/images/uam_header_logo.png', 'Logo del encabezado del syllabus'),
    ('universityAddress', 'Calle 10 No. 20-20, Manizales, Caldas, Colombia', 'Dirección'),
    ('universityPhone',   '+57 606 8860060', 'Teléfono'),
    ('universityEmail',   'info@um.edu.co', 'Correo institucional'),
    ('facultyName',       'Facultad de Ingeniería', 'Facultad'),
    ('departmentName',    'Departamento de Ingeniería de Sistemas', 'Departamento'),
    ('syllabusHeader',    'SYLLABUS DE LA ASIGNATURA', 'Título del encabezado'),
    ('documentCode',      'GFR-FOR-005', 'Código del formato Almera'),
    ('documentVersion',   '6', 'Versión del formato Almera'),
    ('documentDate',      '01/09/2026', 'Fecha del formato Almera'),
    ('syllabusSubtitle',  'Acreditación Institucional de Alta Calidad', 'Subtítulo'),
    ('footerText',        'Documento generado por el Sistema de Gestión de Syllabus', 'Pie de página'),
    ('evaluationTemplate',
     '[{"corte":1,"label":"PRIMER CORTE","porcentaje":30,"tipos":[{"tipo":"Quiz","porcentaje":30},{"tipo":"Talleres","porcentaje":30},{"tipo":"Avance proyecto","porcentaje":40}]},{"corte":2,"label":"SEGUNDO CORTE","porcentaje":35,"tipos":[{"tipo":"Quiz","porcentaje":30},{"tipo":"Talleres","porcentaje":30},{"tipo":"Avance proyecto","porcentaje":40}]},{"corte":3,"label":"TERCER CORTE","porcentaje":35,"tipos":[{"tipo":"Quiz","porcentaje":40},{"tipo":"Entrega final proyecto","porcentaje":60}]}]',
     'Estrategia de evaluación: cortes, tipos y porcentajes');
