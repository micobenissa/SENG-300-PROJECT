package campusplan.application;

import campusplan.application.scheduling.SuggestionEngine;
import campusplan.application.scheduling.SuggestionResult;
import campusplan.domain.AppState;
import campusplan.domain.AttendanceResponse;
import campusplan.domain.Availability;
import campusplan.domain.DraftEvent;
import campusplan.domain.Room;
import campusplan.domain.Student;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// NFR-03: 20 invitees, 20 rooms, a seven day window + 100 existing events, under two seconds, over 10 runs.
class SuggestionPerformanceTest {
    private static final LocalDate START = LocalDate.of(2026, 11, 16); // first day of seven day window

    @Test
    void largestSpecifiedCaseCompletesWithinTwoSecondsOverTenRuns() {
        // 24 students (20 are invitees, 4 are filler), everyone is free every day of window.
        List<Student> students = new ArrayList<>();
        List<Availability> availability = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            students.add(new Student("s" + i, "Student " + i, "s" + i + "@campus.test"));
            for (int day = 0; day < 7; day++) {
                availability.add(new Availability("s" + i, START.plusDays(day), LocalTime.of(8, 0), LocalTime.of(22, 0)));
            }
        }
        // 20 rooms with capacity 20-24 and no special features. each room can hold all invitees
        // capacity never rejects, work is spread across rules
        List<Room> rooms = new ArrayList<>();
        for (int r = 1; r <= 20; r++) {
            rooms.add(new Room("R" + r, "Room " + r, 20 + r % 5, Set.of()));
        }
        // 100 scheduled events spread across rooms, days + start times, each with two invitees who accepted.
        // checking RoomFree rule because some candidates overlap
        List<DraftEvent> events = new ArrayList<>();
        for (int e = 0; e < 100; e++) {
            // getting two filler students for each old event
            String a = "s" + (21 + e % 4);
            String b = "s" + (21 + (e + 1) % 4);

            // start times (8:00, 10:00...16:00) in groups of 20 events
            LocalTime start = LocalTime.of(8 + (e / 20) * 2, 0);
            DraftEvent event = new DraftEvent("old" + e, "Old " + e, List.of(a, b), START, START.plusDays(6), 60, Set.of(), 2).scheduledAt("R" + (1 + e % 20), START.plusDays(e % 7), start);
            events.add(event);
        }
        // actual event being booked (30 min, inviting s1-s20, min attendance of 15)
        // accepted attendance only where it cannot overlap. give each student at most one accepted event per slot.
        List<String> invitees = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            invitees.add("s" + i);
        }
        DraftEvent target = new DraftEvent("target", "Big meeting", invitees, START, START.plusDays(6), 30, Set.of(), 15);
        events.add(target);
        AppState state = new AppState(students, rooms, availability, events);
        SuggestionEngine engine = SuggestionEngine.standard();

        engine.suggest(state, target); // warm-up, as the requirement excludes startup

        // run search 10 times, keep slowest time to check worst case
        long slowest = 0;
        SuggestionResult result = null;
        for (int run = 0; run < 10; run++) {
            long begin = System.nanoTime();
            result = engine.suggest(state, target);
            slowest = Math.max(slowest, (System.nanoTime() - begin) / 1_000_000);
        }
        // since there's more than enough valid rooms, there should be 5 suggestions
        assertEquals(5, result.suggestions().size());

        // 20 rooms * 7 days * 28 start times/day for a 30 min event. should consider all candidates
        assertEquals(20 * 7 * 28, result.candidatesConsidered());

        // checking worst case scenario to see if it passes the time threshold
        System.out.println("NFR-03 slowest of 10 runs: " + slowest + " ms");
        assertTrue(slowest < 2000, "slowest run took " + slowest + " ms");

        // checking for suggestions doesn't affect the event. nobody has responded, so s1 should still be pending.
        assertEquals(AttendanceResponse.PENDING, target.responses().get("s1"));
    }
}