import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ReplaceColors {
    public static void main(String[] args) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("DarkBackground", "MaterialTheme.colorScheme.background");
        replacements.put("DarkSurfaceVariant", "MaterialTheme.colorScheme.surfaceVariant");
        replacements.put("DarkSurface", "MaterialTheme.colorScheme.surface");
        replacements.put("TextPrimary", "MaterialTheme.colorScheme.onBackground");
        replacements.put("TextSecondary", "MaterialTheme.colorScheme.onSurfaceVariant");
        replacements.put("TextTertiary", "MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)");

        try (Stream<Path> paths = Files.walk(Paths.get("c:/Code/StudyTracker/app/src/main/java/com/iu/studytracker/ui"))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".kt"))
                 .filter(p -> !p.toString().endsWith("Theme.kt"))
                 .filter(p -> !p.toString().endsWith("Color.kt"))
                 .forEach(path -> processFile(path, replacements));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processFile(Path path, Map<String, String> replacements) {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            String newContent = content;
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                newContent = newContent.replace(entry.getKey(), entry.getValue());
            }
            if (!content.equals(newContent)) {
                Files.write(path, newContent.getBytes(StandardCharsets.UTF_8));
                System.out.println("Updated " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
