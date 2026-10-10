package co.edu.uam.syllabus.syllabus;

import co.edu.uam.syllabus.common.BaseEntity;
import co.edu.uam.syllabus.configuracion.Evaluacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Sección de un syllabus. La de evaluación guarda sus cortes en {@code evaluation}; las demás, texto. */
@Entity
@Table(name = "syllabus_sections")
public class SeccionSyllabus extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "syllabus_id")
    private Syllabus syllabus;

    @Column(name = "section_type", nullable = false)
    private String sectionType;

    @Column(nullable = false)
    private String title;

    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<Evaluacion.Corte> evaluation;

    @Column(name = "section_order", nullable = false)
    private int orden;

    protected SeccionSyllabus() {}

    public SeccionSyllabus(String sectionType, String title, String content, List<Evaluacion.Corte> evaluation,
                           int orden) {
        this.sectionType = sectionType;
        this.title = title;
        this.content = content;
        this.evaluation = evaluation;
        this.orden = orden;
    }

    void setSyllabus(Syllabus syllabus) {
        this.syllabus = syllabus;
    }

    public String getSectionType() {
        return sectionType;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public List<Evaluacion.Corte> getEvaluation() {
        return evaluation;
    }

    public int getOrden() {
        return orden;
    }
}
