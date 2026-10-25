package campusplan.persistence;

import campusplan.application.CampusPlanService;
import campusplan.domain.AppState;
import campusplan.domain.Availability;
import campusplan.domain.DraftEvent;
import campusplan.domain.Feature;
import campusplan.domain.Room;
import campusplan.domain.Student;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class JsonStorageTest {
    @TempDir Path directory;

    private AppState example() {
        return new AppState(List.of(new Student("s1", "Zoë Lee", "zoe@example.test"),
                new Student("s2", "Alex Park", "alex@example.test")),
                List.of(new Room("r1", "Cedar", 6, Set.of(Feature.WHITEBOARD))),
                List.of(new Availability("s1", LocalDate.of(2026, 10, 24),
                        LocalTime.of(8, 0), LocalTime.of(22, 0))),
                List.of(new DraftEvent("e1", "Project meeting", List.of("s1", "s2"),
                        LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 30),
                        180, Set.of(Feature.WHITEBOARD), 2)));
    }

    @Test void missingFileStartsEmptyWithoutCreatingAFile() throws IOException {
        Path path = directory.resolve("missing.json");
        assertTrue(new JsonStorage(path).load().isEmpty());
        assertFalse(Files.exists(path));
    }

    @Test void jsonRoundTripPreservesEveryCollectionIdsDatesAndUtf8() throws IOException {
        Path path = directory.resolve("nested/state.json");
        new JsonStorage(path).save(example());
        assertEquals(example(), new JsonStorage(path).load());
        String json = Files.readString(path, StandardCharsets.UTF_8);
        assertTrue(json.contains("Zoë"));
        assertTrue(json.contains("2026-10-24"));
        assertFalse(json.contains("\"empty\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{broken", "null", "{}", "[]", "{\"students\":null}",
            "{\"students\":[],\"rooms\":[],\"availability\":[],\"events\":[]} {}",
            "{\"students\":[],\"students\":[],\"rooms\":[],\"availability\":[],\"events\":[]}"})
    void corruptStorageIsReportedAndNeverOverwritten(String invalid) throws IOException {
        Path path = directory.resolve("bad.json");
        Files.writeString(path, invalid);
        JsonStorage storage = new JsonStorage(path);
        assertThrows(IOException.class, storage::load);
        assertThrows(IOException.class, () -> storage.save(example()));
        assertEquals(invalid, Files.readString(path));
    }

    @Test void loadedDataMustSatisfyDomainAndReferenceRules() throws IOException {
        Path path = directory.resolve("bad-domain.json");
        JsonStorage storage = new JsonStorage(path);
        storage.save(example());
        String valid = Files.readString(path);
        for (String invalid : List.of(valid.replace("\"capacity\" : 6", "\"capacity\" : 101"),
                valid.replace("\"studentId\" : \"s1\"", "\"studentId\" : \"unknown\""),
                valid.replace("\"durationMinutes\" : 180", "\"durationMinutes\" : null"))) {
            assertNotEquals(valid, invalid);
            Files.writeString(path, invalid);
            assertThrows(IOException.class, storage::load);
            assertEquals(invalid, Files.readString(path));
        }
    }

    @Test void failedAtomicReplacementKeepsPreviousValidBytesAndRemovesTemporaryFile() throws IOException {
        Path path = directory.resolve("state.json");
        new JsonStorage(path).save(example());
        byte[] before = Files.readAllBytes(path);
        JsonStorage failing = new JsonStorage(path) {
            @Override protected void replaceFile(Path temporary) throws IOException {
                throw new IOException("Simulated disk failure");
            }
        };
        assertThrows(IOException.class, () -> failing.save(AppState.empty()));
        assertArrayEquals(before, Files.readAllBytes(path));
        assertEquals(example(), new JsonStorage(path).load());
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test void serviceToFileRestartPreservesNewStudentAndDraftEdit() throws IOException {
        Path path = directory.resolve("state.json");
        CampusPlanService first = new CampusPlanService(new JsonStorage(path));
        first.loadSeed(example());
        Student student = first.createStudent("Demo Student", "demo@example.test");
        first.editDraft("e1", "Updated", List.of("s1", student.id()),
                LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 24), 30, Set.of(), 2);
        first.save();
        CampusPlanService second = new CampusPlanService(new JsonStorage(path));
        assertEquals(first.state(), second.state());
        assertEquals(student, second.student(student.id()));
        assertEquals("Updated", second.event("e1").title());
    }
}
