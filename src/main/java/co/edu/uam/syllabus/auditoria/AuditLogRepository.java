package co.edu.uam.syllabus.auditoria;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    @Override
    @EntityGraph(attributePaths = "usuario")
    @NonNull
    Page<AuditLog> findAll(@Nullable Specification<AuditLog> spec, @NonNull Pageable pageable);

    @Query("select distinct a.entity from AuditLog a order by a.entity")
    List<String> entidades();

    @Query("select distinct a.action from AuditLog a order by a.action")
    List<String> acciones();
}
