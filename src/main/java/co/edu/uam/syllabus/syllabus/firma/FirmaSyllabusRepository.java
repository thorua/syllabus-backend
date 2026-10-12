package co.edu.uam.syllabus.syllabus.firma;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FirmaSyllabusRepository extends JpaRepository<FirmaSyllabus, UUID> {

    List<FirmaSyllabus> findBySyllabusIdAndVersionNumberOrderBySignedAt(UUID syllabusId, int versionNumber);

    @Query(value = "SELECT step FROM syllabus_signatures WHERE syllabus_id = :id AND version_number = :version",
            nativeQuery = true)
    List<String> pasosFirmados(UUID id, int version);

    /** Filas [syllabus_id, step] con las firmas de la versión actual de cada syllabus. */
    @Query(value = """
            SELECT f.syllabus_id, f.step FROM syllabus_signatures f
            WHERE f.syllabus_id IN (:ids) AND f.version_number =
                (SELECT max(v.version_number) FROM syllabus_versions v WHERE v.syllabus_id = f.syllabus_id)""",
            nativeQuery = true)
    List<Object[]> pasosDeVersionActual(Collection<UUID> ids);
}
