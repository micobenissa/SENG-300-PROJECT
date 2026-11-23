package campusplan.persistence;

import campusplan.domain.AppState;
import campusplan.domain.AttendanceResponse;
import campusplan.domain.Availability;
import campusplan.domain.DraftEvent;
import campusplan.domain.EventStatus;
import campusplan.domain.Room;
import campusplan.domain.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

class ScheduledStorageTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 16);
    @TempDir Path directory;

    /**
     * This file will create sample application data to be used by other tests. It creates:
     *  - 2 students
     *  - two rooms
     *  - set availability
     *  - three events
     * 
     *  @return - app state to test with
     */
    private static AppState scheduledState() {
        List<Student> students = List.of(new Student("s1", "Zoë Ångström", "zoe@x.ca"), new Student("s2", "Sam", "sam@x.ca"));

        List<Room> rooms = List.of(new Room("R1", "Cedar", 4, Set.of()), new Room("R2", "Maple", 4, Set.of()));

        List<Availability> availability = List.of(new Availability("s1", DAY, LocalTime.of(9, 0), LocalTime.of(11, 0)));

        DraftEvent draft = new DraftEvent("e0", "Plain draft", List.of("s1", "s2"), DAY, DAY, 30, Set.of(), 2);

        DraftEvent scheduled = new DraftEvent("e1", "Study, \"group\"", List.of("s1", "s2"), DAY, DAY, 60, Set.of(), 2)
                .scheduledAt("R1", DAY, LocalTime.of(9, 0)).withResponse("s1", AttendanceResponse.ACCEPTED)
                .withResponse("s2", AttendanceResponse.DECLINED);

        DraftEvent cancelled = new DraftEvent("e2", "Gone", List.of("s1", "s2"), DAY, DAY, 60, Set.of(), 2)
                .scheduledAt("R2", DAY, LocalTime.of(9, 0)).withResponse("s2", AttendanceResponse.DECLINED).cancelled();

        return new AppState(students, rooms, availability, List.of(draft, scheduled, cancelled));
    }

    /**
     * Tests whether scheduled and cancelled events are saved and loaded properly.
     * Also makes sure that attendence responses, room assignments, event times, and student names are preserved.
     * 
     * @throws IOException - if it fails
     */
    @Test
    void scheduledAndCancelledEventsRoundTripWithResponsesRoomsAndTimes() throws IOException {
        JsonStorage storage = new JsonStorage(directory.resolve("data.json"));
        AppState original = scheduledState();
        storage.save(original);
        AppState reloaded = new JsonStorage(directory.resolve("data.json")).load();
        assertEquals(original, reloaded);
        DraftEvent scheduled = reloaded.events().get(1);
        assertEquals(EventStatus.SCHEDULED, scheduled.status());
        assertEquals("R1", scheduled.roomId());
        assertEquals(LocalTime.of(10, 0), scheduled.endTime());
        assertEquals(AttendanceResponse.ACCEPTED, scheduled.responses().get("s1"));
        assertEquals(EventStatus.CANCELLED, reloaded.events().get(2).status());
        assertEquals(AttendanceResponse.DECLINED, reloaded.events().get(2).responses().get("s2"));
        // The file really is UTF-8 and keeps non-ASCII names readable.
        assertTrue(Files.readString(directory.resolve("data.json"), StandardCharsets.UTF_8).contains("Zoë Ångström"));
    }


    /**
     * Tests that a P1 JSON file without scheduling fields can still be loaded.
     * Basically just makes sure that P1 data still works in P2 implementation
     * 
     * @throws IOException - if it fails
     */
    @Test
    void aP1FileWithoutSchedulingFieldsStillLoadsAsDrafts() throws IOException {
        String p1 = """
                {"students":[{"id":"s1","name":"A","email":"a@x.ca"},{"id":"s2","name":"B","email":"b@x.ca"}],
                 "rooms":[{"id":"R1","name":"Cedar","capacity":4,"features":[]}],
                 "availability":[],
                 "events":[{"id":"e1","title":"Old draft","inviteeIds":["s1","s2"],"startDate":"2026-10-24",
                  "endDate":"2026-10-24","durationMinutes":60,"requiredFeatures":[],"minimumAttendance":2,
                  "status":"DRAFT","responses":{"s1":"PENDING","s2":"PENDING"}}]}
                """;
        Path path = directory.resolve("p1.json");
        Files.writeString(path, p1, StandardCharsets.UTF_8);
        AppState loaded = new JsonStorage(path).load();
        DraftEvent event = loaded.events().getFirst();
        assertEquals(EventStatus.DRAFT, event.status());
        assertNull(event.roomId());
        assertNull(event.startTime());
    }

    /**
     * Tests that conflicting room reservations are detected
     * Also checks that a file is not overwritte when a conflict is found
     * 
     * @throws IOException - if it fails
     */
    @Test
    void conflictingReservationsInAFileAreReportedAndTheFileIsNotOverwritten() throws IOException {
        Path path = directory.resolve("conflict.json");
        new JsonStorage(path).save(scheduledState());
        // Turn the cancelled event into a second SCHEDULED event in the same room and time as e1.
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var tree = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(path.toFile());
        var twin = (com.fasterxml.jackson.databind.node.ObjectNode) tree.get("events").get(2);
        twin.put("status", "SCHEDULED").put("roomId", "R1").put("scheduledDate", "2026-11-16")
                .put("startTime", "09:00").put("endTime", "10:00");
        mapper.writeValue(path.toFile(), tree);
        byte[] before = Files.readAllBytes(path);
        IOException error = assertThrows(IOException.class, () -> new JsonStorage(path).load());
        assertTrue(error.getMessage().contains("The file was not changed"));
        assertTrue(error.getMessage().contains("reserve the same room"), error.getMessage());
        assertThrows(IOException.class, () -> new JsonStorage(path).save(AppState.empty()));
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    /**
     * Tests that an event with a status inconsistent with its scheduling information is rejected when loading a JSON file.
     * 
     * @throws IOException - if fails
     */
    @Test
    void anEventWithAnInconsistentStatusIsRejectedAsCorrupt() throws IOException {
        Path path = directory.resolve("bad.json");
        new JsonStorage(path).save(scheduledState());
        String bad = Files.readString(path).replaceFirst("\"status\" : \"DRAFT\"", "\"status\" : \"SCHEDULED\"");
        Files.writeString(path, bad, StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> new JsonStorage(path).load());
    }

}