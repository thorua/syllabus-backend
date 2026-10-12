package co.edu.uam.syllabus.catalogos;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlanAsignaturaRepository extends JpaRepository<PlanAsignatura, PlanAsignatura.Id> {

    /** Planes en que aparecen las asignaturas indicadas, con su programa. */
    @Query("select pa from PlanAsignatura pa join fetch pa.plan p join fetch p.programa"
            + " where pa.asignatura.id in :ids order by p.programa.name, p.name")
    List<PlanAsignatura> deAsignaturas(Collection<UUID> ids);

    /** Asignaturas del programa: las de sus planes y, en posgrado, las propias. */
    @Query("select pa.asignatura.id from PlanAsignatura pa where pa.plan.programa.id = :programaId"
            + " union select a.id from Asignatura a where a.programa.id = :programaId")
    Set<UUID> asignaturasDelPrograma(UUID programaId);
}
