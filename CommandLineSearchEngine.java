import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class CommandLineSearchEngine {

    // Indice invertido cargado desde el archivo
    private static final Map<String, List<String>> invertedIndex = new HashMap<>();

    public static void main(String[] args) {
        // Cargar el índice invertido desde el archivo
        try {
            loadIndexFromFile("utility/indice_invertido.txt");
        } catch (IOException e) {
            System.err.println("Error al cargar el índice invertido: " + e.getMessage());
            return;
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

            // Realizar la búsqueda
            List<String> results = performSearch(query);

            // Mostrar los resultados
            displayResults(results);
        }

        scanner.close();
    }

    private static void loadIndexFromFile(String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Parsear la línea del archivo
                String[] parts = line.split(";");
                if (parts.length < 3) continue; // Asegurarse de que la línea tiene formato válido

                String term = parts[0]; // La palabra clave
                // Extraer los documentos (nombre_documento-tf) como una lista
                List<String> documents = new ArrayList<>();
                for (int i = 2; i < parts.length; i++) {
                    String docInfo = parts[i].split("-")[0]; // Solo tomamos el nombre del documento
                    documents.add(docInfo);
                }

                // Guardar en el índice invertido
                invertedIndex.put(term, documents);
            }
        }
    }

    private static List<String> performSearch(String query) {
        List<String> results = new ArrayList<>();

        if (query.startsWith("\"") && query.endsWith("\"")) { // Búsqueda de frases exactas
            query = query.replace("\"", "").toLowerCase();
            // Dividir la frase en palabras y buscar documentos que contengan todos esos términos
            String[] terms = query.split("\\s+");
            results = Arrays.stream(terms)
                    .map(String::trim)
                    .map(term -> invertedIndex.getOrDefault(term, Collections.emptyList()))
                    .reduce((a, b) -> {
                        List<String> intersection = new ArrayList<>(a);
                        intersection.retainAll(b); // Intersección de las listas
                        return intersection;
                    }).orElse(Collections.emptyList());
        } else if (query.contains("AND")) { // Búsqueda con AND
            String[] terms = query.split("\\s*AND\\s*");  // Se maneja AND con espacios opcionales
            results = Arrays.stream(terms)
                    .map(String::trim)
                    .map(term -> invertedIndex.getOrDefault(term, Collections.emptyList()))
                    .reduce((a, b) -> {
                        List<String> intersection = new ArrayList<>(a);
                        intersection.retainAll(b); // Intersección de las listas
                        return intersection;
                    }).orElse(Collections.emptyList());
        } else if (query.contains("OR")) { // Búsqueda con OR
            String[] terms = query.split("\\s*OR\\s*");  // Se maneja OR con espacios opcionales
            results = Arrays.stream(terms)
                    .map(String::trim)
                    .flatMap(term -> invertedIndex.getOrDefault(term, Collections.emptyList()).stream())
                    .distinct() // Evitar duplicados
                    .collect(Collectors.toList());
        } else { // Búsqueda de un único término
            query = query.toLowerCase();
            results = invertedIndex.getOrDefault(query, Collections.emptyList());
        }

        return results;
    }

    private static void displayResults(List<String> results) {
        if (results.isEmpty()) {
            System.out.println("No se encontraron documentos relevantes para la consulta.");
        } else {
            System.out.println("Documentos encontrados:");
            for (String doc : results) {
                System.out.println("- " + doc);
            }
        }
    }
}
