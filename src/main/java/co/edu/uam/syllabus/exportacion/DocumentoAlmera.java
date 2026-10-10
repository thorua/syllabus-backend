package co.edu.uam.syllabus.exportacion;

import java.util.List;

/** Contenido ya resuelto que pinta la plantilla templates/syllabus-almera.html (UC17). */
public record DocumentoAlmera(Encabezado encabezado, List<Seccion> secciones) {

    /** Encabezado 3×3: logo, título y CÓDIGO / VERSIÓN / FECHA. El logo es un data URI. */
    public record Encabezado(String titulo, String codigo, String version, String fecha, String logo,
                             double logoAnchoMm, double logoAltoMm) {}

    /** Sección numerada; según su tipo usa párrafos, tabla de identificación, evaluación, bibliografía o líneas. */
    public record Seccion(int numero, String titulo, Tipo tipo, List<String> parrafos, List<FilaIdentificacion> filas,
                          List<Corte> cortes, List<Referencia> referencias, int lineas) {

        static Seccion texto(int numero, String titulo, List<String> parrafos) {
            return new Seccion(numero, titulo, Tipo.TEXTO, parrafos, List.of(), List.of(), List.of(), 0);
        }

        static Seccion identificacion(int numero, String titulo, List<FilaIdentificacion> filas) {
            return new Seccion(numero, titulo, Tipo.IDENTIFICACION, List.of(), filas, List.of(), List.of(), 0);
        }

        static Seccion evaluacion(int numero, String titulo, List<Corte> cortes) {
            return new Seccion(numero, titulo, Tipo.EVALUACION, List.of(), List.of(), cortes, List.of(), 0);
        }

        static Seccion bibliografia(int numero, String titulo, List<Referencia> referencias) {
            return new Seccion(numero, titulo, Tipo.BIBLIOGRAFIA, List.of(), List.of(), List.of(), referencias, 0);
        }

        static Seccion lineas(int numero, String titulo, int lineas) {
            return new Seccion(numero, titulo, Tipo.LINEAS, List.of(), List.of(), List.of(), List.of(), lineas);
        }
    }

    public enum Tipo { TEXTO, IDENTIFICACION, EVALUACION, BIBLIOGRAFIA, LINEAS }

    public record FilaIdentificacion(String rotulo, String valor, boolean correo) {}

    /** Corte de evaluación: su celda CORTE se combina verticalmente sobre sus tipos. */
    public record Corte(String rotulo, String total, List<TipoEvaluacion> tipos) {}

    public record TipoEvaluacion(String tipo, String parcial) {}

    /** Referencia APA: el título de la obra va en cursiva; un rótulo (Requerida, Sugerida…) va en negrita. */
    public record Referencia(boolean rotulo, String antes, String cursiva, String despues) {}
}
