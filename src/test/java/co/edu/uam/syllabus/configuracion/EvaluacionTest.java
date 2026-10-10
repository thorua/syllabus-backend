package co.edu.uam.syllabus.configuracion;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uam.syllabus.configuracion.Evaluacion.Corte;
import co.edu.uam.syllabus.configuracion.Evaluacion.Tipo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** UC20: cortes que suman 100 % y tipos con porcentaje parcial que suman 100 % en cada corte. */
class EvaluacionTest {

    private static final List<Corte> PLANTILLA = List.of(
            new Corte(1, "PRIMER CORTE", 30, List.of(new Tipo("Quiz", 40), new Tipo("Taller", 60))),
            new Corte(2, "SEGUNDO CORTE", 35, List.of(new Tipo("Parcial", 100))),
            new Corte(3, "TERCER CORTE", 35, List.of(new Tipo("Proyecto", 100))));

    @Test
    void aceptaUnaPlantillaValida() {
        assertThatCode(() -> Evaluacion.validarPlantilla(PLANTILLA)).doesNotThrowAnyException();
    }

    @Test
    void rechazaCortesQueNoSuman100() {
        List<Corte> plantilla = List.of(PLANTILLA.get(0), PLANTILLA.get(1));
        assertThatThrownBy(() -> Evaluacion.validarPlantilla(plantilla))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("suman 65 % y deben sumar 100 %");
    }

    @Test
    void rechazaCortesSinRotuloONumeracionConsecutiva() {
        List<Corte> plantilla = List.of(
                new Corte(1, " ", 50, List.of(new Tipo("Quiz", 100))),
                new Corte(3, "TERCER CORTE", 50, List.of(new Tipo("Quiz", 100))));
        assertThatThrownBy(() -> Evaluacion.validarPlantilla(plantilla))
                .hasMessageContaining("el rótulo es obligatorio")
                .hasMessageContaining("numerarse de forma consecutiva");
    }

    @Test
    void elSyllabusDebeConservarLosCortesDeLaPlantilla() {
        List<Corte> otroPorcentaje = List.of(
                new Corte(1, "PRIMER CORTE", 40, PLANTILLA.get(0).tipos()), PLANTILLA.get(1), PLANTILLA.get(2));
        assertThatThrownBy(() -> Evaluacion.validarSyllabus(PLANTILLA, otroPorcentaje, false))
                .hasMessageContaining("los define la plantilla institucional");
        assertThatThrownBy(() -> Evaluacion.validarSyllabus(PLANTILLA, PLANTILLA.subList(0, 2), false))
                .hasMessageContaining("los 3 cortes");
    }

    @Test
    void unBorradorPuedeQuedarIncompletoPeroUnActivoNo() {
        List<Corte> incompleta = List.of(
                new Corte(1, "PRIMER CORTE", 30, List.of(new Tipo("", 20))), PLANTILLA.get(1), PLANTILLA.get(2));
        assertThatCode(() -> Evaluacion.validarSyllabus(PLANTILLA, incompleta, false)).doesNotThrowAnyException();
        assertThatThrownBy(() -> Evaluacion.validarSyllabus(PLANTILLA, incompleta, true))
                .hasMessageContaining("deben tener nombre")
                .hasMessageContaining("suman 20 %");
    }
}
