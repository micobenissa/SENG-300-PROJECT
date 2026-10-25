package campusplan.persistence;

import campusplan.domain.AppState;
import java.io.IOException;

public interface Storage {
    AppState load() throws IOException;
    void save(AppState state) throws IOException;
}
