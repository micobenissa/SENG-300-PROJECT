package campusplan.application;

import campusplan.application.history.Command;
import campusplan.application.history.CommandHistory;
import campusplan.application.history.StateChangeCommand;
import campusplan.application.scheduling.Candidate;
import campusplan.application.scheduling.SuggestionEngine;
import campusplan.application.scheduling.SuggestionResult;
import campusplan.domain.AppState;
import campusplan.domain.AttendanceResponse;
import campusplan.domain.Availability;
import campusplan.domain.DraftEvent;
import campusplan.domain.Eligibility;
import campusplan.domain.EventStatus;
import campusplan.domain.Feature;
import campusplan.domain.Room;
import campusplan.domain.Student;
import campusplan.persistence.Storage;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The application operations. The CLI calls these and never checks a rule itself.
 *
 * <p>Every change builds a complete, valid replacement state first, then commits it as one undoable
 * command. A rejected change throws before anything is committed, so the state, the reservations and the
 * undo history stay as they were.
 */
public final class CampusPlanService {
    private final Storage storage;
    private final Clock clock;
    private final SuggestionEngine engine;
    private final CommandHistory history = new CommandHistory();
    // Each edit builds a valid replacement before it can replace the current state.
    private AppState state;

    public CampusPlanService(Storage storage) throws IOException {
        this(storage, Clock.systemDefaultZone());
    }

    public CampusPlanService(Storage storage, Clock clock) throws IOException {
        this(storage, clock, SuggestionEngine.standard());
    }

    public CampusPlanService(Storage storage, Clock clock, SuggestionEngine engine) throws IOException {
        this.storage = Objects.requireNonNull(storage);
        this.clock = Objects.requireNonNull(clock);
        this.engine = Objects.requireNonNull(engine);
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

    // ---------------------------------------------------------------- students and rooms (P1)

    public Student createStudent(String name, String email) {
        Student student = new Student(UUID.randomUUID().toString(), name, email);
        List<Student> students = new ArrayList<>(state.students());
        students.add(student);
        commit("Create student " + student.name(), state.withStudents(students));
        return student;
    }

    public Student editStudent(String id, String name, String email) {
        Student previous = student(id);
        Student replacement = new Student(previous.id(), name, email);
        List<Student> students = new ArrayList<>(state.students());
        students.set(students.indexOf(previous), replacement);
        commit("Edit student " + replacement.name(), state.withStudents(students));
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

    // ---------------------------------------------------------------- availability

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
        requireAcceptedAttendanceStillCovered(studentId, date, AppState.mergeIntervals(intervals));
        // Replace only this date; leave the student's other dates and everyone else alone.
        List<Availability> updated = new ArrayList<>(state.availability().stream()
                .filter(interval -> !(studentId.equals(interval.studentId()) && date.equals(interval.date()))).toList());
        updated.addAll(intervals);
        commit("Update availability for " + student(studentId).name() + " on " + date,
                state.withAvailability(updated));
        return state.availability().stream().filter(interval -> studentId.equals(interval.studentId())
                && date.equals(interval.date())).toList();
    }

    // An availability edit may not remove time the student has already accepted (FR-06).
    private void requireAcceptedAttendanceStillCovered(String studentId, LocalDate date, List<Availability> merged) {
        List<String> affected = state.events().stream()
                .filter(event -> event.isScheduled() && event.scheduledDate().equals(date))
                .filter(event -> event.responses().get(studentId) == AttendanceResponse.ACCEPTED)
                .filter(event -> merged.stream().noneMatch(interval ->
                        interval.covers(date, event.startTime(), event.endTime())))
                .map(event -> "\"" + event.title() + "\" " + event.startTime() + "-" + event.endTime())
                .toList();
        if (!affected.isEmpty()) {
            throw new IllegalArgumentException("This change would break accepted attendance for "
                    + String.join("; ", affected) + ". Change the response to PENDING or DECLINED first.");
        }
    }

    // ---------------------------------------------------------------- drafts

    public DraftEvent createDraft(String title, List<String> inviteeIds, LocalDate startDate, LocalDate endDate,
                                  int durationMinutes, Set<Feature> requiredFeatures, int minimumAttendance) {
        DraftEvent event = new DraftEvent(UUID.randomUUID().toString(), title, inviteeIds,
                startDate, endDate, durationMinutes, requiredFeatures, minimumAttendance);
        requireCurrentOrFuture(event.startDate(), "Event window start date");
        List<DraftEvent> events = new ArrayList<>(state.events());
        events.add(event);
        commit("Create draft " + event.title(), state.withEvents(events));
        return event;
    }

    public DraftEvent editDraft(String id, String title, List<String> inviteeIds, LocalDate startDate,
                                LocalDate endDate, int durationMinutes, Set<Feature> requiredFeatures,
                                int minimumAttendance) {
        DraftEvent previous = event(id);
        // Once scheduled, the planning fields stay fixed in P2.
        if (previous.status() != EventStatus.DRAFT) {
            throw new IllegalArgumentException("Only a draft can be edited; this event is "
                    + previous.status() + ".");
        }
        DraftEvent replacement = new DraftEvent(previous.id(), title, inviteeIds,
                startDate, endDate, durationMinutes, requiredFeatures, minimumAttendance);
        requireCurrentOrFuture(replacement.startDate(), "Event window start date");
        List<DraftEvent> events = new ArrayList<>(state.events());
        events.set(events.indexOf(previous), replacement);
        commit("Edit draft " + replacement.title(), state.withEvents(events));
        return replacement;
    }

    // ---------------------------------------------------------------- scheduling (FR-04, FR-05)

    /** The top five valid time and room combinations for a draft, with reasons (FR-04). Changes nothing. */
    public SuggestionResult suggestions(String eventId) {
        DraftEvent event = event(eventId);
        requireDraft(event);
        return engine.suggest(state, event);
    }

    /**
     * Books a suggestion. Choosing a suggestion does not reserve it, so every rule is checked again against the
     * current state first. A stale or invalid choice is rejected and nothing changes (FR-05).
     */
    public DraftEvent confirmSuggestion(String eventId, String roomId, LocalDate date, LocalTime start) {
        DraftEvent event = event(eventId);
        requireDraft(event);
        Room room = state.rooms().stream().filter(candidate -> candidate.id().equals(roomId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown room ID: " + roomId));
        if (date == null || start == null) {
            throw new IllegalArgumentException("Suggestion date and start time are required.");
        }
        LocalTime end = start.plusMinutes(event.durationMinutes());
        boolean inWindow = !date.isBefore(event.startDate()) && !date.isAfter(event.endDate());
        boolean inHours = !start.isBefore(LocalTime.of(8, 0)) && !end.isAfter(LocalTime.of(22, 0))
                && end.isAfter(start) && start.getMinute() % 30 == 0;
        if (!inWindow || !inHours) {
            throw new IllegalArgumentException(
                    "Suggestion is not valid: it must be inside the date window and 08:00-22:00.");
        }
        Optional<String> rejection = engine.rejection(state, event, new Candidate(room, date, start, end));
        if (rejection.isPresent()) {
            throw new IllegalArgumentException("Suggestion is no longer valid: " + rejection.get() + ".");
        }
        DraftEvent scheduled = event.scheduledAt(room.id(), date, start);
        commit("Schedule " + event.title() + " in " + room.name() + " on " + date + " " + start,
                state.withEvents(replace(event, scheduled)));
        return scheduled;
    }

    /**
     * Cancels a scheduled event, freeing its room and its participants' time but keeping its record and
     * responses. The caller must pass confirmed = true; declining the confirmation changes nothing.
     */
    public DraftEvent cancelEvent(String eventId, boolean confirmed) {
        DraftEvent event = event(eventId);
        if (!event.isScheduled()) {
            throw new IllegalArgumentException("Only a scheduled event can be cancelled; this event is "
                    + event.status() + ".");
        }
        if (!confirmed) {
            throw new IllegalArgumentException("Cancellation was not confirmed. Nothing was changed.");
        }
        DraftEvent cancelled = event.cancelled();
        commit("Cancel " + event.title(), state.withEvents(replace(event, cancelled)));
        return cancelled;
    }

    // ---------------------------------------------------------------- responses (FR-06)

    /**
     * Records an invitee's response to a scheduled event. ACCEPTED needs availability for the whole time and
     * no overlapping ACCEPTED event elsewhere. PENDING and DECLINED release any accepted commitment.
     */
    public DraftEvent recordResponse(String eventId, String studentId, AttendanceResponse response) {
        DraftEvent event = event(eventId);
        if (response == null) {
            throw new IllegalArgumentException("Attendance response is required.");
        }
        if (!event.isScheduled()) {
            throw new IllegalArgumentException(
                    "Responses can only be recorded for a scheduled event; this event is " + event.status() + ".");
        }
        if (!event.responses().containsKey(studentId)) {
            throw new IllegalArgumentException("That student is not invited to this event.");
        }
        if (event.responses().get(studentId) == response) {
            return event; // nothing changes, so nothing enters the undo history
        }
        if (response == AttendanceResponse.ACCEPTED) {
            Optional<String> reason = Eligibility.ineligibleReason(state, studentId, event.scheduledDate(),
                    event.startTime(), event.endTime(), event.id());
            if (reason.isPresent()) {
                throw new IllegalArgumentException(student(studentId).name() + " cannot accept: "
                        + reason.get() + ".");
            }
        }
        DraftEvent updated = event.withResponse(studentId, response);
        commit("Record " + response + " for " + student(studentId).name() + " at " + event.title(),
                state.withEvents(replace(event, updated)));
        return updated;
    }

    /**
     * Invitees who could attend a scheduled event right now. This is separate from their responses: a student
     * can be eligible and still PENDING, and a response never changes eligibility.
     */
    public List<Student> eligibleStudents(String eventId) {
        DraftEvent event = event(eventId);
        if (!event.isScheduled()) {
            throw new IllegalArgumentException("Eligible attendance is only shown for a scheduled event.");
        }
        return event.inviteeIds().stream().filter(id -> Eligibility.ineligibleReason(state, id,
                event.scheduledDate(), event.startTime(), event.endTime(), event.id()).isEmpty())
                .map(this::student).toList();
    }

    // ---------------------------------------------------------------- listing and export (FR-07)

    /** Events ordered by date, then start time, then title, optionally limited by status and date. */
    public List<DraftEvent> listEvents(EventStatus status, LocalDate date) {
        return state.events().stream()
                .filter(event -> status == null || event.status() == status)
                .filter(event -> date == null || (event.isScheduled()
                        ? event.scheduledDate().equals(date)
                        : !date.isBefore(event.startDate()) && !date.isAfter(event.endDate())))
                .sorted(Comparator.comparing((DraftEvent event) -> event.isScheduled()
                                ? event.scheduledDate() : event.startDate())
                        .thenComparing(event -> event.isScheduled() ? event.startTime() : LocalTime.MIN)
                        .thenComparing(DraftEvent::title).thenComparing(DraftEvent::id))
                .toList();
    }

    /** The roster as CSV text: header student_id,name,email,response. Exporting never enters undo history. */
    public String rosterCsv(String eventId) {
        return RosterCsv.format(event(eventId), state.students());
    }

    // ---------------------------------------------------------------- undo and redo (FR-08)

    public boolean canUndo() {
        return history.canUndo();
    }

    public boolean canRedo() {
        return history.canRedo();
    }

    /** Reverses the latest change, restoring the related data and reservations together. */
    public String undo() {
        String description = history.nextUndo()
                .orElseThrow(() -> new IllegalArgumentException("Nothing to undo."));
        state = history.undo();
        return description;
    }

    public String redo() {
        String description = history.nextRedo()
                .orElseThrow(() -> new IllegalArgumentException("Nothing to redo."));
        state = history.redo();
        return description;
    }

    // ---------------------------------------------------------------- seed and save

    public void loadSeed(AppState seed) {
        if (!state.isEmpty()) {
            throw new IllegalArgumentException("Seed data can only be loaded into an empty application.");
        }
        if (seed == null) {
            throw new IllegalArgumentException("Seed data is required.");
        }
        // Loading seed data is not undoable, and old history must not be able to overwrite it later.
        history.clear();
        state = seed;
    }

    public void save() throws IOException {
        storage.save(state);
    }

    // ---------------------------------------------------------------- helpers

    private void commit(String description, AppState next) {
        Command command = new StateChangeCommand(description, next);
        state = history.execute(command, state);
    }

    private List<DraftEvent> replace(DraftEvent previous, DraftEvent replacement) {
        List<DraftEvent> events = new ArrayList<>(state.events());
        events.set(events.indexOf(previous), replacement);
        return events;
    }

    private void requireDraft(DraftEvent event) {
        if (event.status() == EventStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled events cannot be scheduled again.");
        }
        if (event.status() != EventStatus.DRAFT) {
            throw new IllegalArgumentException("This event is already scheduled.");
        }
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
