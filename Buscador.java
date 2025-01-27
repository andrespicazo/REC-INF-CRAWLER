import utility.Stemmer;
import utility.preprocesado;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Buscador {

    // Indice invertido construido desde el archivo
    private static final Map<String, List<DocumentoPeso>> indiceInvertido = new HashMap<>();
    //           Mapa que guarda esto:
    //             - Clave: el termino 
    //             - Valor:la lLista de documentos que contienen el término, con sus pesos TF-IDF

    public static void main(String[] args) {
        // Construir el índice desde el archivo en la carpeta "utility"
        try {
            cargarIndexArchivo("utility/indice_invertido.dat");
        } catch (IOException e) {
            System.err.println("Error al cargar el índice invertido: " + e.getMessage());
            return; // Termina el programa si no se puede cargar el archivo
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("Bienvenido al buscador de documentos.");
        System.out.println("Introduce una consulta (puedes usar operadores AND/OR):");

        while (true) {
            System.out.print("> ");
            String query = scanner.nextLine().trim();

            // Salir si el usuario escribe "salir"
            if (query.equalsIgnoreCase("salir")) {
                System.out.println("Saliendo del buscador.");
                break;
            }

            // Procesar la consulta
            query = preprocesado.procesar(query);

            // Realizar la búsqueda y ranking
            Map<String, Double> rankResultado = rankDocumentos(query);

            // Mostrar los resultados
            monstrarResultados(rankResultado);
        }

        scanner.close();
    }

    private static void cargarIndexArchivo(String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Parsear cada línea del archivo
                String[] parts = line.split(";");
                if (parts.length < 3) continue; // Saltar líneas mal formateadas
    
                String word = parts[0].toLowerCase(); // Primera parte: palabra clave
                double idf = Double.parseDouble(parts[1]); // Segunda parte: IDF
                List<DocumentoPeso> documents = new ArrayList<>();
    
                // Procesar documentos-tf_idf asociados
                for (int i = 2; i < parts.length; i++) {
                    String[] docParts = parts[i].split("-");
                    if (docParts.length > 1) {
                        String nombreDocumento = docParts[0]; // Guarda el documento seguido del tf_idf-idf
                        double tf_idf = Double.parseDouble(docParts[1]);
                        documents.add(new DocumentoPeso(nombreDocumento, tf_idf, idf));
                    }
                }
    
                // Agregar al índice invertido
                indiceInvertido.put(word, documents);
            }
        }
    }

    private static Map<String, Double> rankDocumentos(String query) {
        // Crear una instancia del stemmer
        Stemmer stemmer = new Stemmer();

        // Determinar el tipo de consulta
        boolean esAndQuery = query.contains("and");
        boolean esOrQuery = query.contains("or");
    
        // Si la consulta tiene AND o OR, dividirla en términos
        String[] terminos;
        if (esAndQuery) {
            terminos = query.split("\\s*and\\s*");  // Se maneja AND con espacios opcionales
        } else if (esOrQuery) {
            terminos = query.split("\\s*or\\s*");   // Se maneja OR con espacios opcionales
        } else {
            // Si no contiene AND ni OR, tratamos toda la consulta como un único término
            terminos = new String[]{query.trim()};  // Se toma como un único término
        }
    
        Set<String> documentoRelevante = new HashSet<>();
        Map<String, Double> puntuacionDocumento = new HashMap<>(); // Para acumular los puntajes
    
        for (String term : terminos) {
            term = term.trim();  // Limpiar posibles espacios extras
    
            if (term.isEmpty()) continue;  // Asegurarse de que no estamos procesando términos vacíos

            // Aplicar stemming al término
            stemmer.add(term.toCharArray(), term.length());
            stemmer.stem();
            term = stemmer.toString();  // Obtener la raíz del término
    
            List<DocumentoPeso> pesoDocumento = indiceInvertido.getOrDefault(term, Collections.emptyList());
            Set<String> documentParaTermino = pesoDocumento.stream()
                    .map(dw -> dw.nombreDocumento)
                    .collect(Collectors.toSet());
    
            if (esAndQuery) {
                // Si es una consulta con AND, hacemos la intersección de documentos
                if (documentoRelevante.isEmpty()) {
                    documentoRelevante = new HashSet<>(documentParaTermino);
                } else {
                    documentoRelevante.retainAll(documentParaTermino); // Intersección
                }
            } else if (esOrQuery) {
                // Si es una consulta con OR, hacemos la unión de documentos
                documentoRelevante.addAll(documentParaTermino);
            } else {
                // Implícito OR si no hay operadores
                documentoRelevante.addAll(documentParaTermino);
            }
        }
    
        // Si no hay documentos relevantes, retornar un mapa vacío
        if (documentoRelevante.isEmpty()) {
            return Collections.emptyMap();
        }
    
        // Calcular puntajes para los documentos relevantes
        for (String term : terminos) {
            term = term.trim();  // Limpiar espacios
    
            if (term.isEmpty()) continue;  // Saltar si el término está vacío

            // Aplica steming otra vez antes de calcular puntajes
            stemmer.add(term.toCharArray(), term.length());
            stemmer.stem();
            term = stemmer.toString();

            List<DocumentoPeso> pesoDocumento = indiceInvertido.getOrDefault(term, Collections.emptyList());
            for (DocumentoPeso docWeight : pesoDocumento) {
                if (documentoRelevante.contains(docWeight.nombreDocumento)) {

                    double score = docWeight.tf_idf * docWeight.idf;
                    puntuacionDocumento.merge(docWeight.nombreDocumento, score, Double::sum);
                    
                }
            }
        }
    
        // Ordenar los documentos por puntaje en orden descendente
        return puntuacionDocumento.entrySet()
                .stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10) // Mostrar solo los 10 mejores resultados
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }



    private static void monstrarResultados(Map<String, Double> rankResultado) {
        if (rankResultado.isEmpty()) {
            System.out.println("No se encontraron documentos relevantes para la consulta.");
        } else {
            System.out.println("Documentos encontrados:");
            rankResultado.forEach((doc, score) -> System.out.printf("- %s (Score: %.4f)%n", doc, score));
        }
    }

    // Clase auxiliar para representar un documento y su peso
    // Clase auxiliar para representar un documento, su TF y el IDF del término
    private static class DocumentoPeso {
    String nombreDocumento;
    double tf_idf; // Frecuencia del término en el documento
    double idf; // IDF del término

        DocumentoPeso(String nombreDocumento, double tf_idf, double idf) {
            this.nombreDocumento = nombreDocumento;
            this.tf_idf = tf_idf;
            this.idf = idf;
        }
    }

}
