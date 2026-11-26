package campusplan.application.scheduling;

import campusplan.domain.AppState;
import campusplan.domain.AttendanceResponse;
import campusplan.domain.Availability;
import campusplan.domain.DraftEvent;
import campusplan.domain.Feature;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static campusplan.application.scheduling.PlanFixture.*;
import static org.junit.jupiter.api.Assertions.*;

// section 3 acceptance examples + ranking, boundaries and the no-result report (FR-04).
class SuggestionEngineTest {
    private final SuggestionEngine engine = SuggestionEngine.standard();

    // 60 min event on one day has 27 possible starts (08:00 through 21:00) in every room.
    private static final int STARTS_PER_ROOM = 27;

    @Test
    void sixInviteesFiveEligibleMinimumFourReturnsFiveNamesAndWhySixthCannotAttend() {
        DraftEvent event = draft("e1", ids(1, 6), 60, 4);
        AppState state = state(students(6), List.of(room("R1", 6)), freeAll(ids(1, 5), DAY, "09:00", "11:00"), List.of(event));
        SuggestionResult result = engine.suggest(state, event);
        // 09:00, 09:30 and 10:00 are the only starts where the whole hour is covered.
        assertEquals(3, result.suggestions().size());

        // all three tie on attendance, earliest one comes first
        Suggestion best = result.suggestions().getFirst();
        assertEquals(LocalTime.of(9, 0), best.candidate().start());
        assertEquals(LocalTime.of(10, 0), best.candidate().end());

        // give who can come are listed by name in order
        assertEquals(5, best.eligibleCount());
        assertEquals(List.of("Student 1", "Student 2", "Student 3", "Student 4", "Student 5"), best.eligible().stream().map(s -> s.name()).toList());

        // person who can't is named along with a reason
        assertEquals(1, best.ineligible().size());
        assertEquals("Student 6", best.ineligible().getFirst().student().name());
        assertEquals("not available for the whole time", best.ineligible().getFirst().reason());
        assertEquals("5 of 6 invitees are available, all room requirements are met.", best.explanation());
    }

    @Test // attendance issue
    void fiveEligibleWhenMinimumIsSixExcludesTheCandidate() { // if event needs 6, but only 5 can come, there shouldn't be any suggestions
        DraftEvent event = draft("e1", ids(1, 6), 60, 6);

        // 5 free, minimum 6.
        AppState state = state(students(6), List.of(room("R1", 6)), freeAll(ids(1, 5), DAY, "08:00", "22:00"), List.of(event));
        SuggestionResult result = engine.suggest(state, event);
        assertTrue(result.isEmpty());
        assertEquals(STARTS_PER_ROOM, result.candidatesConsidered());
        assertEquals(0, result.roomRejections());
        assertEquals(STARTS_PER_ROOM, result.attendanceRejections());

        // explanation shown to user
        assertTrue(result.noResultReport().contains("0 rejected for room constraints"));
        assertTrue(result.noResultReport().contains("27 for insufficient eligible attendance"));
    }

    // room has to fit everyone invited, not just people that can come.
    @Test
    void roomThatHoldsFiveIsExcludedWhenSixAreInvitedEvenIfFiveAreEligible() {
        DraftEvent event = draft("e1", ids(1, 6), 60, 2);
        AppState state = state(students(6), List.of(room("R1", 5)), freeAll(ids(1, 5), DAY, "08:00", "22:00"), List.of(event));
        SuggestionResult result = engine.suggest(state, event);
        assertTrue(result.isEmpty());

        // room failures (capacity)
        assertEquals(STARTS_PER_ROOM, result.roomRejections());
        assertEquals(0, result.attendanceRejections());
        assertEquals(STARTS_PER_ROOM, (int) result.rejectionsByRule().get("room capacity"));
    }

    // back to back bookings work but overlaps don't
    @Test
    void adjacentReservationsInTheSameRoomAreAllowedButRealOverlapIsRejected() {
        // existing event (R1, 13:00-14:00) has different invitees (s7, s8)
        DraftEvent existing = draft("e0", ids(7, 8), 60, 2).scheduledAt("R1", DAY, LocalTime.of(13, 0));
        DraftEvent event = draft("e1", ids(1, 2), 60, 2);

        // case 1: invitees only free from 14:00, only start is when other event ends. adjacent reservation allowed
        List<Availability> afterwards = freeAll(ids(1, 2), DAY, "14:00", "15:00");
        AppState adjacent = state(students(8), List.of(room("R1", 6)), afterwards, List.of(existing, event));
        SuggestionResult allowed = engine.suggest(adjacent, event);
        assertEquals(1, allowed.suggestions().size());
        assertEquals(LocalTime.of(14, 0), allowed.suggestions().getFirst().candidate().start());

        // case 2: both students only free 13:30-14:30, which overlaps the 13:00-14:00 reservation. not allowed.
        AppState overlapping = state(students(8), List.of(room("R1", 6)), freeAll(ids(1, 2), DAY, "13:30", "14:30"), List.of(existing, event));
        SuggestionResult rejected = engine.suggest(overlapping, event);
        assertTrue(rejected.isEmpty());
        assertTrue(rejected.rejectionsByRule().get("room reservation") >= 1);
    }

    // someone that agreed to another event cant be double booked for times that overlap with their events
    @Test
    void studentAlreadyAcceptedElsewhereIsIneligibleForOverlappingCandidatesOnly() {
        // s1 accepted event in r2 from 13:00-14:00
        DraftEvent elsewhere = draft("e0", ids(1, 2), 60, 2).scheduledAt("R2", DAY, LocalTime.of(13, 0)).withResponse("s1", AttendanceResponse.ACCEPTED);
        DraftEvent event = draft("e1", ids(1, 3), 60, 2);

        // students free from 13:00-15:00, so availability isn't an issue
        List<Availability> free = new ArrayList<>(freeAll(ids(1, 3), DAY, "13:00", "15:00"));
        AppState state = state(students(3), List.of(room("R1", 6), room("R2", 6)), free, List.of(elsewhere, event));
        SuggestionResult result = engine.suggest(state, event);

        // at 14:00 the earlier 13:00-14:00 commitment ends, so all three can attend and it ranks first
        Suggestion first = result.suggestions().getFirst();
        assertEquals(3, first.eligibleCount());
        assertEquals(LocalTime.of(14, 0), first.candidate().start());

        // at 13:00 the commitment overlaps, so s1 drops out and it says why
        // only r1 can offer this slot bc r2 is reserved by the other event.
        Suggestion overlapping = result.suggestions().stream().filter(s -> s.candidate().start().equals(LocalTime.of(13, 0))).findFirst().orElseThrow();
        assertEquals(2, overlapping.eligibleCount());
        assertEquals("Student 1", overlapping.ineligible().getFirst().student().name());
        assertTrue(overlapping.ineligible().getFirst().reason().startsWith("already accepted \"Event e0\""));
    }

    // only an accepted response blocks someone. declined/still-pending to another event still leaves em free for overlapping times
    @Test
    void pendingAndDeclinedResponsesDoNotBlockAnyone() {
        // s1 declined other event, s2+s3 haven't answered
        DraftEvent elsewhere = draft("e0", ids(1, 2), 60, 2).scheduledAt("R2", DAY, LocalTime.of(13, 0)).withResponse("s1", AttendanceResponse.DECLINED);
        DraftEvent event = draft("e1", ids(1, 3), 60, 2);
        AppState state = state(students(3), List.of(room("R1", 6), room("R2", 6)), freeAll(ids(1, 3), DAY, "13:00", "14:00"), List.of(elsewhere, event));

        // all three eligible at overlapping 13:00 timeslot
        assertEquals(3, engine.suggest(state, event).suggestions().getFirst().eligibleCount());
    }

    // when suggestions tie on attendance, tie-break order decides resulting in the same answer every time
    @Test
    void equalEligibleCountsUseTheTieBreakOrderAndRepeatedSearchesAgree() {
        DraftEvent event = draft("e1", ids(1, 6), 60, 2);
        // R3 has the fewest spare seats, then R1 and R2 tie on seats and are ordered by room ID
        // rooms out of order so that the engine can clearly sort them
        AppState state = state(students(6), List.of(room("R2", 8), room("R1", 8), room("R3", 6)), freeAll(ids(1, 6), DAY, "09:00", "10:00"), List.of(event));
        List<String> rooms = engine.suggest(state, event).suggestions().stream().map(s -> s.candidate().room().id()).toList();
        assertEquals(List.of("R3", "R1", "R2"), rooms);

        // identical searches = identical results
        assertEquals(engine.suggest(state, event), engine.suggest(state, event));
    }

    // date and time compared before spare seats. earlier date wins even if later time of day
    // seats only matter between suggestions with the same date and time
    @Test
    void earlierDateAndTimeBeatLaterOnesBeforeSeatsAreCompared() {
        // two day window (candidates can land on either day)
        DraftEvent event = new DraftEvent("e1", "Two days", ids(1, 2), DAY, DAY.plusDays(1), 60, java.util.Set.of(), 2);

        // day 2 free at 9:00, but day one only free at 15:00
        List<Availability> free = new ArrayList<>(freeAll(ids(1, 2), DAY.plusDays(1), "09:00", "10:00"));
        free.addAll(freeAll(ids(1, 2), DAY, "15:00", "16:00"));
        AppState state = state(students(2), List.of(room("R1", 30), room("R2", 2)), free, List.of(event));
        List<Suggestion> suggestions = engine.suggest(state, event).suggestions();

        // d1 at 15:00 beats d2 at 9:00 because date is compared first
        assertEquals(DAY, suggestions.getFirst().candidate().date());

        // same date and time: the room with fewer unused seats (R2) comes before R1.
        assertEquals("R2", suggestions.get(0).candidate().room().id());
        assertEquals("R1", suggestions.get(1).candidate().room().id());

        // day two options come after both day 1 options
        assertEquals(DAY.plusDays(1), suggestions.get(2).candidate().date());
    }

    // more than 5 valid, equally good options, but only 5 returned (in order of room id)
    @Test
    void onlyTheTopFiveAreReturned() {
        DraftEvent event = draft("e1", ids(1, 2), 60, 2);

        // 7 identical rooms, all suggestions tie until room id breaks it
        List<campusplan.domain.Room> rooms = List.of(room("R1", 4), room("R2", 4), room("R3", 4), room("R4", 4), room("R5", 4), room("R6", 4), room("R7", 4));
        AppState state = state(students(2), rooms, freeAll(ids(1, 2), DAY, "09:00", "10:00"), List.of(event));
        List<String> roomIds = engine.suggest(state, event).suggestions().stream().map(s -> s.candidate().room().id()).toList();
        assertEquals(List.of("R1", "R2", "R3", "R4", "R5"), roomIds);
    }

    // every rejected candidate counted once (either room/attendance issue)
    // valid + room + attendance should add up to all things that were considered
    @Test
    void roomAndAttendanceRejectionCountsDoNotOverlapAndAddUp() {
        // event needs a projector and at least three attendees
        DraftEvent event = draft("e1", ids(1, 4), 60, 3, Feature.PROJECTOR);

        // R1 lacks the projector (room failure), R2 has it but only three people are free for 2 hours (attendance failure)
        AppState state = state(students(4), List.of(room("R1", 6), room("R2", 6, Feature.PROJECTOR)), freeAll(ids(1, 3), DAY, "10:00", "12:00"), List.of(event));
        SuggestionResult result = engine.suggest(state, event);

        int valid = 3; // 10:00, 10:30 and 11:00 in R2
        assertEquals(valid, result.suggestions().size());
        assertEquals(2 * STARTS_PER_ROOM, result.candidatesConsidered());

        // every r1 slot fails on the room, remaining r2 slots fail on attendance
        assertEquals(STARTS_PER_ROOM, result.roomRejections());
        assertEquals(STARTS_PER_ROOM - valid, result.attendanceRejections());

        // nothing double counted or missed
        assertEquals(result.candidatesConsidered(), valid + result.roomRejections() + result.attendanceRejections());
        assertEquals(STARTS_PER_ROOM, (int) result.rejectionsByRule().get("room features"));
    }

    // reports reason from the first rule that fails
    @Test
    void rejectionNamesTheFirstFailingRule() {
        // event needs whiteboard and projector, but room only has whiteboard
        DraftEvent event = draft("e1", ids(1, 2), 60, 2, Feature.WHITEBOARD, Feature.PROJECTOR);
        AppState state = state(students(2), List.of(room("R1", 6, Feature.WHITEBOARD)), freeAll(ids(1, 2), DAY, "09:00", "10:00"), List.of(event));
        var candidate = new Candidate(state.rooms().getFirst(), DAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

        // standard engine notices missing projector
        assertEquals("Room R1 lacks PROJECTOR", engine.rejection(state, event, candidate).orElseThrow());

        // engine that only checks capacity doesn't complain because the room is big enough
        var small = new SuggestionEngine(List.of(new RoomCapacityRule()));
        assertTrue(small.rejection(state, event, candidate).isEmpty());
    }

    // search stays inside opening and closing hours
    @Test
    void windowAndOpeningHoursLimitTheCandidates() {
        // a 180 minute event must end by 22:00, so the last start is 19:00 (23 starts from 08:00)
        DraftEvent event = draft("e1", ids(1, 2), 180, 2);

        // no one is available, so no suggestions, but candidates are still counted
        AppState state = state(students(2), List.of(room("R1", 4)), List.of(), List.of(event));
        assertEquals(23, engine.suggest(state, event).candidatesConsidered());
    }
}