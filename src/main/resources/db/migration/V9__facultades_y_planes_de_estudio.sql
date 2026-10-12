-- Estructura académica de la UAM: facultad → departamento → programa. Cada asignatura pertenece
-- al departamento que la ofrece (en posgrado, a su programa) y entra en la malla de uno o varios
-- programas a través de sus planes de estudio. Así, un departamento sin programas propios (por
-- ejemplo, Matemáticas) alimenta con sus asignaturas los planes de otros programas.

CREATE TABLE faculties (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL UNIQUE,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO faculties (name, code) VALUES
    ('Facultad de Ingeniería', 'FING'),
    ('Facultad de Salud', 'FSAL'),
    ('Facultad de Estudios Sociales y Empresariales', 'FESE');

-- El departamento pertenece a una facultad y su coordinación tiene un correo, que es el que
-- aparece en los syllabus de sus asignaturas.
ALTER TABLE departments ADD COLUMN faculty_id UUID REFERENCES faculties (id);
ALTER TABLE departments ADD COLUMN coordination_email VARCHAR(150);
UPDATE departments SET faculty_id = (SELECT id FROM faculties WHERE code = 'FING');
ALTER TABLE departments ALTER COLUMN faculty_id SET NOT NULL;

-- El pregrado llega a su facultad por el departamento; el posgrado no tiene departamento y
-- pertenece directamente a una facultad. El correo de coordinación queda solo para posgrado.
ALTER TABLE programs ADD COLUMN faculty_id UUID REFERENCES faculties (id);
UPDATE programs SET faculty_id = (SELECT id FROM faculties WHERE code = 'FING') WHERE level <> 'PREGRADO';
UPDATE departments d SET coordination_email = (
    SELECT min(p.coordination_email) FROM programs p WHERE p.department_id = d.id);
UPDATE programs SET coordination_email = NULL WHERE level = 'PREGRADO';
ALTER TABLE programs ADD CONSTRAINT programs_level_faculty CHECK ((level = 'PREGRADO') = (faculty_id IS NULL));

-- Planes de estudio (malla curricular) de cada programa, con el semestre de cada asignatura.
CREATE TABLE study_plans (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id  UUID         NOT NULL REFERENCES programs (id),
    name        VARCHAR(100) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (program_id, name)
);

CREATE TABLE study_plan_subjects (
    plan_id     UUID    NOT NULL REFERENCES study_plans (id) ON DELETE CASCADE,
    subject_id  UUID    NOT NULL REFERENCES subjects (id),
    semester    INTEGER NOT NULL CHECK (semester BETWEEN 1 AND 12),
    PRIMARY KEY (plan_id, subject_id)
);

CREATE INDEX study_plan_subjects_subject_idx ON study_plan_subjects (subject_id);

-- Las asignaturas existentes pasan al plan vigente de su programa, en su semestre.
INSERT INTO study_plans (program_id, name)
SELECT DISTINCT program_id, 'Plan vigente' FROM subjects;

INSERT INTO study_plan_subjects (plan_id, subject_id, semester)
SELECT sp.id, s.id, s.semester FROM subjects s JOIN study_plans sp ON sp.program_id = s.program_id;

-- Dueño de la asignatura: el departamento (pregrado) o el programa de posgrado, nunca ambos.
UPDATE subjects s SET department_id = p.department_id
FROM programs p WHERE p.id = s.program_id AND p.level = 'PREGRADO' AND s.department_id IS NULL;
ALTER TABLE subjects ALTER COLUMN program_id DROP NOT NULL;
UPDATE subjects s SET program_id = NULL
FROM programs p WHERE p.id = s.program_id AND p.level = 'PREGRADO';
ALTER TABLE subjects DROP COLUMN semester;
ALTER TABLE subjects ADD CONSTRAINT subjects_owner CHECK ((department_id IS NULL) <> (program_id IS NULL));
