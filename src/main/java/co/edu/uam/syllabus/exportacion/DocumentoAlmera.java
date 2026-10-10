package co.edu.uam.syllabus.exportacion;

import java.util.List;

/** Contenido ya resuelto que pinta la plantilla templates/syllabus-almera.html (UC17). */
public record DocumentoAlmera(Encabezado encabezado, List<Seccion> secciones) {

    /** Encabezado 3×3: logo, título y CÓDIGO / VERSIÓN / FECHA. El logo es un data URI. */
    public record Encabezado(String titulo, String codigo, String version, String fecha, String logo,
                             double logoAnchoMm, double logoAltoMm) {}

    /** Sección numerada; según su tipo usa bloques de texto, tabla de identificación, evaluación o líneas. */
    public record Seccion(int numero, String titulo, Tipo tipo, List<Bloque> bloques, List<FilaIdentificacion> filas,
                          List<Corte> cortes, int lineas) {

        static Seccion texto(int numero, String titulo, List<Bloque> bloques) {
            return new Seccion(numero, titulo, Tipo.TEXTO, bloques, List.of(), List.of(), 0);
        }

        static Seccion bibliografia(int numero, String titulo, List<Bloque> bloques) {
            return new Seccion(numero, titulo, Tipo.BIBLIOGRAFIA, bloques, List.of(), List.of(), 0);
        }

        static Seccion identificacion(int numero, String titulo, List<FilaIdentificacion> filas) {
            return new Seccion(numero, titulo, Tipo.IDENTIFICACION, List.of(), filas, List.of(), 0);
        }

        static Seccion evaluacion(int numero, String titulo, List<Corte> cortes) {
            return new Seccion(numero, titulo, Tipo.EVALUACION, List.of(), List.of(), cortes, 0);
        }

        static Seccion lineas(int numero, String titulo, int lineas) {
            return new Seccion(numero, titulo, Tipo.LINEAS, List.of(), List.of(), List.of(), lineas);
        }
    }

    public enum Tipo { TEXTO, IDENTIFICACION, EVALUACION, BIBLIOGRAFIA, LINEAS }

    /** Línea de texto con su forma en el documento (ver {@link TextoConFormato}). */
    public record Bloque(Forma forma, List<Fragmento> fragmentos) {}

    public enum Forma { PARRAFO, SUBTITULO, VINETA, SUBVINETA, ESPACIO }

    public record Fragmento(String texto, boolean negrita, boolean cursiva) {}

    public record FilaIdentificacion(String rotulo, String valor, boolean correo) {}

    /** Corte de evaluación: CORTE y % TOTAL se combinan verticalmente sobre sus tipos. */
    public record Corte(String rotulo, String total, List<TipoEvaluacion> tipos) {}

    public record TipoEvaluacion(String tipo, String parcial) {}
}
