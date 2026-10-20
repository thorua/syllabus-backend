-- Imagen de la firma manuscrita (rúbrica) que el firmante sube o dibuja: se dibuja en el sello
-- visible de sus firmas digitales. Es un PNG ya normalizado (fondo transparente, recortado).
CREATE TABLE user_signature_images (
    user_id      UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    image        BYTEA       NOT NULL,
    uploaded_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
