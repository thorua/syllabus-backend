package co.edu.uam.syllabus.catalogos;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Asignatura dentro de un plan de estudios: semestre, área de formación (la del plan publicado,
 * que puede variar entre programas) y orden dentro del semestre.
 */
@Entity
@Table(name = "study_plan_subjects")
public class PlanAsignatura {

    /** Áreas de formación de los planes publicados (pregrado y posgrado). */
    public enum Area {
        BASICA, PROFESIONAL, SOCIOHUMANISTICA, COMPLEMENTARIA,
        FUNDAMENTACION, PROFUNDIZACION, INVESTIGACION, INSTITUCIONAL
    }

    @Embeddable
    public static class Id implements Serializable {

        @Column(name = "plan_id")
        private UUID planId;

        @Column(name = "subject_id")
        private UUID subjectId;

        protected Id() {}

        Id(UUID planId, UUID subjectId) {
            this.planId = planId;
            this.subjectId = subjectId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Id otro && Objects.equals(planId, otro.planId) && Objects.equals(subjectId, otro.subjectId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(planId, subjectId);
        }
    }

    @EmbeddedId
    private Id id;

    @MapsId("planId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "plan_id")
    private PlanEstudios plan;

    @MapsId("subjectId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Asignatura asignatura;

    @Column(nullable = false)
    private int semester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Area area;

    @Column(nullable = false)
    private int position;

    protected PlanAsignatura() {}

    public PlanAsignatura(PlanEstudios plan, Asignatura asignatura, int semester, Area area, int position) {
        this.id = new Id(plan.getId(), asignatura.getId());
        this.plan = plan;
        this.asignatura = asignatura;
        this.semester = semester;
        this.area = area;
        this.position = position;
    }

    public PlanEstudios getPlan() {
        return plan;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public int getSemester() {
        return semester;
    }

    public Area getArea() {
        return area;
    }

    public int getPosition() {
        return position;
    }
}
