package co.edu.uam.syllabus.catalogos;

import co.edu.uam.syllabus.common.BaseEntity;
import co.edu.uam.syllabus.usuarios.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "subjects")
public class Asignatura extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private int credits;

    /**
     * Dueño de la asignatura: el departamento que la ofrece o, en posgrado, su programa (nunca ambos).
     * Los programas en que se cursa y su semestre salen de los planes de estudio.
     */
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Departamento departamento;

    @ManyToOne
    @JoinColumn(name = "program_id")
    private Programa programa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    private Usuario profesor;

    @Column(nullable = false)
    private String modality = "Presencial";

    @Column(name = "hours_presencial")
    private Integer hoursPresencial;

    @Column(name = "hours_dirigido")
    private Integer hoursDirigido;

    @Column(name = "hours_independiente")
    private Integer hoursIndependiente;

    private String prerequisites;

    /** Falso en los espacios del plan sin syllabus propio: cursos libres, electivas, énfasis, prácticas. */
    @Column(name = "requires_syllabus", nullable = false)
    private boolean requiresSyllabus = true;

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

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    /** Programa de posgrado dueño de la asignatura; nulo si la ofrece un departamento. */
    public Programa getPrograma() {
        return programa;
    }

    public void setPrograma(Programa programa) {
        this.programa = programa;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public void setDepartamento(Departamento departamento) {
        this.departamento = departamento;
    }

    public Usuario getProfesor() {
        return profesor;
    }

    public void setProfesor(Usuario profesor) {
        this.profesor = profesor;
    }

    public String getModality() {
        return modality;
    }

    public void setModality(String modality) {
        this.modality = modality;
    }

    public Integer getHoursPresencial() {
        return hoursPresencial;
    }

    public void setHoursPresencial(Integer hoursPresencial) {
        this.hoursPresencial = hoursPresencial;
    }

    public Integer getHoursDirigido() {
        return hoursDirigido;
    }

    public void setHoursDirigido(Integer hoursDirigido) {
        this.hoursDirigido = hoursDirigido;
    }

    public Integer getHoursIndependiente() {
        return hoursIndependiente;
    }

    public void setHoursIndependiente(Integer hoursIndependiente) {
        this.hoursIndependiente = hoursIndependiente;
    }

    public String getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(String prerequisites) {
        this.prerequisites = prerequisites;
    }

    /** Correo de la coordinación responsable: la del departamento o la del programa de posgrado. */
    public String coordinationEmail() {
        return departamento != null ? departamento.getCoordinationEmail() : programa.getCoordinationEmail();
    }

    /** Nombre del responsable de la asignatura, para los mensajes. */
    public String responsable() {
        return departamento != null ? departamento.getName() : programa.getName();
    }

    public boolean isRequiresSyllabus() {
        return requiresSyllabus;
    }

    public void setRequiresSyllabus(boolean requiresSyllabus) {
        this.requiresSyllabus = requiresSyllabus;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
