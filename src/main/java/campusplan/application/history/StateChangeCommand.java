package campusplan.application.history;

import campusplan.domain.AppState;

import java.util.Objects;

/**
 * A command whose effect is a complete, already validated replacement state.
 *
 * <p>Operations such as confirming a suggestion change several things at once (the event, its room and
 * its responses). Keeping the states before and after makes undo and redo restore all of them together.
 * Redo reuses the original result rather than running the operation again, so generated IDs do not change.
 */
public final class StateChangeCommand implements Command {
    private final String description;
    private final AppState after;
    private AppState before;

    public StateChangeCommand(String description, AppState after) {
        this.description = Objects.requireNonNull(description);
        this.after = Objects.requireNonNull(after);
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public AppState execute(AppState current) {
        before = Objects.requireNonNull(current);
        return after;
    }

    @Override
    public AppState undo() {
        if (before == null) {
            throw new IllegalStateException("Command was never executed.");
        }
        return before;
    }

    @Override
    public AppState redo() {
        return after;
    }
}
