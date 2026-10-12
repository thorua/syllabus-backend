package co.edu.uam.syllabus.catalogos;

import co.edu.uam.syllabus.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "departments")
public class Departamento extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @ManyToOne(optional = false)
    @JoinColumn(name = "faculty_id")
    private Facultad facultad;

    /** Correo de la coordinación: es el contacto de los syllabus de las asignaturas del departamento. */
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

    public Facultad getFacultad() {
        return facultad;
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
