package co.edu.uam.syllabus.syllabus.firma;

import java.util.Collection;

/**
 * Aprobación del syllabus con firma digital: primero firma la coordinación que ofrece la asignatura
 * (departamento o programa de posgrado) y después Registro Académico. Con las dos firmas sobre la
 * versión actual, el syllabus queda verificado.
 */
public final class Verificacion {

    public enum Paso { DEPARTAMENTO, REGISTRO }

    public enum Estado { SIN_FIRMAS, PENDIENTE_REGISTRO, VERIFICADO }

    private Verificacion() {}

    /** Estado según los pasos ya firmados de una versión. */
    public static Estado estado(Collection<String> pasos) {
        if (pasos.contains(Paso.REGISTRO.name())) {
            return Estado.VERIFICADO;
        }
        return pasos.contains(Paso.DEPARTAMENTO.name()) ? Estado.PENDIENTE_REGISTRO : Estado.SIN_FIRMAS;
    }
}
