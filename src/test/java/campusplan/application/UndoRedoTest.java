package campusplan.application;

import campusplan.domain.AppState;
import campusplan.domain.AttendanceResponse;
import campusplan.domain.DraftEvent;
import campusplan.domain.EventStatus;
import campusplan.persistence.InMemoryStorage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static campusplan.application.scheduling.PlanFixture.*;
import static org.junit.jupiter.api.Assertions.*;

/** Undo and redo of user mutations (FR-08). */
class UndoRedoTest {
    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final Clock CLOCK = Clock.fixed(LocalDate.of(2026, 11, 15).atStartOfDay(ZoneOffset.UTC).toInstant(),
            ZoneOffset.UTC);

    private InMemoryStorage storage;

    private CampusPlanService service() throws IOException {
        AppState state = state(students(6), List.of(room("R1", 6), room("R2", 8)),
                freeAll(ids(1, 5), DAY, "09:00", "12:00"), List.of());
        storage = new InMemoryStorage(state);
        return new CampusPlanService(storage, CLOCK);
    }

    private DraftEvent draft(CampusPlanService service, String title) {
        return service.createDraft(title, ids(1, 6), DAY, DAY, 60, Set.of(), 4);
    }

    @Test
    void undoAfterSchedulingRestoresTheDraftAndFreesTheRoomThenRedoReappliesIt() throws IOException {
        CampusPlanService service = service();
        DraftEvent created = draft(service, "Study");
        AppState draftState = service.state();
        DraftEvent scheduled = service.confirmSuggestion(created.id(), "R1", DAY, NINE);
        AppState scheduledState = service.state();

        assertTrue(service.undo().startsWith("Schedule Study"));
        assertSame(draftState, service.state());
        assertEquals(EventStatus.DRAFT, service.event(created.id()).status());
        assertNull(service.event(created.id()).roomId());
        // Redo puts back exactly what the original scheduling produced.
        assertTrue(service.redo().startsWith("Schedule Study"));
        assertSame(scheduledState, service.state());
        assertEquals(scheduled, service.event(created.id()));

        // After another undo the room is genuinely free: a different event can book the same slot.
        service.undo();
        DraftEvent other = draft(service, "Other");
        service.confirmSuggestion(other.id(), "R1", DAY, NINE);
        assertEquals("R1", service.event(other.id()).roomId());
        assertEquals(EventStatus.DRAFT, service.event(created.id()).status());
    }

    @Test
    void undoRestoresReservationsAndResponsesTogether() throws IOException {
        CampusPlanService service = service();
        DraftEvent created = draft(service, "Study");
        service.confirmSuggestion(created.id(), "R1", DAY, NINE);
        service.recordResponse(created.id(), "s1", AttendanceResponse.ACCEPTED);
        AppState beforeCancel = service.state();
        service.cancelEvent(created.id(), true);
        assertEquals(EventStatus.CANCELLED, service.event(created.id()).status());

        service.undo(); // undo the cancellation
        assertSame(beforeCancel, service.state());
        DraftEvent restored = service.event(created.id());
        assertEquals(EventStatus.SCHEDULED, restored.status());
        assertEquals("R1", restored.roomId());
        assertEquals(AttendanceResponse.ACCEPTED, restored.responses().get("s1"));

        service.undo(); // undo the response
        assertEquals(AttendanceResponse.PENDING, service.event(created.id()).responses().get("s1"));
        assertEquals(EventStatus.SCHEDULED, service.event(created.id()).status());
    }

    @Test
    void redoKeepsTheSameGeneratedIds() throws IOException {
        CampusPlanService service = service();
        var student = service.createStudent("New Person", "new@campus.test");
        service.undo();
        assertTrue(service.state().students().stream().noneMatch(s -> s.id().equals(student.id())));
        service.redo();
        assertEquals(student, service.student(student.id()));
    }

    @Test
    void aNewMutationAfterUndoClearsRedo() throws IOException {
        CampusPlanService service = service();
        draft(service, "One");
        service.undo();
        assertTrue(service.canRedo());
        draft(service, "Two");
        assertFalse(service.canRedo());
        AppState before = service.state();
        assertThrows(IllegalArgumentException.class, service::redo);
        assertSame(before, service.state());
    }

    @Test
    void failedOperationsAndExportsDoNotEnterHistory() throws IOException {
        CampusPlanService service = service();
        DraftEvent created = draft(service, "Study");
        DraftEvent other = draft(service, "Other");
        service.confirmSuggestion(created.id(), "R1", DAY, NINE);
        // A rejected confirmation, a rejected response, an export and a suggestion search change nothing...
        assertThrows(IllegalArgumentException.class, () -> service.confirmSuggestion(other.id(), "R1", DAY, NINE));
        assertThrows(IllegalArgumentException.class, () -> service.recordResponse(created.id(), "s6",
                AttendanceResponse.ACCEPTED));
        service.rosterCsv(created.id());
        service.suggestions(other.id());
        service.listEvents(null, null);
        // ...so the next undo reverses the successful scheduling, not any of them.
        assertTrue(service.undo().startsWith("Schedule Study"));
        assertEquals(EventStatus.DRAFT, service.event(created.id()).status());
    }

    @Test
    void repeatingTheCurrentResponseIsNotAnUndoStep() throws IOException {
        CampusPlanService service = service();
        DraftEvent created = draft(service, "Study");
        service.confirmSuggestion(created.id(), "R1", DAY, NINE);
        service.recordResponse(created.id(), "s1", AttendanceResponse.PENDING);
        assertTrue(service.undo().startsWith("Schedule Study"));
    }

    @Test
    void atLeastTwentyOperationsCanBeUndoneAndRedone() throws IOException {
        CampusPlanService service = service();
        for (int i = 1; i <= 25; i++) {
            service.createStudent("Extra " + i, "extra" + i + "@campus.test");
        }
        assertEquals(31, service.state().students().size());
        for (int i = 25; i >= 1; i--) {
            assertEquals("Create student Extra " + i, service.undo());
        }
        assertEquals(6, service.state().students().size());
        assertFalse(service.canUndo());
        for (int i = 1; i <= 25; i++) {
            assertEquals("Create student Extra " + i, service.redo());
        }
        assertEquals(31, service.state().students().size());
    }

    @Test
    void historyKeepsTheMostRecentFiftyChanges() throws IOException {
        CampusPlanService service = service();
        for (int i = 1; i <= 60; i++) {
            service.createStudent("Extra " + i, "extra" + i + "@campus.test");
        }
        int undone = 0;
        while (service.canUndo()) {
            service.undo();
            undone++;
        }
        assertEquals(50, undone);
        assertEquals(16, service.state().students().size());
    }

    @Test
    void savingAfterUndoPersistsTheRestoredState() throws IOException {
        CampusPlanService service = service();
        DraftEvent created = draft(service, "Study");
        service.save();
        service.confirmSuggestion(created.id(), "R1", DAY, NINE);
        service.save();
        assertEquals(EventStatus.SCHEDULED, storage.load().events().getFirst().status());
        service.undo();
        service.save();
        AppState persisted = storage.load();
        assertEquals(EventStatus.DRAFT, persisted.events().getFirst().status());
        assertNull(persisted.events().getFirst().roomId());
        assertSame(service.state(), persisted);
    }

    @Test
    void loadingSeedDataIsNotUndoableAndClearsOldHistory() throws IOException {
        storage = new InMemoryStorage();
        CampusPlanService service = new CampusPlanService(storage, CLOCK);
        service.createStudent("Temp", "temp@campus.test");
        service.undo();
        assertTrue(service.canRedo());
        AppState seed = state(students(3), List.of(room("R1", 6)), List.of(), List.of());
        service.loadSeed(seed);
        assertSame(seed, service.state());
        assertFalse(service.canUndo());
        assertFalse(service.canRedo());
        assertThrows(IllegalArgumentException.class, service::undo);
        assertThrows(IllegalArgumentException.class, service::redo);
        assertSame(seed, service.state());
    }

    @Test
    void undoAndRedoWithNothingToDoReportAnErrorAndChangeNothing() throws IOException {
        CampusPlanService service = service();
        AppState before = service.state();
        assertFalse(service.canUndo());
        assertThrows(IllegalArgumentException.class, service::undo);
        assertThrows(IllegalArgumentException.class, service::redo);
        assertSame(before, service.state());
    }

    @Test
    void availabilityAndStudentEditsAreUndoableToo() throws IOException {
        CampusPlanService service = service();
        AppState start = service.state();
        service.setAvailability("s6", DAY, List.of(free("s6", DAY, "10:00", "11:00")));
        service.editStudent("s1", "Renamed", "s1@campus.test");
        service.undo();
        service.undo();
        assertSame(start, service.state());
    }
}
