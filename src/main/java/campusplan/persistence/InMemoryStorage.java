package campusplan.persistence;

import campusplan.domain.AppState;
import java.util.Objects;

public final class InMemoryStorage implements Storage {
    private AppState state;

    public InMemoryStorage() {
        this(AppState.empty());
    }

    public InMemoryStorage(AppState state) {
        this.state = Objects.requireNonNull(state);
    }

    @Override
    public AppState load() {
        return state;
    }

    @Override
    public void save(AppState state) {
        this.state = Objects.requireNonNull(state);
    }
}
