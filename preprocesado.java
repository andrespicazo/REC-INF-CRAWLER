package utility;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class preprocesado {

    private static Set<String> STOPWORDS = new HashSet<>();

    static {
        try {
            STOPWORDS = new HashSet<>(Files.readAllLines(Paths.get("./utility/stopwords-en.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String procesar(String cad) {
        // String[] terminos = new String[0];

        cad = minusculas(cad);
        cad = eliminar_signos(cad);
        cad = eliminar_numeros(cad);
        cad = eliminar_guiones(cad);
        cad = eliminar_espacios(cad);
        cad = eliminar_stopwords(cad);
        //cad = steaming(cad);

        return cad;
    }

    private static String eliminar_signos(String cad) {
        Pattern pat = Pattern.compile("[!\"#$%&'()*+,./:;<=>?@\\[\\]^_`{|}~]");
        Matcher mat = pat.matcher(cad);
        return mat.replaceAll("");
    }

    private static String eliminar_guiones(String cad) {
        return cad.replaceAll("\\s-\\s", " ");
    }

    private static String eliminar_numeros(String cad) {
        return cad.replaceAll("\\b\\d*\\b", " ");
    }

    private static String minusculas(String cad) {
        return cad.toLowerCase();
    }

    private static String eliminar_espacios(String cad) {
        return cad.replaceAll("\\s+", " ");
    }

    private static String eliminar_stopwords(String cad) {
        ArrayList<String> aux = Stream.of(cad.split("\\s"))
                .collect(Collectors.toCollection(ArrayList<String>::new));
        aux.removeAll(STOPWORDS);
        return aux.stream().collect(Collectors.joining(" "));
    }
    /*
     * private static String steaming(String cad) {
     * return cad;
     * }
     */
    public static void main(String[] args) {
    }
}
