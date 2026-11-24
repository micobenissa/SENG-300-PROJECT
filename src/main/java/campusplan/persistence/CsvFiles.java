package campusplan.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Writes exported text (such as a roster CSV) to a UTF-8 file. Export is a file operation, so it lives here. */
public final class CsvFiles {
    private CsvFiles() { }

    public static Path write(Path path, String content) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(content);
        Path absolute = path.toAbsolutePath().normalize();
        if (absolute.getParent() != null) {
            Files.createDirectories(absolute.getParent());
        }
        Files.writeString(absolute, content, StandardCharsets.UTF_8);
        return absolute;
    }
}
