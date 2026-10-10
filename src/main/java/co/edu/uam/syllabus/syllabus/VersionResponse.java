package co.edu.uam.syllabus.syllabus;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/** Versión de un syllabus (UC14); el listado omite el snapshot. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VersionResponse(UUID id, UUID syllabusId, int versionNumber, String changeDescription,
                              Autor modifiedBy, Instant createdAt, VersionSyllabus.Snapshot snapshot) {

    public record Autor(UUID id, String name, String email) {}

    static VersionResponse resumen(VersionSyllabus v) {
        return of(v, null);
    }

    static VersionResponse detalle(VersionSyllabus v) {
        return of(v, v.getSnapshot());
    }

    private static VersionResponse of(VersionSyllabus v, VersionSyllabus.Snapshot snapshot) {
        var u = v.getModifiedBy();
        return new VersionResponse(v.getId(), v.getSyllabusId(), v.getVersionNumber(), v.getChangeDescription(),
                new Autor(u.getId(), u.getName(), u.getEmail()), v.getCreatedAt(), snapshot);
    }
}
