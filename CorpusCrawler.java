import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;

public class CorpusCrawler {
    private static final String BASE_URL = "https://raw.githubusercontent.com/PdedP/RECINF-Project/refs/heads/main/";
    private static final String START_URL = BASE_URL + "index.html"; // Página inicial
    private static final String OUTPUT_DIRECTORY = ".";

    public static void main(String[] args) {
        try {
            File directory = new File(OUTPUT_DIRECTORY);
            if (!directory.exists()) {
                directory.mkdir();
            }

            // Descargar y analizar la página inicial
            System.out.println("Descargando índice...");
            Document document = Jsoup.connect(START_URL).get();
            Elements links = document.select("a[href]");

            for (Element link : links) {
                String relativePath = link.attr("href");
                String fileUrl = BASE_URL + relativePath;
                downloadFile(fileUrl, relativePath);
            }

            System.out.println("Descarga completada. Los archivos están en el directorio: " + OUTPUT_DIRECTORY);
        } catch (IOException e) {
            System.err.println("Error al procesar el índice: " + e.getMessage());
        }
    }

    private static void downloadFile(String fileUrl, String relativePath) {
        try (InputStream in = new URL(fileUrl).openStream()) {
            // Crear subdirectorios si es necesario
            File outputFile = new File(OUTPUT_DIRECTORY, relativePath);
            outputFile.getParentFile().mkdirs();

            try (OutputStream out = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                System.out.println("Archivo descargado: " + relativePath);
            }
        } catch (IOException e) {
            System.err.println("Error al descargar el archivo: " + fileUrl + " - " + e.getMessage());
        }
    }
}
