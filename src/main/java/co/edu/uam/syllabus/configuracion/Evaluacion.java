package co.edu.uam.syllabus.configuracion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Estrategia de evaluación (UC20). La plantilla institucional fija los cortes, sus rótulos y su
 * porcentaje sobre la nota final; cada corte se reparte en tipos de actividad cuyo porcentaje es
 * parcial (relativo al corte) y suma 100. El docente solo completa los tipos de su syllabus.
 */
public final class Evaluacion {

    public record Corte(int corte, String label, int porcentaje, List<Tipo> tipos) {}

    public record Tipo(String tipo, int porcentaje) {}

    private static final int MAX_CORTES = 3;
    private static final int MAX_TIPOS = 10;
    private static final int MAX_TEXTO = 100;

    private Evaluacion() {}

    /** Valida la plantilla que define el administrador: cortes y tipos completos y que sumen 100 %. */
    public static void validarPlantilla(List<Corte> cortes) {
        List<String> errores = new ArrayList<>();
        if (cortes == null || cortes.isEmpty() || cortes.size() > MAX_CORTES) {
            throw invalida("La plantilla debe tener entre 1 y " + MAX_CORTES + " cortes.");
        }
        for (int i = 0; i < cortes.size(); i++) {
            Corte c = cortes.get(i);
            if (c.corte() != i + 1) {
                errores.add("Los cortes deben numerarse de forma consecutiva desde 1.");
            }
            String rotulo = StringUtils.hasText(c.label()) ? c.label().trim() : "Corte " + (i + 1);
            if (!StringUtils.hasText(c.label()) || c.label().length() > MAX_TEXTO) {
                errores.add(rotulo + ": el rótulo es obligatorio (máximo " + MAX_TEXTO + " caracteres).");
            }
            if (c.porcentaje() < 1 || c.porcentaje() > 100) {
                errores.add(rotulo + ": el porcentaje debe estar entre 1 y 100.");
            }
            validarTipos(rotulo, c.tipos(), true, errores);
        }
        int total = cortes.stream().mapToInt(Corte::porcentaje).sum();
        if (total != 100) {
            errores.add("Los porcentajes de los cortes suman " + total + " % y deben sumar 100 %.");
        }
        lanzarSiHay(errores);
    }

    /**
     * Valida la evaluación de un syllabus frente a la plantilla vigente: los cortes (número, rótulo
     * y porcentaje) son los de la plantilla. Si {@code completa}, además exige tipos con nombre y
     * porcentajes parciales que sumen 100 % por corte (syllabus activo); un borrador puede quedar a medias.
     */
    public static void validarSyllabus(List<Corte> plantilla, List<Corte> evaluacion, boolean completa) {
        if (evaluacion == null || evaluacion.size() != plantilla.size()) {
            throw invalida("La evaluación debe tener los " + plantilla.size()
                    + " cortes definidos en la plantilla institucional.");
        }
        List<String> errores = new ArrayList<>();
        for (int i = 0; i < plantilla.size(); i++) {
            Corte base = plantilla.get(i);
            Corte c = evaluacion.get(i);
            if (c.corte() != base.corte() || !Objects.equals(c.label(), base.label())
                    || c.porcentaje() != base.porcentaje()) {
                throw invalida("Los cortes, rótulos y porcentajes de la evaluación los define la plantilla "
                        + "institucional; recargue el syllabus para aplicar la plantilla vigente.");
            }
            validarTipos(base.label(), c.tipos(), completa, errores);
        }
        lanzarSiHay(errores);
    }

    private static void validarTipos(String rotulo, List<Tipo> tipos, boolean completa, List<String> errores) {
        if (tipos == null || tipos.isEmpty() || tipos.size() > MAX_TIPOS) {
            errores.add(rotulo + ": debe tener entre 1 y " + MAX_TIPOS + " tipos de evaluación.");
            return;
        }
        for (Tipo t : tipos) {
            if (t.tipo() != null && t.tipo().length() > MAX_TEXTO) {
                errores.add(rotulo + ": el tipo de evaluación admite máximo " + MAX_TEXTO + " caracteres.");
            }
            if (t.porcentaje() < 0 || t.porcentaje() > 100) {
                errores.add(rotulo + ": cada porcentaje parcial debe estar entre 0 y 100.");
            }
            if (completa && !StringUtils.hasText(t.tipo())) {
                errores.add(rotulo + ": todos los tipos de evaluación deben tener nombre.");
            }
        }
        int suma = tipos.stream().mapToInt(Tipo::porcentaje).sum();
        if (completa && suma != 100) {
            errores.add(rotulo + ": los porcentajes parciales suman " + suma + " % y deben sumar 100 %.");
        }
    }

    private static void lanzarSiHay(List<String> errores) {
        if (!errores.isEmpty()) {
            throw invalida(String.join(" ", errores.stream().distinct().toList()));
        }
    }

    private static ResponseStatusException invalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
