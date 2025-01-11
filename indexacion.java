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
import utility.Stemmer;

public class indexacion {
    // Numero de documentos
    private static Integer N = 0;
    // Direccion por defecto del corpus
    private static String corpus_path = "corpus";
    // Map auxiliar donde almaceno los terminos y su frecuencia en un fichero para
    // posteriormente calcular el tf
    private static Map<String, Integer> terminos_map = new HashMap<>();
    // Aqui almaceno el tf-ifd
    private static Map<String, Tupla<Double, Map<String, Double>>> indice_invertido = new HashMap<String, Tupla<Double, Map<String, Double>>>();
    // Aqui almaceno la longitud de cada documento
    private static Map<String, Double> longitud = new HashMap<>();

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
            // Aplico el algoritmo de stemming
            Stemmer stemmer = new Stemmer();
            char[] termArray = termino.toCharArray();
            stemmer.add(termArray, termArray.length);
            stemmer.stem();
            termino = stemmer.toString();
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
            if (!indice_invertido.containsKey(termino)) {
                Map<String, Double> mapaInterno = new HashMap<>();
                Tupla<Double, Map<String, Double>> nuevaTupla = new Tupla<>(0.0, mapaInterno);
                // Inserta la nueva tupla en el mapa con la clave proporcionada
                indice_invertido.put(termino, nuevaTupla);
            }
            // Recupero la tupla del termino actual
            Tupla<Double, Map<String, Double>> tupla_actual = indice_invertido.get(termino);
            // Recupero el map donde guardo el documento y su tf
            Map<String, Double> map_actual = tupla_actual.second;
            // Guardo el id y el tf
            map_actual.put(name, tf);
        }
    }

    private static void calcular_idf_y_longitud() {
        // Recorro todos los terminos del corpus
        for (Map.Entry<String, Tupla<Double, Map<String, Double>>> entry : indice_invertido.entrySet()) {
            // Extraigo la tupla formada por el IDF(inicialmente 0) y el map con los
            // documentos y el peso
            Tupla<Double, Map<String, Double>> tupla_actual = entry.getValue();
            // Obtengo el numero de documentos distintos en los que aparece el termino
            Integer n = tupla_actual.second.size();
            // Calculo el idf y lo incluyo en el map
            Double a = (double) N / n;
            Double idf = Math.log(a) / Math.log(2);
            tupla_actual.first = idf;
            for (Map.Entry<String, Double> doc : tupla_actual.second.entrySet()) {
                String docName = doc.getKey();
                // sustituyo el tf por el peso del termino en el documento
                Double peso = doc.getValue() * idf;
                doc.setValue(peso);
                // calculo de la longitud
                if (longitud.get(docName) != null)
                    longitud.put(docName, longitud.get(docName) + peso * peso);
                else
                    longitud.put(docName, peso * peso);
            }
        }
        for (Map.Entry<String, Double> doc : longitud.entrySet())
            doc.setValue(Math.sqrt(doc.getValue()));
    }

    private static void guardar_indice() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("utility/indice_invertido.dat"))) {
            for (Map.Entry<String, Tupla<Double, Map<String, Double>>> entry : indice_invertido.entrySet()) {
                String termino = entry.getKey();
                // System.out.println(termino + "\n");
                Tupla<Double, Map<String, Double>> tupla_actual = entry.getValue();
                Double idf = tupla_actual.first;
                Map<String, Double> documentos = tupla_actual.second;
                writer.write(termino + ";" + idf + ";");
                for (Map.Entry<String, Double> documento : documentos.entrySet())
                    writer.write(documento.getKey() + "-" + documento.getValue() + ";");
                writer.newLine();
            }
            System.out.println("¡Indice invertido guardado exitosamente!\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void guardar_longitud() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("utility/longitud.dat"))) {
            for (Map.Entry<String, Double> entry : longitud.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
            System.out.println("¡Longitud guardada exitosamente!\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void indexar() {
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
                System.out.println("Calculando TF...\n");
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
        System.out.println("Calculando el IDF de cada termino y la longitud de cada documento...\n");
        // Calculo el idf y completo el indice
        calcular_idf_y_longitud();
        // Imprimo el fichero con el indice
        System.out.println("Guardando el indice invertido...\n");
        guardar_indice();
        System.out.println("Guardando la longitud de los documentos...\n");
        guardar_longitud();
    }

    public static void main(String[] args) {
        indexar();
    }
}