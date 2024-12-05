import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import utility.Tupla;
import utility.preprocesado;

public class indexacion {
    // Numero de documentos
    private static Integer N = 0;
    // Direccion por defecto del corpus
    private static String corpus_path = "corpus";
    // Map auxiliar donde almaceno los terminos y su frecuencia en un fichero para
    // posteriormente calcular el tf
    private static Map<String, Integer> terminos_map = new HashMap<>();
    // Aqui almaceno el tf-ifd
    private static Map<String, Tupla<Double, Map<String, Double>>> tf_idf = new HashMap<String, Tupla<Double, Map<String, Double>>>();

    // Dividir texto en terminos y contar frecuencia en cada texto
    private static void dividir_en_terminos(String texto) {
        // Divido el texto en palbras
        String[] terminos = texto.split("\\s+");
        // Sin esto se crea siempre al principio un registro vacio
        ArrayList<String> listaTerminos = new ArrayList<>(Arrays.asList(terminos));
        listaTerminos.removeIf(String::isEmpty);
        terminos = listaTerminos.toArray(new String[0]);
        // Recorro todos los terminos
        for (String termino : terminos) {
            // Si no esta en el map
            if (terminos_map.get(termino) == null)
                // Inicializo el valor a 1
                terminos_map.put(termino, 1);
            // Si esta
            else {
                // Sumo 1 al valor
                terminos_map.put(termino, terminos_map.get(termino) + 1);
            }
        }
    }

    // Calcular tf de un documento. Se considera que la frecuencia de cada termino
    // para este documento esta almacenada en terminos_map
    private static void calcular_tf(String name) {
        // Recorro todos los terminos del documento actual
        for (Map.Entry<String, Integer> entry : terminos_map.entrySet()) {
            // Obtengo el termino
            String termino = entry.getKey();
            // Obtengo su frecuencia
            Integer frecuencia = entry.getValue();
            // Calculo el tf
            Double tf = 1 + Math.log(frecuencia) / Math.log(2);
            // Si no esta el termino en el mapa tf-idf lo inicializo vacio
            if (!tf_idf.containsKey(termino)) {
                Map<String, Double> mapaInterno = new HashMap<>();
                Tupla<Double, Map<String, Double>> nuevaTupla = new Tupla<>(0.0, mapaInterno);
                // Inserta la nueva tupla en el mapa con la clave proporcionada
                tf_idf.put(termino, nuevaTupla);
            }
            // Recupero la tupla del termino actual
            Tupla<Double, Map<String, Double>> tupla_actual = tf_idf.get(termino);
            // Recupero el map donde guardo el documento y su tf
            Map<String, Double> map_actual = tupla_actual.second;
            // Guardo el id y el tf
            map_actual.put(name, tf);
        }
    }

    private static void calcular_idf() {
        // Recorro todos los terminos del corpus
        for (Map.Entry<String, Tupla<Double, Map<String, Double>>> entry : tf_idf.entrySet()) {
            // Extraigo la tupla formada por el IDF(actualmente 0) y el map con los
            // documentos y el peso
            Tupla<Double, Map<String, Double>> tupla_actual = entry.getValue();
            // Obtengo el numero de documentos distintos en los que aparece el termino
            Integer n = tupla_actual.second.size();
            // Calculo el idf y lo incluyo en el map
            Double a = (double) N / n;
            tupla_actual.first = Math.log(a) / Math.log(2);
        }
    }

    private static void imprimir_indice() {
        String texto = "";
        // Recorro el indice
        for (Map.Entry<String, Tupla<Double, Map<String, Double>>> entry : tf_idf.entrySet()) {
            String termino = entry.getKey();
            Tupla<Double, Map<String, Double>> tupla_actual = entry.getValue();
            Double idf = tupla_actual.first;
            Map<String, Double> documentos = tupla_actual.second;
            texto = texto + termino + ";" + idf + ";";
            for (Map.Entry<String, Double> documento : documentos.entrySet())
                texto = texto + documento.getKey() + "-" + documento.getValue() + ";";
            texto = texto + "\n";
        }
        // Escribo en el fichero
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("utility/indice_invertido.txt"))) {
            writer.write(texto);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // En caso de que se indique se cambia el directorio del corpus
        if (args.length > 0) {
            corpus_path = args[0];
        }
        // Abro el directorio del corpus
        File dir = new File(corpus_path);
        // Compruebo que la direccion proporcionada (o por defecto) exista y si es un
        // directorio
        if (dir.exists() && dir.isDirectory()) {
            // Listo todos los documentos en un array
            File[] documentos = dir.listFiles();
            // Compruebo que este no sea nulo
            if (documentos != null) {
                // Recorro todos los documentos
                for (File documento : documentos) {
                    try {
                        // Leo 1 documento
                        String contenido = new String(Files.readAllBytes(Paths.get(documento.getPath())));
                        // Preproceso el documento (el preprocesado esta delegado a la clase
                        // preprocesado que tambien se usara para la busqueda)
                        contenido = preprocesado.procesar(contenido);
                        // Divido los terminos
                        dividir_en_terminos(contenido);
                        // Calculo el tf y empiezo a rellenar el indice
                        calcular_tf(documento.getName());
                        // Borro el contenido del map auxiliar para que lo use el siguiente documento
                        terminos_map.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // Aumento el numero de documentos
                    N++;
                }
            }
        }
        // Calculo el idf y completo el indice
        calcular_idf();
        // Imprimo el fichero con el indice
        imprimir_indice();
    }
}