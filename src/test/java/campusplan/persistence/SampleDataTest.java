package campusplan.persistence;

import java.nio.file.Path;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import campusplan.domain.AppState;
import campusplan.domain.AttendanceResponse;
import campusplan.domain.Availability;
import campusplan.domain.EventStatus;

class SampleDataTest {
    /**
     * Tests that the sample data files contain the expected records and that the events have the correct status and responses.
     * Starts by loading seed.json.
     * Then checks that the number of records (students and rooms and such).
     * Then verify everything about the events (status, invitees, responses).
     * Then load no-result data.
     * Verify that the no-result data has the same students, rooms, and events as the seed data, but has no availability records.
     */
    @Test
    void sampleFilesContainTheRequiredRecords() throws Exception {
        AppState seed = new JsonStorage(Path.of("sample-data", "seed.json")).load();
        assertEquals(12, seed.students().size());
        assertEquals(6, seed.rooms().size());
        assertEquals(7, seed.availability().stream().map(Availability::date).distinct().count());
        assertEquals(3, seed.events().size());

        for (var event : seed.events()) {
            assertEquals(EventStatus.DRAFT, event.status());
            assertEquals(new HashSet<>(event.inviteeIds()), event.responses().keySet());
            assertTrue(event.responses().values().stream()
                    .allMatch(response -> response == AttendanceResponse.PENDING));
        }

        AppState noResult = new JsonStorage(Path.of("sample-data", "no-result.json")).load();
        assertEquals(seed.students(), noResult.students());
        assertEquals(seed.rooms(), noResult.rooms());
        assertEquals(seed.events(), noResult.events());
        assertTrue(noResult.availability().isEmpty());
    }
}