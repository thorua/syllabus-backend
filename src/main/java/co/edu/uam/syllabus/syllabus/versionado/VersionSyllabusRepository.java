package co.edu.uam.syllabus.syllabus.versionado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface VersionSyllabusRepository extends JpaRepository<VersionSyllabus, UUID> {

    @EntityGraph(attributePaths = "modifiedBy")
    List<VersionSyllabus> findBySyllabusIdOrderByVersionNumberDesc(UUID syllabusId);

    @EntityGraph(attributePaths = "modifiedBy")
    Optional<VersionSyllabus> findByIdAndSyllabusId(UUID id, UUID syllabusId);

    Optional<VersionSyllabus> findFirstBySyllabusIdOrderByVersionNumberDesc(UUID syllabusId);
}
