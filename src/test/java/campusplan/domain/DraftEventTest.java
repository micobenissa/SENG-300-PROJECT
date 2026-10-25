package campusplan.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class DraftEventTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 26);

    @Test
    void draftAcceptsBoundaryValuesAndCopiesInvitees() {
        List<String> invitees = new ArrayList<>(List.of("s1", "s2"));
        DraftEvent event = draft(invitees, DAY, 30, 2);
        invitees.clear();
        assertEquals(List.of("s1", "s2"), event.inviteeIds());
        assertThrows(UnsupportedOperationException.class, () -> event.inviteeIds().clear());
        List<String> twenty = IntStream.rangeClosed(1, 20).mapToObj(i -> "s" + i).toList();
        assertEquals(20, draft(twenty, DAY.plusDays(6), 180, 20).inviteeIds().size());
        assertEquals(100, new DraftEvent("e1", "x".repeat(100), List.of("s1", "s2"),
                DAY, DAY, 30, Set.of(), 2).title().length());
    }

    @Test
    void draftRejectsInvalidLimitsAndDuplicateInvitees() {
        List<String> valid = List.of("s1", "s2");
        for (List<String> ids : List.of(List.of("s1"), List.of("s1", "s1"), List.of("s1", " "),
                IntStream.rangeClosed(1, 21).mapToObj(i -> "s" + i).toList())) {
            assertThrows(IllegalArgumentException.class, () -> draft(ids, DAY, 60, 2));
        }
        assertThrows(IllegalArgumentException.class, () -> draft(valid, DAY.minusDays(1), 60, 2));
        assertThrows(IllegalArgumentException.class, () -> draft(valid, DAY.plusDays(7), 60, 2));
        for (int duration : List.of(0, 15, 45, 210)) {
            assertThrows(IllegalArgumentException.class, () -> draft(valid, DAY, duration, 2));
        }
        assertThrows(IllegalArgumentException.class, () -> draft(valid, DAY, 60, 1));
        assertThrows(IllegalArgumentException.class, () -> draft(valid, DAY, 60, 3));
        assertThrows(IllegalArgumentException.class, () -> new DraftEvent("e1", "x".repeat(101), valid,
                DAY, DAY, 60, Set.of(), 2));
        assertThrows(IllegalArgumentException.class, () -> new DraftEvent("e1", "Study", valid,
                null, DAY, 60, Set.of(), 2));
        assertThrows(IllegalArgumentException.class, () -> new DraftEvent("e1", "Study", valid,
                DAY, null, 60, Set.of(), 2));
        assertThrows(IllegalArgumentException.class, () -> new DraftEvent("e1", "Study", valid,
                DAY, DAY, 60, null, 2));
    }

    @Test
    void draftStartsPendingAndRequiresCompleteImmutablePendingResponses() {
        List<String> invitees = List.of("s1", "s2");
        DraftEvent draft = draft(invitees, DAY, 60, 2);
        assertEquals(EventStatus.DRAFT, draft.status());
        Map<String, AttendanceResponse> pending = new HashMap<>(Map.of(
                "s1", AttendanceResponse.PENDING, "s2", AttendanceResponse.PENDING));
        assertEquals(pending, draft.responses());
        DraftEvent copy = new DraftEvent("e2", "Study", invitees, DAY, DAY, 60, Set.of(), 2,
                EventStatus.DRAFT, pending);
        pending.clear();
        assertEquals(2, copy.responses().size());
        assertThrows(UnsupportedOperationException.class, () -> copy.responses().clear());
        for (EventStatus status : new EventStatus[]{null, EventStatus.SCHEDULED, EventStatus.CANCELLED}) {
            assertThrows(IllegalArgumentException.class, () -> new DraftEvent("e2", "Study", invitees,
                    DAY, DAY, 60, Set.of(), 2, status, draft.responses()));
        }
        List<Map<String, AttendanceResponse>> invalidResponses = List.of(
                Map.of(), Map.of("s1", AttendanceResponse.PENDING),
                Map.of("s1", AttendanceResponse.PENDING, "other", AttendanceResponse.PENDING),
                Map.of("s1", AttendanceResponse.PENDING, "s2", AttendanceResponse.PENDING, "other", AttendanceResponse.PENDING),
                Map.of("s1", AttendanceResponse.ACCEPTED, "s2", AttendanceResponse.PENDING),
                Map.of("s1", AttendanceResponse.PENDING, "s2", AttendanceResponse.DECLINED));
        for (Map<String, AttendanceResponse> responses : invalidResponses) {
            assertThrows(IllegalArgumentException.class, () -> new DraftEvent("e2", "Study", invitees,
                    DAY, DAY, 60, Set.of(), 2, EventStatus.DRAFT, responses));
        }
        Map<String, AttendanceResponse> nullResponse = new HashMap<>(draft.responses());
        nullResponse.put("s1", null);
        assertThrows(IllegalArgumentException.class, () -> new DraftEvent("e2", "Study", invitees,
                DAY, DAY, 60, Set.of(), 2, EventStatus.DRAFT, nullResponse));
        assertThrows(IllegalArgumentException.class, () -> new DraftEvent("e2", "Study", invitees,
                DAY, DAY, 60, Set.of(), 2, EventStatus.DRAFT, null));
        assertThrows(IllegalArgumentException.class, () -> draft(null, DAY, 60, 2));
    }

    private static DraftEvent draft(List<String> ids, LocalDate end, int duration, int minimum) {
        return new DraftEvent("e1", "Study", ids, DAY, end, duration, Set.of(), minimum);
    }
}