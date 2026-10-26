package campusplan.persistence;

import campusplan.domain.AppState;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class JsonStorage implements Storage {
    private final Path path;
    private final JsonMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();

    public JsonStorage(Path path) {
        this.path = Objects.requireNonNull(path).toAbsolutePath().normalize();
    }

    @Override
    public AppState load() throws IOException {
        if (Files.notExists(path)) {
            return AppState.empty();
        }
        try {
            AppState result = mapper.readValue(Files.readAllBytes(path), AppState.class);
            if (result == null) {
                throw new IOException("Expected a JSON object.");
            }
            return result;
        } catch (IOException | IllegalArgumentException ex) {
            throw new IOException("Cannot load " + path + ": " + ex.getMessage()
                    + ". The file was not changed; restore a backup or choose another --data path.", ex);
        }
    }

    @Override
    public void save(AppState state) throws IOException {
        Objects.requireNonNull(state);
        // Check the current file before replacing it, including changes made outside the app.
        if (Files.exists(path)) {
            load();
        }
        Files.createDirectories(path.getParent());
        // Keep the temporary file on the same filesystem so replacement can be atomic.
        Path temporary = Files.createTempFile(path.getParent(), ".campusplan-", ".tmp");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), state);
            replaceFile(temporary);
        } catch (IOException ex) {
            throw new IOException("Save failed for " + path + ". Previous data was kept: "
                    + ex.getMessage(), ex);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    protected void replaceFile(Path temporary) throws IOException {
        // A filesystem without atomic replacement reports an error rather than risking the old file.
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
