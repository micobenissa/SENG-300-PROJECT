package campusplan.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Scheduled and cancelled events, and the reservation and attendance invariants of AppState. */
class ScheduledEventTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 16);
    private static final List<String> INVITEES = List.of("s1", "s2");

    private static DraftEvent draft(String id) {
        return new DraftEvent(id, "Event " + id, INVITEES, DAY, DAY.plusDays(1), 60, Set.of(), 2);
    }

    private static List<Student> students() {
        return List.of(new Student("s1", "A", "a@x.ca"), new Student("s2", "B", "b@x.ca"));
    }

    private static List<Room> rooms() {
        return List.of(new Room("R1", "Cedar", 4, Set.of()), new Room("R2", "Maple", 4, Set.of()));
    }

    private static Availability free(String id, String start, String end) {
        return new Availability(id, DAY, LocalTime.parse(start), LocalTime.parse(end));
    }

    private static AppState state(List<Availability> availability, DraftEvent... events) {
        return new AppState(students(), rooms(), availability, List.of(events));
    }

    @Test
    void schedulingAddsRoomAndTimeAndKeepsEverythingElse() {
        DraftEvent draft = draft("e1");
        DraftEvent scheduled = draft.scheduledAt("R1", DAY, LocalTime.of(9, 0));
        assertEquals(EventStatus.SCHEDULED, scheduled.status());
        assertTrue(scheduled.isScheduled());
        assertEquals(LocalTime.of(10, 0), scheduled.endTime());
        assertEquals(draft.responses(), scheduled.responses());
        assertEquals(draft.inviteeIds(), scheduled.inviteeIds());
    }

    @Test
    void scheduledTimeMustMatchTheEventRules() {
        DraftEvent draft = draft("e1");
        assertThrows(IllegalArgumentException.class, () -> draft.scheduledAt(null, DAY, LocalTime.of(9, 0)));
        assertThrows(IllegalArgumentException.class, () -> draft.scheduledAt("R1", null, LocalTime.of(9, 0)));
        assertThrows(IllegalArgumentException.class, () -> draft.scheduledAt("R1", DAY, null));
        assertThrows(IllegalArgumentException.class, () -> draft.scheduledAt("R1", DAY.minusDays(1), LocalTime.of(9, 0)));
        assertThrows(IllegalArgumentException.class, () -> draft.scheduledAt("R1", DAY.plusDays(2), LocalTime.of(9, 0)));
        assertThrows(IllegalArgumentException.class, () -> draft.scheduledAt("R1", DAY, LocalTime.of(7, 30)));
        assertThrows(IllegalArgumentException.class, () -> draft.scheduledAt("R1", DAY, LocalTime.of(21, 30)));
        assertThrows(IllegalArgumentException.class, () -> draft.scheduledAt("R1", DAY, LocalTime.of(9, 15)));
        // The last window day is allowed, as is a start that ends exactly at closing time.
        assertEquals(LocalTime.of(22, 0), draft.scheduledAt("R1", DAY.plusDays(1), LocalTime.of(21, 0)).endTime());
    }

    @Test
    void draftsAndCancelledEventsCannotCarryARoomOrTime() {
        DraftEvent draft = draft("e1");
        assertThrows(IllegalArgumentException.class, () -> new DraftEvent("e1", "T", INVITEES, DAY, DAY, 60,
                Set.of(), 2, EventStatus.DRAFT, draft.responses(), "R1", DAY, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        assertThrows(IllegalArgumentException.class, () -> new DraftEvent("e1", "T", INVITEES, DAY, DAY, 60,
                Set.of(), 2, EventStatus.CANCELLED, draft.responses(), "R1", null, null, null));
        DraftEvent cancelled = draft.scheduledAt("R1", DAY, LocalTime.of(9, 0)).cancelled();
        assertEquals(EventStatus.CANCELLED, cancelled.status());
        assertNull(cancelled.roomId());
        assertNull(cancelled.startTime());
        assertFalse(cancelled.overlaps(DAY, LocalTime.of(9, 0), LocalTime.of(10, 0)));
    }

    @Test
    void draftResponsesMustBePendingButScheduledOnesCanChangeAndKeepOrder() {
        DraftEvent scheduled = draft("e1").scheduledAt("R1", DAY, LocalTime.of(9, 0));
        DraftEvent accepted = scheduled.withResponse("s2", AttendanceResponse.ACCEPTED);
        assertEquals(AttendanceResponse.ACCEPTED, accepted.responses().get("s2"));
        assertEquals(AttendanceResponse.PENDING, accepted.responses().get("s1"));
        assertEquals(AttendanceResponse.PENDING, scheduled.responses().get("s2"));
        assertThrows(IllegalArgumentException.class, () -> scheduled.withResponse("other", AttendanceResponse.DECLINED));
        assertThrows(IllegalArgumentException.class, () -> scheduled.withResponse("s1", null));
        assertThrows(IllegalArgumentException.class, () -> new DraftEvent("e1", "T", INVITEES, DAY, DAY, 60,
                Set.of(), 2, EventStatus.DRAFT, Map.of("s1", AttendanceResponse.ACCEPTED, "s2", AttendanceResponse.PENDING)));
    }

    @Test
    void overlapIsHalfOpenAndOnlyAppliesToScheduledEvents() {
        DraftEvent scheduled = draft("e1").scheduledAt("R1", DAY, LocalTime.of(13, 0));
        assertFalse(draft("e2").overlaps(DAY, LocalTime.of(13, 0), LocalTime.of(14, 0)));
        assertTrue(scheduled.overlaps(DAY, LocalTime.of(13, 30), LocalTime.of(14, 30)));
        assertTrue(scheduled.overlaps(DAY, LocalTime.of(12, 30), LocalTime.of(13, 30)));
        assertFalse(scheduled.overlaps(DAY, LocalTime.of(14, 0), LocalTime.of(15, 0)));
        assertFalse(scheduled.overlaps(DAY, LocalTime.of(12, 0), LocalTime.of(13, 0)));
        assertFalse(scheduled.overlaps(DAY.plusDays(1), LocalTime.of(13, 0), LocalTime.of(14, 0)));
    }

    // ------------------------------------------------------------ AppState invariants

    @Test
    void sameRoomOverlapIsRejectedButAdjacentAndOtherRoomsAreAllowed() {
        DraftEvent first = draft("e1").scheduledAt("R1", DAY, LocalTime.of(13, 0));
        DraftEvent adjacent = draft("e2").scheduledAt("R1", DAY, LocalTime.of(14, 0));
        DraftEvent otherRoom = draft("e3").scheduledAt("R2", DAY, LocalTime.of(13, 0));
        DraftEvent overlapping = draft("e4").scheduledAt("R1", DAY, LocalTime.of(13, 30));
        assertEquals(3, state(List.of(), first, adjacent, otherRoom).events().size());
        assertThrows(IllegalArgumentException.class, () -> state(List.of(), first, overlapping));
    }

    @Test
    void cancelledEventsAndDraftsDoNotBlockARoom() {
        DraftEvent cancelled = draft("e1").scheduledAt("R1", DAY, LocalTime.of(13, 0)).cancelled();
        DraftEvent scheduled = draft("e2").scheduledAt("R1", DAY, LocalTime.of(13, 0));
        assertEquals(3, state(List.of(), cancelled, scheduled, draft("e3")).events().size());
    }

    @Test
    void scheduledEventMustUseAKnownRoom() {
        assertThrows(IllegalArgumentException.class,
                () -> state(List.of(), draft("e1").scheduledAt("nowhere", DAY, LocalTime.of(9, 0))));
    }

    @Test
    void acceptedAttendanceNeedsAvailabilityForTheWholeEventAndCannotOverlap() {
        DraftEvent accepted = draft("e1").scheduledAt("R1", DAY, LocalTime.of(9, 0))
                .withResponse("s1", AttendanceResponse.ACCEPTED);
        // Two touching intervals are merged, so they cover the whole hour.
        assertEquals(1, state(List.of(free("s1", "08:30", "09:30"), free("s1", "09:30", "11:00")), accepted)
                .events().size());
        assertThrows(IllegalArgumentException.class, () -> state(List.of(), accepted));
        assertThrows(IllegalArgumentException.class, () -> state(List.of(free("s1", "09:30", "11:00")), accepted));

        DraftEvent overlapping = draft("e2").scheduledAt("R2", DAY, LocalTime.of(9, 30))
                .withResponse("s1", AttendanceResponse.ACCEPTED);
        assertThrows(IllegalArgumentException.class,
                () -> state(List.of(free("s1", "09:00", "11:00")), accepted, overlapping));
        // Pending on one of the two events is fine, and so is an adjacent accepted event.
        DraftEvent pending = draft("e2").scheduledAt("R2", DAY, LocalTime.of(9, 30));
        DraftEvent adjacent = draft("e3").scheduledAt("R2", DAY, LocalTime.of(10, 0))
                .withResponse("s1", AttendanceResponse.ACCEPTED);
        assertEquals(2, state(List.of(free("s1", "09:00", "11:00")), accepted, pending).events().size());
        assertEquals(2, state(List.of(free("s1", "09:00", "11:00")), accepted, adjacent).events().size());
    }

    @Test
    void withHelpersReplaceOneCollectionAndRevalidate() {
        AppState base = state(List.of());
        DraftEvent draft = draft("e1");
        assertEquals(List.of(draft), base.withEvents(List.of(draft)).events());
        assertEquals(1, base.withAvailability(List.of(free("s1", "09:00", "10:00"))).availability().size());
        assertEquals(1, base.withStudents(List.of(new Student("s1", "A", "a@x.ca"))).students().size() - 1 + 1);
        assertThrows(IllegalArgumentException.class, () -> base.withStudents(List.of()).withEvents(List.of(draft)));
    }
}