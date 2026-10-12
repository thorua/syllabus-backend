package co.edu.uam.syllabus.catalogos;

import co.edu.uam.syllabus.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "programs")
public class Programa extends BaseEntity {

    public enum Nivel { PREGRADO, ESPECIALIZACION, MAESTRIA, DOCTORADO }

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Nivel level;

    /** Nulo en posgrado: los programas de posgrado no pertenecen a un departamento. */
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Departamento departamento;

    /** Solo en posgrado: el pregrado llega a su facultad por el departamento. */
    @ManyToOne
    @JoinColumn(name = "faculty_id")
    private Facultad facultad;

    /**
     * Solo en posgrado: correo de la coordinación del programa. Si es nulo, el syllabus muestra el
     * docente asignado. En pregrado el contacto es la coordinación del departamento.
     */
    @Column(name = "coordination_email")
    private String coordinationEmail;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Nivel getLevel() {
        return level;
    }

    public void setLevel(Nivel level) {
        this.level = level;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public void setDepartamento(Departamento departamento) {
        this.departamento = departamento;
    }

    /** Facultad del programa: la propia (posgrado) o la de su departamento (pregrado). */
    public Facultad getFacultad() {
        return departamento != null ? departamento.getFacultad() : facultad;
    }

    public void setFacultad(Facultad facultad) {
        this.facultad = facultad;
    }

    public String getCoordinationEmail() {
        return coordinationEmail;
    }

    public void setCoordinationEmail(String coordinationEmail) {
        this.coordinationEmail = coordinationEmail;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
