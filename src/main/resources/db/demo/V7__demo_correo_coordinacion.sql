-- Demo: los programas de pregrado tienen coordinación; los posgrados no y muestran su docente.
UPDATE programs SET coordination_email = v.correo
FROM (VALUES
    ('20000000-0000-0000-0000-000000000001'::uuid, 'coordinacion.sistemas@autonoma.edu.co'),
    ('20000000-0000-0000-0000-000000000002'::uuid, 'coordinacion.software@autonoma.edu.co'),
    ('20000000-0000-0000-0000-000000000003'::uuid, 'coordinacion.matematicas@autonoma.edu.co')
) AS v (id, correo)
WHERE programs.id = v.id;
