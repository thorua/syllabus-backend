-- UC20: el formato institucional GFR-FOR-005 rotula los cortes con números romanos (I, II, III).
-- Solo cambia los rótulos por defecto; una plantilla ya personalizada por la institución se respeta.
UPDATE institutional_config
SET value = replace(replace(replace(value,
        '"label":"PRIMER CORTE"', '"label":"I"'),
        '"label":"SEGUNDO CORTE"', '"label":"II"'),
        '"label":"TERCER CORTE"', '"label":"III"'),
    updated_at = now()
WHERE key = 'evaluationTemplate';
