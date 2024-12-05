import utility.preprocesado;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class CommandLineSearchEngine {

    // Indice invertido construido desde el archivo
    private static final Map<String, List<DocumentWeight>> invertedIndex = new HashMap<>();

    public static void main(String[] args) {
        // Construir el índice desde el archivo en la carpeta "utility"
        try {
            loadIndexFromFile("utility/indice_invertido.txt");
        } catch (IOException e) {
            System.err.println("Error al cargar el índice invertido: " + e.getMessage());
            return; // Termina el programa si no se puede cargar el archivo
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("Bienvenido al buscador de documentos.");
        System.out.println("Introduce una consulta (puedes usar operadores AND/OR o comillas para frases):");

        while (true) {
            System.out.print("> ");
            String query = scanner.nextLine().trim();

            // Salir si el usuario escribe "salir"
            if (query.equalsIgnoreCase("salir")) {
                System.out.println("Saliendo del buscador. ¡Hasta luego!");
                break;
            }

            // Procesar la consulta
            query = preprocesado.procesar(query);

            // Realizar la búsqueda y ranking
            Map<String, Double> rankedResults = rankDocuments(query);

            // Mostrar los resultados
            displayResults(rankedResults);
        }

        scanner.close();
    }

    private static void loadIndexFromFile(String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Parsear cada línea del archivo
                String[] parts = line.split(";");
                if (parts.length < 3) continue; // Saltar líneas mal formateadas
    
                String word = parts[0].toLowerCase(); // Primera parte: palabra clave
                double idf = Double.parseDouble(parts[1]); // Segunda parte: IDF
                List<DocumentWeight> documents = new ArrayList<>();
    
                // Procesar documentos-tf asociados
                for (int i = 2; i < parts.length; i++) {
                    String[] docParts = parts[i].split("-");
                    if (docParts.length > 1) {
                        String documentName = docParts[0];
                        double tf = Double.parseDouble(docParts[1]);
                        documents.add(new DocumentWeight(documentName, tf, idf));
                    }
                }
    
                // Agregar al índice invertido
                invertedIndex.put(word, documents);
            }
        }
    }

    private static Map<String, Double> rankDocuments(String query) {
        // Determinar el tipo de consulta
        boolean isAndQuery = query.contains("and");
        boolean isOrQuery = query.contains("or");
    
        // Si la consulta tiene AND o OR, dividirla en términos
        String[] terms;
        if (isAndQuery) {
            terms = query.split("\\s*and\\s*");  // Se maneja AND con espacios opcionales
        } else if (isOrQuery) {
            terms = query.split("\\s*or\\s*");   // Se maneja OR con espacios opcionales
        } else {
            // Si no contiene AND ni OR, tratamos toda la consulta como un único término
            terms = new String[]{query.trim()};  // Se toma como un único término
        }
    
        Set<String> relevantDocuments = new HashSet<>();
        Map<String, Double> documentScores = new HashMap<>(); // Para acumular los puntajes
    
        for (String term : terms) {
            term = term.trim();  // Limpiar posibles espacios extras
    
            if (term.isEmpty()) continue;  // Asegurarse de que no estamos procesando términos vacíos
    
            List<DocumentWeight> documentWeights = invertedIndex.getOrDefault(term, Collections.emptyList());
            Set<String> documentsForTerm = documentWeights.stream()
                    .map(dw -> dw.documentName)
                    .collect(Collectors.toSet());
    
            if (isAndQuery) {
                // Si es una consulta con AND, hacemos la intersección de documentos
                if (relevantDocuments.isEmpty()) {
                    relevantDocuments = new HashSet<>(documentsForTerm);
                } else {
                    relevantDocuments.retainAll(documentsForTerm);
                }
            } else if (isOrQuery) {
                // Si es una consulta con OR, hacemos la unión de documentos
                relevantDocuments.addAll(documentsForTerm);
            } else {
                // Implícito OR si no hay operadores
                relevantDocuments.addAll(documentsForTerm);
            }
        }
    
        // Si no hay documentos relevantes, retornar un mapa vacío
        if (relevantDocuments.isEmpty()) {
            return Collections.emptyMap();
        }
    
        // Calcular puntajes para los documentos relevantes
        for (String term : terms) {
            term = term.trim();  // Limpiar espacios
    
            if (term.isEmpty()) continue;  // Saltar si el término está vacío
    
            List<DocumentWeight> documentWeights = invertedIndex.getOrDefault(term, Collections.emptyList());
            for (DocumentWeight docWeight : documentWeights) {
                if (relevantDocuments.contains(docWeight.documentName)) {
                    // Calcular el puntaje para cada documento
                    double score = docWeight.tf * docWeight.idf * docWeight.idf;
                    // Sumar puntaje al documento sin duplicar
                    documentScores.merge(docWeight.documentName, score, Double::sum);
                }
            }
        }
    
        // Ordenar los documentos por puntaje en orden descendente
        return documentScores.entrySet()
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

    private static double calculateIDF(String term) {
        // Obtener los documentos donde aparece el término
        List<DocumentWeight> documentWeights = invertedIndex.getOrDefault(term, Collections.emptyList());
        if (documentWeights.isEmpty()) return 0.0;

        // Tomar el IDF del primer documento asociado
        return documentWeights.get(0).idf;
    }

    private static void displayResults(Map<String, Double> rankedResults) {
        if (rankedResults.isEmpty()) {
            System.out.println("No se encontraron documentos relevantes para la consulta.");
        } else {
            System.out.println("Documentos encontrados:");
            rankedResults.forEach((doc, score) -> System.out.printf("- %s (Score: %.4f)%n", doc, score));
        }
    }

    // Clase auxiliar para representar un documento y su peso
    // Clase auxiliar para representar un documento, su TF y el IDF del término
    private static class DocumentWeight {
    String documentName;
    double tf; // Frecuencia del término en el documento
    double idf; // IDF del término

        DocumentWeight(String documentName, double tf, double idf) {
            this.documentName = documentName;
            this.tf = tf;
            this.idf = idf;
        }
    }

}
