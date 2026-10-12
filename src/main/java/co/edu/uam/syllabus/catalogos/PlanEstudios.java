package co.edu.uam.syllabus.catalogos;

import co.edu.uam.syllabus.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/** Plan de estudios (malla curricular) de un programa: qué asignaturas se ven y en qué semestre. */
@Entity
@Table(name = "study_plans")
public class PlanEstudios extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "program_id", updatable = false)
    private Programa programa;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanAsignatura> asignaturas = new ArrayList<>();

    protected PlanEstudios() {}

    public PlanEstudios(Programa programa) {
        this.programa = programa;
    }

    public Programa getPrograma() {
        return programa;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<PlanAsignatura> getAsignaturas() {
        return asignaturas;
    }
}
