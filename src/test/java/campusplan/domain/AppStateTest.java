package campusplan.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AppStateTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 26);

    @Test
    void stateMergesOverlappingAdjacentAndContainedIntervalsWithoutCrossingDatesOrStudents() {
        List<Availability> input = List.of(
                interval("s1", DAY, "10:00", "11:00"), interval("s1", DAY, "08:00", "09:00"),
                interval("s1", DAY, "08:30", "10:00"), interval("s1", DAY, "09:00", "09:30"),
                interval("s1", DAY, "12:00", "13:00"), interval("s1", DAY.plusDays(1), "08:00", "09:00"),
                interval("s2", DAY, "08:00", "09:00"));
        AppState state = new AppState(students(), List.of(), input, List.of());
        assertEquals(List.of(interval("s1", DAY, "08:00", "11:00"), interval("s1", DAY, "12:00", "13:00"),
                interval("s1", DAY.plusDays(1), "08:00", "09:00"), interval("s2", DAY, "08:00", "09:00")), state.availability());
        assertTrue(state.availability().getFirst().covers(DAY, LocalTime.of(8, 30), LocalTime.of(10, 30)));
    }

    @Test
    void stateRejectsDuplicatesAndUnknownReferences() {
        Student first = students().getFirst();
        assertThrows(IllegalArgumentException.class,
                () -> new AppState(List.of(first, first), List.of(), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new AppState(List.of(first, new Student("s3", "Other", " A@B ")), List.of(), List.of(), List.of()));
        Room room = new Room("r1", "Room", 6, Set.of());
        assertThrows(IllegalArgumentException.class,
                () -> new AppState(List.of(), List.of(room, room), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new AppState(List.of(), List.of(room, new Room("r2", " ROOM ", 5, Set.of())), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new AppState(List.of(), List.of(),
                List.of(interval("missing", DAY, "08:00", "09:00")), List.of()));
        DraftEvent event = draft(List.of("s1", "s2"), DAY, 60, 2);
        assertThrows(IllegalArgumentException.class,
                () -> new AppState(List.of(first), List.of(), List.of(), List.of(event)));
        assertThrows(IllegalArgumentException.class,
                () -> new AppState(students(), List.of(), List.of(), List.of(event, event)));
    }

    @Test
    void stateCopiesListsAndEmptyMeansNoDomainData() {
        List<Student> source = new ArrayList<>(students());
        AppState state = new AppState(source, List.of(), List.of(), List.of());
        source.clear();
        assertEquals(2, state.students().size());
        assertThrows(UnsupportedOperationException.class, () -> state.students().clear());
        assertTrue(AppState.empty().isEmpty());
        assertFalse(state.isEmpty());
        assertFalse(new AppState(List.of(), List.of(new Room("r1", "Room", 1, Set.of())), List.of(), List.of()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new AppState(null, List.of(), List.of(), List.of()));
        List<Student> invalid = new ArrayList<>();
        invalid.add(null);
        assertThrows(IllegalArgumentException.class, () -> new AppState(invalid, List.of(), List.of(), List.of()));
    }

    private static List<Student> students() {
        return List.of(new Student("s1", "Alex", "a@b"), new Student("s2", "Sam", "s@b"));
    }

    private static Availability interval(String student, LocalDate day, String start, String end) {
        return new Availability(student, day, LocalTime.parse(start), LocalTime.parse(end));
    }

    private static DraftEvent draft(List<String> ids, LocalDate end, int duration, int minimum) {
        return new DraftEvent("e1", "Study", ids, DAY, end, duration, Set.of(), minimum);
    }
}