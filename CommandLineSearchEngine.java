import java.util.*;
import java.util.stream.Collectors;

public class CommandLineSearchEngine {

    // ndice invertido simulado
    private static final Map<String, List<String>> invertedIndex = new HashMap<>();

    public static void main(String[] args) {
        // Simulación de datos para el índice invertido
        simulateIndex();

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

    private static void simulateIndex() {
        invertedIndex.put("ingenieria", Arrays.asList("doc1", "doc2"));
        invertedIndex.put("informatica", Arrays.asList("doc2", "doc3"));
        invertedIndex.put("sistemas", Arrays.asList("doc1", "doc3"));
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
            System.out.println(terms[0]);
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
