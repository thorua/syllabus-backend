-- Esquema base del Sistema de Gestión de Syllabus (UAM).
-- Nombres de tabla y columna heredados del modelo anterior (schema.prisma).

CREATE TABLE departments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL UNIQUE,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Regla heredada: pregrado pertenece a un departamento, posgrado no.
CREATE TABLE programs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(200) NOT NULL,
    code           VARCHAR(20)  NOT NULL UNIQUE,
    level          VARCHAR(20)  NOT NULL CHECK (level IN ('PREGRADO', 'ESPECIALIZACION', 'MAESTRIA', 'DOCTORADO')),
    department_id  UUID REFERENCES departments (id),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT programs_level_department CHECK ((level = 'PREGRADO') = (department_id IS NOT NULL))
);

CREATE TABLE users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email          VARCHAR(150) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    name           VARCHAR(150) NOT NULL,
    gender         VARCHAR(10)  CHECK (gender IN ('MASCULINO', 'FEMENINO', 'OTRO')),
    phone          VARCHAR(30),
    student_code   VARCHAR(30),
    employee_code  VARCHAR(30),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id  UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role     VARCHAR(30) NOT NULL CHECK (role IN ('ESTUDIANTE', 'PROFESOR', 'COORDINADOR_PROGRAMA',
                                                  'COORDINADOR_DEPARTAMENTO', 'ADM_REGISTRO', 'ADMINISTRADOR')),
    PRIMARY KEY (user_id, role)
);

CREATE TABLE user_departments (
    user_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    department_id  UUID NOT NULL REFERENCES departments (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, department_id)
);

CREATE TABLE user_programs (
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    program_id  UUID NOT NULL REFERENCES programs (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, program_id)
);

-- UC4: allow=true otorga, allow=false restringe un permiso "resource:action".
CREATE TABLE user_permission_overrides (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    action      VARCHAR(30) NOT NULL,
    resource    VARCHAR(30) NOT NULL,
    allow       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_by  UUID        REFERENCES users (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, action, resource)
);

CREATE TABLE subjects (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(200) NOT NULL,
    code                 VARCHAR(20)  NOT NULL UNIQUE,
    credits              INTEGER      NOT NULL CHECK (credits > 0),
    semester             INTEGER      NOT NULL CHECK (semester > 0),
    program_id           UUID         NOT NULL REFERENCES programs (id),
    department_id        UUID         REFERENCES departments (id),
    professor_id         UUID         REFERENCES users (id),
    modality             VARCHAR(30)  NOT NULL DEFAULT 'Presencial',
    hours_presencial     INTEGER,
    hours_dirigido       INTEGER,
    hours_independiente  INTEGER,
    prerequisites        VARCHAR(300),
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE syllabus (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id       UUID         NOT NULL REFERENCES subjects (id),
    academic_period  VARCHAR(10)  NOT NULL,
    title            VARCHAR(250) NOT NULL,
    header_title     VARCHAR(200),
    document_code    VARCHAR(50),
    document_date    VARCHAR(20),
    status           VARCHAR(10)  NOT NULL DEFAULT 'BORRADOR' CHECK (status IN ('ACTIVO', 'INACTIVO', 'BORRADOR')),
    created_by       UUID         NOT NULL REFERENCES users (id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (subject_id, academic_period)
);

-- La tabla de evaluación vive en "evaluation" (JSONB); el resto de secciones en "content".
CREATE TABLE syllabus_sections (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    syllabus_id    UUID         NOT NULL REFERENCES syllabus (id) ON DELETE CASCADE,
    section_type   VARCHAR(30)  NOT NULL,
    title          VARCHAR(200) NOT NULL,
    content        TEXT,
    evaluation     JSONB,
    section_order  INTEGER      NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE syllabus_versions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    syllabus_id         UUID         NOT NULL REFERENCES syllabus (id),
    version_number      INTEGER      NOT NULL,
    snapshot            JSONB        NOT NULL,
    change_description  VARCHAR(500) NOT NULL,
    modified_by         UUID         NOT NULL REFERENCES users (id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (syllabus_id, version_number)
);

-- UC22: las versiones son inmutables.
CREATE FUNCTION syllabus_versions_inmutables() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Las versiones de syllabus son inmutables';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER syllabus_versions_sin_cambios
    BEFORE UPDATE OR DELETE ON syllabus_versions
    FOR EACH ROW EXECUTE FUNCTION syllabus_versions_inmutables();

CREATE TABLE institutional_config (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key          VARCHAR(50)  NOT NULL UNIQUE,
    value        TEXT         NOT NULL,
    description  VARCHAR(200),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        REFERENCES users (id),
    action      VARCHAR(30) NOT NULL,
    entity      VARCHAR(30) NOT NULL,
    entity_id   VARCHAR(50),
    details     JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX audit_logs_created_at_idx ON audit_logs (created_at DESC);
CREATE INDEX audit_logs_user_idx ON audit_logs (user_id);
CREATE INDEX audit_logs_entity_idx ON audit_logs (entity, entity_id);

-- UC2: se guarda el hash SHA-256 del token, nunca el token.
CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX subjects_program_idx ON subjects (program_id);
CREATE INDEX subjects_department_idx ON subjects (department_id);
CREATE INDEX subjects_professor_idx ON subjects (professor_id);
CREATE INDEX syllabus_sections_syllabus_idx ON syllabus_sections (syllabus_id);
