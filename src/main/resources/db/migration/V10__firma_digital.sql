-- Aprobación con firma digital: un syllabus vigente queda verificado cuando lo firman, sobre la
-- misma versión, la coordinación del departamento (en posgrado, la del programa) y luego Registro
-- Académico. Cada firma guarda el PDF firmado hasta ese paso; editar el syllabus crea una versión
-- nueva sin firmas.
CREATE TABLE syllabus_signatures (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    syllabus_id          UUID         NOT NULL REFERENCES syllabus (id),
    version_number       INTEGER      NOT NULL,
    step                 VARCHAR(20)  NOT NULL CHECK (step IN ('DEPARTAMENTO', 'REGISTRO')),
    signer_id            UUID         NOT NULL REFERENCES users (id),
    signer_name          VARCHAR(150) NOT NULL,
    certificate_subject  VARCHAR(500) NOT NULL,
    certificate_issuer   VARCHAR(500) NOT NULL,
    certificate_serial   VARCHAR(100) NOT NULL,
    document             BYTEA        NOT NULL,
    signed_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (syllabus_id, version_number, step)
);

CREATE INDEX syllabus_signatures_syllabus_idx ON syllabus_signatures (syllabus_id, version_number);

-- Igual que las versiones (UC22), una firma no se modifica ni se borra.
CREATE FUNCTION syllabus_signatures_inmutables() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Las firmas de syllabus son inmutables';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER syllabus_signatures_sin_cambios
    BEFORE UPDATE OR DELETE ON syllabus_signatures
    FOR EACH ROW EXECUTE FUNCTION syllabus_signatures_inmutables();

-- Certificado de firma (PKCS#12) que el firmante decide dejar registrado para no buscarlo cada
-- vez. El archivo sigue protegido por su contraseña, que nunca se guarda.
CREATE TABLE user_certificates (
    user_id              UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    file                 BYTEA        NOT NULL,
    certificate_subject  VARCHAR(500) NOT NULL,
    certificate_issuer   VARCHAR(500) NOT NULL,
    certificate_serial   VARCHAR(100) NOT NULL,
    not_after            TIMESTAMPTZ  NOT NULL,
    uploaded_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Límite de caracteres por sección del syllabus (2000 por defecto).
INSERT INTO institutional_config (key, value, description) VALUES
    ('maxSectionLength', '2000', 'Máximo de caracteres por sección del syllabus');
