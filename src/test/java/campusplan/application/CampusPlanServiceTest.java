package campusplan.application;

import campusplan.domain.AppState;
import campusplan.domain.Availability;
import campusplan.domain.DraftEvent;
import campusplan.domain.Feature;
import campusplan.domain.Room;
import campusplan.domain.Student;
import campusplan.persistence.Storage;
import campusplan.persistence.InMemoryStorage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CampusPlanServiceTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 26);

    @Test
    void createsEditsSearchesAndPreservesStudentId() throws IOException {
        CampusPlanService service = new CampusPlanService(new TestStorage());
        Student alex = service.createStudent(" Alex Reed ", " ALEX@CAMPUS.TEST ");
        Student sam = service.createStudent("Sam Park", "sam@campus.test");
        assertNotEquals(alex.id(), sam.id());
        assertEquals(List.of(alex), service.searchStudents(" rEEd "));
        assertEquals(List.of(alex), service.searchStudents("ALEX@"));
        assertEquals(2, service.searchStudents("").size());
        assertTrue(service.searchStudents("missing").isEmpty());
        Student edited = service.editStudent(alex.id(), "Alex Lane", "lane@campus.test");
        assertEquals(alex.id(), edited.id());
        assertEquals(edited, service.student(alex.id()));
        assertEquals(2, service.state().students().size());
        assertThrows(IllegalArgumentException.class, () -> service.searchStudents(null));
        assertThrows(IllegalArgumentException.class, () -> service.student("missing"));
    }

    @Test
    void duplicateOrInvalidStudentMutationsLeaveStateUnchanged() throws IOException {
        CampusPlanService service = new CampusPlanService(new TestStorage());
        Student alex = service.createStudent("Alex", "a@b");
        Student sam = service.createStudent("Sam", "s@b");
        AppState before = service.state();
        assertThrows(IllegalArgumentException.class, () -> service.createStudent("Other", " A@B "));
        assertSame(before, service.state());
        assertThrows(IllegalArgumentException.class, () -> service.editStudent(sam.id(), "Sam", alex.email()));
        assertSame(before, service.state());
        assertThrows(IllegalArgumentException.class, () -> service.editStudent(alex.id(), " ", "a@b"));
        assertThrows(IllegalArgumentException.class, () -> service.editStudent("missing", "Alex", "a@b"));
        assertSame(before, service.state());
        assertEquals(alex, service.editStudent(alex.id(), "Alex", " A@B "));
    }

    @Test
    void filtersRoomsByCapacityAndAllRequiredFeatures() throws IOException {
        TestStorage storage = new TestStorage();
        storage.saved = new AppState(List.of(), List.of(
                new Room("r1", "Small", 5, Set.of(Feature.WHITEBOARD)),
                new Room("r2", "Medium", 6, Set.of(Feature.WHITEBOARD, Feature.ACCESSIBLE)),
                new Room("r3", "Large", 20, Set.of(Feature.PROJECTOR))), List.of(), List.of());
        CampusPlanService service = new CampusPlanService(storage);
        assertEquals(List.of("r2"), service.filterRooms(6, Set.of(Feature.WHITEBOARD, Feature.ACCESSIBLE))
                .stream().map(Room::id).toList());
        assertEquals(3, service.filterRooms(1, Set.of()).size());
        assertTrue(service.filterRooms(21, Set.of()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.filterRooms(0, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> service.filterRooms(101, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> service.filterRooms(1, null));
        Set<Feature> invalid = new java.util.HashSet<>();
        invalid.add(null);
        assertThrows(IllegalArgumentException.class, () -> service.filterRooms(1, invalid));
    }

    @Test
    void replacesOneDaysAvailabilityAndMergesAdjacentIntervals() throws IOException {
        CampusPlanService service = new CampusPlanService(new TestStorage());
        Student alex = service.createStudent("Alex", "a@b");
        Student sam = service.createStudent("Sam", "s@b");
        service.setAvailability(alex.id(), DAY.plusDays(1), List.of(interval(alex.id(), DAY.plusDays(1), 9, 10)));
        service.setAvailability(sam.id(), DAY, List.of(interval(sam.id(), DAY, 9, 10)));
        List<Availability> merged = service.setAvailability(alex.id(), DAY, List.of(
                interval(alex.id(), DAY, 10, 12), interval(alex.id(), DAY, 8, 10), interval(alex.id(), DAY, 9, 11)));
        assertEquals(List.of(interval(alex.id(), DAY, 8, 12)), merged);
        service.setAvailability(alex.id(), DAY, List.of(interval(alex.id(), DAY, 14, 15)));
        assertEquals(3, service.state().availability().size());
        assertFalse(service.state().availability().contains(interval(alex.id(), DAY, 8, 12)));
        service.setAvailability(alex.id(), DAY, List.of());
        assertEquals(2, service.state().availability().size());
    }

    @Test
    void rejectsMismatchedAvailabilityWithoutChangingState() throws IOException {
        CampusPlanService service = new CampusPlanService(new TestStorage());
        Student alex = service.createStudent("Alex", "a@b");
        AppState before = service.state();
        assertThrows(IllegalArgumentException.class, () -> service.setAvailability("missing", DAY, List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.setAvailability(alex.id(), null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.setAvailability(alex.id(), DAY, null));
        assertThrows(IllegalArgumentException.class, () -> service.setAvailability(alex.id(), DAY, Arrays.asList((Availability) null)));
        assertThrows(IllegalArgumentException.class, () -> service.setAvailability(alex.id(), DAY,
                List.of(interval("other", DAY, 8, 9))));
        assertThrows(IllegalArgumentException.class, () -> service.setAvailability(alex.id(), DAY,
                List.of(interval(alex.id(), DAY.plusDays(1), 8, 9))));
        assertSame(before, service.state());
    }

    @Test
    void createsAndEditsDraftWithoutChangingItsId() throws IOException {
        CampusPlanService service = withStudents();
        List<String> ids = service.state().students().stream().map(Student::id).toList();
        DraftEvent draft = service.createDraft("Project", ids, DAY, DAY.plusDays(6), 180, Set.of(Feature.WHITEBOARD), 2);
        DraftEvent edited = service.editDraft(draft.id(), "Study", ids, DAY, DAY, 30, Set.of(), 2);
        assertEquals(draft.id(), edited.id());
        assertEquals(edited, service.event(draft.id()));
        assertEquals(1, service.state().events().size());
        assertEquals("Study", edited.title());
        assertThrows(IllegalArgumentException.class, () -> service.event("missing"));
    }

    @Test
    void unknownReferencesAndInvalidDraftEditsPreserveState() throws IOException {
        CampusPlanService service = withStudents();
        List<String> ids = service.state().students().stream().map(Student::id).toList();
        DraftEvent draft = service.createDraft("Project", ids, DAY, DAY, 60, Set.of(), 2);
        AppState before = service.state();
        assertThrows(IllegalArgumentException.class, () -> service.createDraft("Other", List.of(ids.getFirst(), "missing"),
                DAY, DAY, 60, Set.of(), 2));
        assertThrows(IllegalArgumentException.class, () -> service.editDraft(draft.id(), "Project", ids,
                DAY, DAY.plusDays(7), 60, Set.of(), 2));
        assertThrows(IllegalArgumentException.class, () -> service.editDraft(draft.id(), "Project", List.of("missing", ids.getFirst()),
                DAY, DAY, 60, Set.of(), 2));
        assertThrows(IllegalArgumentException.class, () -> service.editDraft("missing", "Project", ids,
                DAY, DAY, 60, Set.of(), 2));
        assertSame(before, service.state());
    }

    @Test
    void seedOnlyLoadsIntoCompletelyEmptyApplication() throws IOException {
        CampusPlanService service = new CampusPlanService(new TestStorage());
        AppState seed = new AppState(List.of(new Student("s1", "Alex", "a@b")), List.of(), List.of(), List.of());
        assertThrows(IllegalArgumentException.class, () -> service.loadSeed(null));
        service.loadSeed(seed);
        assertEquals(seed, service.state());
        assertThrows(IllegalArgumentException.class, () -> service.loadSeed(AppState.empty()));
        assertEquals(seed, service.state());
    }

    @Test
    void storageIsExplicitAndSurvivesNewServiceInstance() throws IOException {
        InMemoryStorage storage = new InMemoryStorage();
        CampusPlanService service = new CampusPlanService(storage);
        Student student = service.createStudent("Alex", "a@b");
        assertTrue(storage.load().isEmpty());
        service.save();
        assertEquals(service.state(), storage.load());
        assertEquals(student, new CampusPlanService(storage).student(student.id()));
    }

    @Test
    void propagatesStorageFailuresAndKeepsUnsavedState() throws IOException {
        Storage broken = new Storage() {
            @Override public AppState load() throws IOException { throw new IOException("Read failed"); }
            @Override public void save(AppState state) {}
        };
        assertThrows(IOException.class, () -> new CampusPlanService(broken));
        Storage failingSave = new Storage() {
            @Override public AppState load() { return AppState.empty(); }
            @Override public void save(AppState state) throws IOException { throw new IOException("Disk full"); }
        };
        CampusPlanService service = new CampusPlanService(failingSave);
        service.createStudent("Alex", "a@b");
        AppState before = service.state();
        assertThrows(IOException.class, service::save);
        assertSame(before, service.state());
    }

    private static CampusPlanService withStudents() throws IOException {
        CampusPlanService service = new CampusPlanService(new TestStorage());
        service.createStudent("Alex", "a@b");
        service.createStudent("Sam", "s@b");
        return service;
    }

    private static Availability interval(String id, LocalDate date, int start, int end) {
        return new Availability(id, date, LocalTime.of(start, 0), LocalTime.of(end, 0));
    }

    private static class TestStorage implements Storage {
        private AppState saved = AppState.empty();
        @Override public AppState load() { return saved; }
        @Override public void save(AppState state) { saved = state; }
    }
}
