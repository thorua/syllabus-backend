package co.edu.uam.syllabus.exportacion;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uam.syllabus.exportacion.DocumentoAlmera.Bloque;
import co.edu.uam.syllabus.exportacion.DocumentoAlmera.Forma;
import co.edu.uam.syllabus.exportacion.DocumentoAlmera.Fragmento;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Convenciones del texto de las secciones; frontend/src/lib/textFormat.test.ts prueba los mismos casos. */
class TextoConFormatoTest {

    private static List<Forma> formas(List<Bloque> bloques) {
        return bloques.stream().map(Bloque::forma).toList();
    }

    @Test
    void reconoceVinetasSubvinetasSubtitulosYEspacios() {
        List<Bloque> bloques = TextoConFormato.bloques("""
                RESULTADOS DE APRENDIZAJE:

                Párrafo de texto.
                - Metodología Scrum
                  - Roles del equipo
                o no es viñeta sin sangría


                ✔ Principios SOLID
                """);
        assertThat(formas(bloques)).containsExactly(Forma.SUBTITULO, Forma.ESPACIO, Forma.PARRAFO, Forma.VINETA,
                Forma.SUBVINETA, Forma.PARRAFO, Forma.ESPACIO, Forma.VINETA);
        assertThat(bloques.get(4).fragmentos()).containsExactly(new Fragmento("Roles del equipo", false, false));
    }

    @Test
    void separaNegritaYCursiva() {
        assertThat(TextoConFormato.fragmentos("**SO2** – Diseño de *software* seguro")).containsExactly(
                new Fragmento("SO2", true, false),
                new Fragmento(" – Diseño de ", false, false),
                new Fragmento("software", false, true),
                new Fragmento(" seguro", false, false));
    }

    @Test
    void enLaBibliografiaCadaReferenciaLlevaVinetaYLosRotulosVanEnNegrita() {
        List<Bloque> bloques = TextoConFormato.bibliografia("""
                Requerida:
                - FOWLER, Martin. *Refactoring*. Addison-Wesley, 2018.
                PRESSMAN, Roger. Ingeniería del software.

                Sugerida
                """);
        assertThat(formas(bloques)).containsExactly(
                Forma.SUBTITULO, Forma.VINETA, Forma.VINETA, Forma.ESPACIO, Forma.SUBTITULO);
        assertThat(bloques.get(0).fragmentos().getFirst().texto()).isEqualTo("Requerida");
        assertThat(bloques.get(1).fragmentos()).contains(new Fragmento("Refactoring", false, true));
    }
}
