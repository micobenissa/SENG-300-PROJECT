package campusplan.cli;

import campusplan.application.CampusPlanService;
import campusplan.domain.AppState;
import campusplan.domain.Feature;
import campusplan.domain.Room;
import campusplan.domain.Student;
import campusplan.persistence.Storage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleAppTest {
    @Test
    void workingSliceCreatesRejectsDuplicateMergesFiltersCreatesDraftAndReloads() throws IOException {
        // start empty so loading the samples is part of the test too
        var storage = new TestStorage(AppState.empty());
        var service = new CampusPlanService(storage);
        // these are the answers to the menus, in the same order a user would enter them
        String output = run(service, sample(), """
                5
                1
                2
                Casey Lane
                casey@example.test
                1
                2
                Duplicate
                CASEY@example.test
                3
                2
                1
                2026-10-24
                2
                09:00
                10:00
                10:00
                11:00
                2
                2
                4
                WHITEBOARD
                4
                2
                Project sync
                1,2
                2026-10-24
                2026-10-25
                60
                WHITEBOARD
                2
                6
                0
                """);
        // check the results shown after student, availability, room and draft actions
        assertTrue(output.contains("Student created:"));
        assertTrue(output.contains("must be unique"));
        assertTrue(output.contains("09:00-11:00"));
        assertTrue(output.contains("Maple | 6 seats | WHITEBOARD"));
        assertTrue(output.contains("Status: DRAFT"));
        assertTrue(output.contains("Alex Morgan | PENDING"));
        assertTrue(output.contains("Saved."));
        // opening a new service checks what actually reached storage
        var reloaded = new CampusPlanService(storage);
        assertEquals(3, reloaded.state().students().size());
        assertEquals(1, reloaded.state().availability().size());
        assertEquals(LocalTime.of(11, 0), reloaded.state().availability().getFirst().end());
        assertEquals("Project sync", reloaded.state().events().getFirst().title());
        assertEquals(service.state(), reloaded.state());
    }

    @Test
    void editSearchAndDetailsUseCurrentRecordsAndPreserveIds() throws IOException {
        var storage = new TestStorage(sample().load());
        var service = new CampusPlanService(storage);
        service.createDraft("Original", List.of("s1", "s2"), LocalDate.of(2026, 10, 24),
                LocalDate.of(2026, 10, 24), 30, Set.of(), 2);
        // keep the original id so the edit cannot silently replace the record
        String eventId = service.state().events().getFirst().id();
        // the blank email keeps the old email while the name and draft fields change
        String output = run(service, sample(), """
                1
                3
                1
                Alex Changed

                1
                4
                CHANGED
                4
                3
                1
                Updated meeting
                1,2
                2026-10-25
                2026-10-25
                90

                2
                4
                4
                1
                0
                yes
                """);
        assertEquals("s1", service.state().students().getFirst().id());
        assertEquals("alex@example.test", service.state().students().getFirst().email());
        assertEquals(eventId, service.state().events().getFirst().id());
        assertEquals("Updated meeting", service.state().events().getFirst().title());
        assertTrue(output.contains("Alex Changed | alex@example.test"));
        assertTrue(output.contains("Duration: 90 minutes"));
        assertTrue(output.contains("Required features: none"));
    }

    @Test
    void badInputReturnsToMenuAndDoesNotPartiallyChangeRecords() throws IOException {
        var service = new CampusPlanService(new TestStorage(sample().load()));
        AppState original = service.state();
        // try bad menu, number, feature, date, time and event selections
        String output = run(service, sample(), """
                unknown
                2
                2
                lots
                2
                2
                4
                POOL
                3
                2
                1
                2026-02-30
                3
                2
                1
                2026-10-24
                1
                09:15
                10:00
                4
                4
                99
                0
                """);
        assertTrue(output.contains("Choose a listed option."));
        assertTrue(output.contains("must be a whole number"));
        assertTrue(output.contains("Unknown feature: POOL"));
        assertTrue(output.contains("valid date"));
        assertTrue(output.contains("Error:"));
        assertTrue(output.contains("Choose a listed event number."));
        // every failed form should leave the original records untouched
        assertEquals(original, service.state());
    }

    @Test
    void eofSavesCompletedChangesAndDoesNotLoop() throws IOException {
        var storage = new TestStorage(AppState.empty());
        var service = new CampusPlanService(storage);
        // stop giving input straight after the student form is complete
        String output = run(service, sample(), "1\n2\nAlex\nalex@example.test\n");
        assertEquals(1, storage.value.students().size());
        assertTrue(output.contains("Saving pending changes before closing."));
        assertTrue(output.endsWith("CampusPlan closed." + System.lineSeparator()));
    }

    @Test
    void eofDuringFormLeavesStateUntouched() throws IOException {
        var storage = new TestStorage(AppState.empty());
        var service = new CampusPlanService(storage);
        // the email never arrives, so this should not create half a student record
        String output = run(service, sample(), "1\n2\nUnfinished name\n");
        assertTrue(service.state().isEmpty());
        assertTrue(storage.value.isEmpty());
        assertTrue(output.contains("Input ended."));
    }

    @Test
    void failedSaveKeepsChangesAndExitCanBeCancelled() throws IOException {
        var storage = new TestStorage(AppState.empty());
        storage.failSave = true;
        var service = new CampusPlanService(storage);
        // fail a save, cancel exiting once, then choose to leave without saving
        String output = run(service, sample(), "1\n2\nAlex\nalex@example.test\n6\n0\ncancel\n0\nno\n");
        assertTrue(output.contains("Storage error: disk full"));
        assertTrue(output.contains("Changes are still in memory."));
        assertTrue(service.state().students().size() == 1);
        // the edit is still in memory, but the failed save did not reach storage
        assertTrue(storage.value.isEmpty());
    }

    @Test
    void seedCannotReplaceExistingDataAndMissingSeedIsReported() throws IOException {
        var service = new CampusPlanService(new TestStorage(sample().load()));
        AppState original = service.state();
        // loading samples must not wipe records already in the session
        assertTrue(run(service, sample(), "5\n0\n").contains("only be loaded into an empty application"));
        assertEquals(original, service.state());
        // also check the message when the sample source has nothing to load
        var empty = new CampusPlanService(new TestStorage(AppState.empty()));
        String output = run(empty, new TestStorage(AppState.empty()), "5\n0\n");
        assertTrue(output.contains("sample file is missing"));
        assertTrue(empty.state().isEmpty());
        assertFalse(output.contains("Sample data loaded."));
    }

    private TestStorage sample() {
        // two students and one room are enough for the menu scenarios above
        return new TestStorage(new AppState(List.of(
                new Student("s1", "Alex Morgan", "alex@example.test"),
                new Student("s2", "Blair Chen", "blair@example.test")),
                List.of(new Room("r1", "Maple", 6, Set.of(Feature.WHITEBOARD))),
                List.of(), List.of()));
    }

    private String run(CampusPlanService service, Storage seed, String commands) throws IOException {
        // capture the menu output while feeding it the supplied answers
        var output = new StringWriter();
        new ConsoleApp(service, seed, new StringReader(commands), new PrintWriter(output)).run();
        return output.toString();
    }

    private static final class TestStorage implements Storage {
        // keep the last saved state separate from unsaved edits in the service
        private AppState value;
        private boolean failSave;

        private TestStorage(AppState value) {
            this.value = value;
        }

        @Override
        public AppState load() {
            return value;
        }

        @Override
        public void save(AppState state) throws IOException {
            // fail before changing the saved state, like a write that did not succeed
            if (failSave) {
                throw new IOException("disk full");
            }
            value = state;
        }
    }
}
