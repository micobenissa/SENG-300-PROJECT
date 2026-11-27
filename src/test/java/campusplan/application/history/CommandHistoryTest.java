package campusplan.application.history;

import campusplan.domain.AppState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static campusplan.application.scheduling.PlanFixture.students;
import static org.junit.jupiter.api.Assertions.*;

/** The Command pattern participants on their own, without the service. */
class CommandHistoryTest {
    private static AppState state(int students) {
        return AppState.empty().withStudents(students(students));
    }

    @Test
    void commandsRunUndoAndRedoAgainstTheStatesTheyWereGiven() {
        CommandHistory history = new CommandHistory();
        AppState zero = AppState.empty();
        AppState one = state(1);
        AppState two = state(2);
        AppState current = history.execute(new StateChangeCommand("add one", one), zero);
        current = history.execute(new StateChangeCommand("add two", two), current);
        assertSame(two, current);
        assertEquals("add two", history.nextUndo().orElseThrow());
        assertSame(one, history.undo());
        assertEquals("add two", history.nextRedo().orElseThrow());
        assertSame(zero, history.undo());
        assertFalse(history.canUndo());
        assertSame(one, history.redo());
        assertSame(two, history.redo());
        assertFalse(history.canRedo());
    }

    @Test
    void executingANewCommandClearsRedo() {
        CommandHistory history = new CommandHistory();
        history.execute(new StateChangeCommand("a", state(1)), AppState.empty());
        history.undo();
        assertTrue(history.canRedo());
        history.execute(new StateChangeCommand("b", state(2)), AppState.empty());
        assertFalse(history.canRedo());
    }

    @Test
    void aCommandThatFailsIsNotRecorded() {
        CommandHistory history = new CommandHistory();
        Command failing = new Command() {
            @Override public String description() { return "fails"; }
            @Override public AppState execute(AppState current) { throw new IllegalArgumentException("no"); }
            @Override public AppState undo() { return null; }
            @Override public AppState redo() { return null; }
        };
        assertThrows(IllegalArgumentException.class, () -> history.execute(failing, AppState.empty()));
        assertFalse(history.canUndo());
    }

    @Test
    void oldestEntriesAreDroppedBeyondTheCapacityAndClearEmptiesBothStacks() {
        CommandHistory history = new CommandHistory();
        AppState current = AppState.empty();
        for (int i = 0; i < CommandHistory.CAPACITY + 5; i++) {
            current = history.execute(new StateChangeCommand("c" + i, state(2)), current);
        }
        int undone = 0;
        while (history.canUndo()) {
            history.undo();
            undone++;
        }
        assertEquals(CommandHistory.CAPACITY, undone);
        assertTrue(CommandHistory.CAPACITY >= 20);
        history.clear();
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
        assertThrows(IllegalStateException.class, history::undo);
        assertThrows(IllegalStateException.class, history::redo);
    }

    @Test
    void undoBeforeExecuteIsAnError() {
        assertThrows(IllegalStateException.class, () -> new StateChangeCommand("x", state(1)).undo());
        assertEquals(List.of(), AppState.empty().students());
    }
}
