package co.edu.uam.syllabus.syllabus;

import co.edu.uam.syllabus.catalogos.Asignatura;
import co.edu.uam.syllabus.common.BaseEntity;
import co.edu.uam.syllabus.usuarios.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "syllabus")
public class Syllabus extends BaseEntity {

    /** ACTIVO es la versión vigente que consulta el estudiante (UC10); INACTIVO es el borrado lógico (UC13). */
    public enum Estado { ACTIVO, INACTIVO, BORRADOR }

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id", updatable = false)
    private Asignatura asignatura;

    @Column(name = "academic_period", nullable = false, updatable = false)
    private String academicPeriod;

    @Column(nullable = false)
    private String title;

    /** Encabezado propio; si es nulo se usa el de la configuración institucional (UC18). */
    @Column(name = "header_title")
    private String headerTitle;

    @Column(name = "document_code")
    private String documentCode;

    @Column(name = "document_date")
    private String documentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado status = Estado.BORRADOR;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by", updatable = false)
    private Usuario createdBy;

    @OneToMany(mappedBy = "syllabus", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<SeccionSyllabus> secciones = new ArrayList<>();

    protected Syllabus() {}

    public Syllabus(Asignatura asignatura, String academicPeriod, Usuario createdBy) {
        this.asignatura = asignatura;
        this.academicPeriod = academicPeriod;
        this.createdBy = createdBy;
    }

    /** Reemplaza las secciones conservando la colección gestionada por JPA. */
    public void reemplazarSecciones(List<SeccionSyllabus> nuevas) {
        secciones.clear();
        nuevas.forEach(s -> {
            s.setSyllabus(this);
            secciones.add(s);
        });
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public String getAcademicPeriod() {
        return academicPeriod;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public void setHeaderTitle(String headerTitle) {
        this.headerTitle = headerTitle;
    }

    public String getDocumentCode() {
        return documentCode;
    }

    public void setDocumentCode(String documentCode) {
        this.documentCode = documentCode;
    }

    public String getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(String documentDate) {
        this.documentDate = documentDate;
    }

    public Estado getStatus() {
        return status;
    }

    public void setStatus(Estado status) {
        this.status = status;
    }

    public Usuario getCreatedBy() {
        return createdBy;
    }

    public List<SeccionSyllabus> getSecciones() {
        return secciones;
    }
}
