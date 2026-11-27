package campusplan.application.history;

import campusplan.domain.AppState;

/**
 * One user mutation as an object that can be undone and redone (the Command interface).
 *
 * <p>Commands do not hold the application state. They only turn one state into the next and back,
 * so the service stays the single owner of the current state.
 */
public interface Command {
    /** Short text for messages such as "Undid: Create draft Study group". */
    String description();

    /** Applies the change to the given current state and returns the new state. */
    AppState execute(AppState current);

    /** Returns the state as it was before this command ran. */
    AppState undo();

    /** Applies the same change again and returns the state it produced the first time. */
    AppState redo();
}
