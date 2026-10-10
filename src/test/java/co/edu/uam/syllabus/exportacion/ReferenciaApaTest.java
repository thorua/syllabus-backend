package co.edu.uam.syllabus.exportacion;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uam.syllabus.exportacion.DocumentoAlmera.Referencia;
import org.junit.jupiter.api.Test;

/** Bibliografía APA del PDF: el título de la obra va en cursiva y los rótulos en negrita. */
class ReferenciaApaTest {

    @Test
    void separaAutoresTituloYFuente() {
        Referencia r = AlmeraPdf.referencia(
                "- Elmasri, R. & Navathe, S. (2016). Fundamentals of Database Systems (7th ed.). Pearson.");
        assertThat(r.antes()).isEqualTo("- Elmasri, R. & Navathe, S. (2016). ");
        assertThat(r.cursiva()).isEqualTo("Fundamentals of Database Systems (7th ed.).");
        assertThat(r.despues()).isEqualTo(" Pearson.");
    }

    @Test
    void reconoceLosRotulos() {
        assertThat(AlmeraPdf.referencia("Requerida:")).isEqualTo(new Referencia(true, "Requerida", "", ""));
    }

    @Test
    void sinAnioSeDejaComoTextoNormal() {
        assertThat(AlmeraPdf.referencia("PRESSMAN, Roger. Ingeniería del software."))
                .isEqualTo(new Referencia(false, "PRESSMAN, Roger. Ingeniería del software.", "", ""));
    }
}
