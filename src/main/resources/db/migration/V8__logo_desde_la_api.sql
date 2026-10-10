-- El logo se sirve siempre desde la API (GET /api/settings/logo): el subido en Configuración o, si no
-- hay, el institucional por defecto. Así el frontend no necesita su propia copia del logo.
UPDATE institutional_config
SET value = '/api/settings/logo', updated_at = now()
WHERE key = 'universityLogoUrl' AND value = '/images/uam_header_logo.png';
