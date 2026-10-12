-- Área de formación de cada asignatura dentro de un plan (la misma asignatura puede ser
-- complementaria en un programa y socio-humanística en otro) y orden dentro del semestre, como en
-- los planes de estudio publicados.
ALTER TABLE study_plan_subjects ADD COLUMN area VARCHAR(20) NOT NULL DEFAULT 'PROFESIONAL'
    CHECK (area IN ('BASICA', 'PROFESIONAL', 'SOCIOHUMANISTICA', 'COMPLEMENTARIA',
                    'FUNDAMENTACION', 'PROFUNDIZACION', 'INVESTIGACION', 'INSTITUCIONAL'));
ALTER TABLE study_plan_subjects ADD COLUMN position INTEGER NOT NULL DEFAULT 0;

-- Espacios del plan sin syllabus propio: cursos libres, electivas, énfasis y prácticas.
ALTER TABLE subjects ADD COLUMN requires_syllabus BOOLEAN NOT NULL DEFAULT TRUE;
