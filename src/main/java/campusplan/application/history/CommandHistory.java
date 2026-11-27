package campusplan.application.history;

import campusplan.domain.AppState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * The undo and redo stacks for the current session (the Command pattern's invoker).
 *
 * <p>Only commands that executed successfully are recorded. Running a new command clears the redo stack.
 * History is not saved, so it starts empty after a restart.
 */
public final class CommandHistory {
    /** The brief requires at least 20; older entries are dropped beyond this. */
    public static final int CAPACITY = 50;

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    /** Runs a command against the current state. If it throws, nothing is recorded. */
    public AppState execute(Command command, AppState current) {
        AppState next = command.execute(current);
        undoStack.push(command);
        if (undoStack.size() > CAPACITY) {
            undoStack.removeLast();
        }
        redoStack.clear();
        return next;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public Optional<String> nextUndo() {
        return Optional.ofNullable(undoStack.peek()).map(Command::description);
    }

    public Optional<String> nextRedo() {
        return Optional.ofNullable(redoStack.peek()).map(Command::description);
    }

    /** The state before the most recent command. */
    public AppState undo() {
        if (undoStack.isEmpty()) {
            throw new IllegalStateException("Nothing to undo.");
        }
        Command command = undoStack.pop();
        AppState previous = command.undo();
        redoStack.push(command);
        return previous;
    }

    /** The state after the most recently undone command. */
    public AppState redo() {
        if (redoStack.isEmpty()) {
            throw new IllegalStateException("Nothing to redo.");
        }
        Command command = redoStack.pop();
        AppState next = command.redo();
        undoStack.push(command);
        return next;
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
