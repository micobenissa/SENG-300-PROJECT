package campusplan.application;

import campusplan.domain.AppState;
import campusplan.domain.Availability;
import campusplan.domain.DraftEvent;
import campusplan.domain.Feature;
import campusplan.domain.Room;
import campusplan.domain.Student;
import campusplan.persistence.Storage;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class CampusPlanService {
    private final Storage storage;
    private final Clock clock;
    // Each edit builds a valid replacement before it can replace the current state.
    private AppState state;

    public CampusPlanService(Storage storage) throws IOException {
        this(storage, Clock.systemDefaultZone());
    }

    public CampusPlanService(Storage storage, Clock clock) throws IOException {
        this.storage = Objects.requireNonNull(storage);
        this.clock = Objects.requireNonNull(clock);
        state = Objects.requireNonNull(storage.load(), "Storage returned no state.");
    }

    public AppState state() {
        return state;
    }

    public Student student(String id) {
        return state.students().stream().filter(student -> student.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown student ID: " + id));
    }

    public DraftEvent event(String id) {
        return state.events().stream().filter(event -> event.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown event ID: " + id));
    }

    public Student createStudent(String name, String email) {
        Student student = new Student(UUID.randomUUID().toString(), name, email);
        List<Student> students = new ArrayList<>(state.students());
        students.add(student);
        state = new AppState(students, state.rooms(), state.availability(), state.events());
        return student;
    }

    public Student editStudent(String id, String name, String email) {
        Student previous = student(id);
        Student replacement = new Student(previous.id(), name, email);
        List<Student> students = new ArrayList<>(state.students());
        students.set(students.indexOf(previous), replacement);
        state = new AppState(students, state.rooms(), state.availability(), state.events());
        return replacement;
    }

    public List<Student> searchStudents(String query) {
        if (query == null) {
            throw new IllegalArgumentException("Search text is required.");
        }
        String search = query.trim().toLowerCase(Locale.ROOT);
        return state.students().stream().filter(student ->
                student.name().toLowerCase(Locale.ROOT).contains(search) || student.email().contains(search)).toList();
    }

    public List<Room> filterRooms(int minimumCapacity, Set<Feature> requiredFeatures) {
        if (minimumCapacity < 1 || minimumCapacity > 100) {
            throw new IllegalArgumentException("Minimum capacity must be 1-100.");
        }
        if (requiredFeatures == null || requiredFeatures.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Room features must contain valid feature values.");
        }
        return state.rooms().stream().filter(room -> room.capacity() >= minimumCapacity
                && room.features().containsAll(requiredFeatures)).toList();
    }

    public List<Availability> setAvailability(String studentId, LocalDate date, List<Availability> intervals) {
        student(studentId);
        if (date == null || intervals == null || intervals.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Availability date and intervals are required.");
        }
        for (Availability interval : intervals) {
            if (!studentId.equals(interval.studentId()) || !date.equals(interval.date())) {
                throw new IllegalArgumentException("Each interval must match the selected student and date.");
            }
        }
        requireCurrentOrFuture(date, "Availability date");
        // Replace only this date; leave the student's other dates and everyone else alone.
        List<Availability> updated = new ArrayList<>(state.availability().stream()
                .filter(interval -> !(studentId.equals(interval.studentId()) && date.equals(interval.date()))).toList());
        updated.addAll(intervals);
        state = new AppState(state.students(), state.rooms(), updated, state.events());
        return state.availability().stream().filter(interval -> studentId.equals(interval.studentId())
                && date.equals(interval.date())).toList();
    }

    public DraftEvent createDraft(String title, List<String> inviteeIds, LocalDate startDate, LocalDate endDate,
                                  int durationMinutes, Set<Feature> requiredFeatures, int minimumAttendance) {
        DraftEvent event = new DraftEvent(UUID.randomUUID().toString(), title, inviteeIds,
                startDate, endDate, durationMinutes, requiredFeatures, minimumAttendance);
        requireCurrentOrFuture(event.startDate(), "Event window start date");
        List<DraftEvent> events = new ArrayList<>(state.events());
        events.add(event);
        state = new AppState(state.students(), state.rooms(), state.availability(), events);
        return event;
    }

    public DraftEvent editDraft(String id, String title, List<String> inviteeIds, LocalDate startDate,
                                LocalDate endDate, int durationMinutes, Set<Feature> requiredFeatures,
                                int minimumAttendance) {
        DraftEvent previous = event(id);
        DraftEvent replacement = new DraftEvent(previous.id(), title, inviteeIds,
                startDate, endDate, durationMinutes, requiredFeatures, minimumAttendance);
        requireCurrentOrFuture(replacement.startDate(), "Event window start date");
        List<DraftEvent> events = new ArrayList<>(state.events());
        events.set(events.indexOf(previous), replacement);
        state = new AppState(state.students(), state.rooms(), state.availability(), events);
        return replacement;
    }

    public void loadSeed(AppState seed) {
        if (!state.isEmpty()) {
            throw new IllegalArgumentException("Seed data can only be loaded into an empty application.");
        }
        if (seed == null) {
            throw new IllegalArgumentException("Seed data is required.");
        }
        state = seed;
    }

    public void save() throws IOException {
        storage.save(state);
    }

    private void requireCurrentOrFuture(LocalDate date, String field) {
        // Check user edits here so saved records can still load after their dates pass.
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today)) {
            throw new IllegalArgumentException(field + " cannot be in the past. Choose today ("
                    + today + ") or a future date.");
        }
    }
}
