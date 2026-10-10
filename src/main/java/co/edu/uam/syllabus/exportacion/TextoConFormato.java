package co.edu.uam.syllabus.exportacion;

import co.edu.uam.syllabus.exportacion.DocumentoAlmera.Bloque;
import co.edu.uam.syllabus.exportacion.DocumentoAlmera.Forma;
import co.edu.uam.syllabus.exportacion.DocumentoAlmera.Fragmento;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte el texto plano de una sección en los bloques del formato institucional. Las mismas
 * reglas las aplica el frontend (lib/textFormat.ts) para que la vista web y el PDF coincidan:
 * <ul>
 *   <li>{@code - texto} (o ✔, •): viñeta ✔; con sangría de dos espacios o más, subviñeta «o»
 *       (también {@code o texto} con sangría).</li>
 *   <li>{@code **texto**}: negrita; {@code *texto*}: cursiva.</li>
 *   <li>Una línea toda en mayúsculas es un subtítulo en negrita.</li>
 *   <li>Las líneas en blanco se conservan como un espacio (varias seguidas cuentan como una).</li>
 * </ul>
 * En la bibliografía cada línea es una referencia con viñeta ✔, salvo los rótulos Requerida,
 * Sugerida y Complementaria.
 */
final class TextoConFormato {

    private static final Pattern VINETA = Pattern.compile("^([-✔•]|o(?=\\s))\\s+(.*)$");
    private static final Pattern ESTILO = Pattern.compile("\\*\\*(.+?)\\*\\*|\\*([^*\\s][^*]*?)\\*");
    private static final Pattern ROTULO_BIBLIOGRAFIA =
            Pattern.compile("^(Requerida|Sugerida|Complementaria)\\s*:?$", Pattern.CASE_INSENSITIVE);

    private TextoConFormato() {}

    static List<Bloque> bloques(String texto) {
        List<Bloque> bloques = new ArrayList<>();
        for (String linea : lineas(texto)) {
            String limpia = linea.strip();
            if (limpia.isEmpty()) {
                espacio(bloques);
                continue;
            }
            int sangria = linea.length() - linea.stripLeading().length();
            Matcher vineta = VINETA.matcher(limpia);
            boolean esO = vineta.matches() && vineta.group(1).equals("o");
            if (vineta.matches() && (!esO || sangria >= 2)) {
                Forma forma = sangria >= 2 || esO ? Forma.SUBVINETA : Forma.VINETA;
                bloques.add(new Bloque(forma, fragmentos(vineta.group(2))));
            } else if (esMayusculas(limpia)) {
                bloques.add(new Bloque(Forma.SUBTITULO, List.of(new Fragmento(sinMarcas(limpia), true, false))));
            } else {
                bloques.add(new Bloque(Forma.PARRAFO, fragmentos(limpia)));
            }
        }
        return sinEspacioFinal(bloques);
    }

    static List<Bloque> bibliografia(String texto) {
        List<Bloque> bloques = new ArrayList<>();
        for (String linea : lineas(texto)) {
            String limpia = linea.strip();
            if (limpia.isEmpty()) {
                espacio(bloques);
            } else if (ROTULO_BIBLIOGRAFIA.matcher(limpia).matches()) {
                String rotulo = limpia.replaceAll("\\s*:$", "");
                bloques.add(new Bloque(Forma.SUBTITULO, List.of(new Fragmento(rotulo, true, false))));
            } else {
                Matcher vineta = VINETA.matcher(limpia);
                bloques.add(new Bloque(Forma.VINETA, fragmentos(vineta.matches() ? vineta.group(2) : limpia)));
            }
        }
        return sinEspacioFinal(bloques);
    }

    static List<Fragmento> fragmentos(String texto) {
        List<Fragmento> fragmentos = new ArrayList<>();
        Matcher estilo = ESTILO.matcher(texto);
        int desde = 0;
        while (estilo.find()) {
            if (estilo.start() > desde) {
                fragmentos.add(new Fragmento(texto.substring(desde, estilo.start()), false, false));
            }
            boolean negrita = estilo.group(1) != null;
            fragmentos.add(new Fragmento(negrita ? estilo.group(1) : estilo.group(2), negrita, !negrita));
            desde = estilo.end();
        }
        if (desde < texto.length()) {
            fragmentos.add(new Fragmento(texto.substring(desde), false, false));
        }
        return fragmentos;
    }

    private static List<String> lineas(String texto) {
        return texto == null ? List.of() : List.of(texto.replace("\t", "    ").split("\\R"));
    }

    private static void espacio(List<Bloque> bloques) {
        if (!bloques.isEmpty() && bloques.getLast().forma() != Forma.ESPACIO) {
            bloques.add(new Bloque(Forma.ESPACIO, List.of()));
        }
    }

    private static List<Bloque> sinEspacioFinal(List<Bloque> bloques) {
        if (!bloques.isEmpty() && bloques.getLast().forma() == Forma.ESPACIO) {
            bloques.removeLast();
        }
        return bloques;
    }

    private static String sinMarcas(String texto) {
        return texto.replace("*", "");
    }

    private static boolean esMayusculas(String texto) {
        return texto.chars().anyMatch(Character::isLetter)
                && texto.equals(texto.toUpperCase(Locale.ROOT))
                && texto.length() <= 120;
    }
}
