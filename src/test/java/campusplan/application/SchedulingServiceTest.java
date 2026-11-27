package campusplan.application;

import campusplan.domain.AppState;
import campusplan.domain.AttendanceResponse;
import campusplan.domain.DraftEvent;
import campusplan.domain.EventStatus;
import campusplan.domain.Feature;
import campusplan.domain.Student;
import campusplan.persistence.InMemoryStorage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static campusplan.application.scheduling.PlanFixture.*;
import static org.junit.jupiter.api.Assertions.*;

/** Confirming, cancelling, responses, availability protection, listing and export (FR-05 to FR-07). */
class SchedulingServiceTest {
    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final Clock CLOCK = Clock.fixed(LocalDate.of(2026, 11, 15).atStartOfDay(ZoneOffset.UTC).toInstant(),
            ZoneOffset.UTC);

    /** Six students, rooms R1 (6 seats) and R2 (8 seats); students 1-5 are free 09:00-12:00. */
    private CampusPlanService service() throws IOException {
        AppState state = state(students(6), List.of(room("R1", 6, Feature.WHITEBOARD), room("R2", 8)),
                freeAll(ids(1, 5), DAY, "09:00", "12:00"), List.of());
        return new CampusPlanService(new InMemoryStorage(state), CLOCK);
    }

    private DraftEvent newDraft(CampusPlanService service, String title) {
        return service.createDraft(title, ids(1, 6), DAY, DAY, 60, Set.of(), 4);
    }

    private DraftEvent book(CampusPlanService service, String title, String room, LocalTime start) {
        DraftEvent draft = newDraft(service, title);
        return service.confirmSuggestion(draft.id(), room, DAY, start);
    }

    // ------------------------------------------------------------------ FR-05 confirm

    @Test
    void confirmingASuggestionSchedulesTheEventReservesTheRoomAndKeepsResponsesPending() throws IOException {
        CampusPlanService service = service();
        DraftEvent draft = newDraft(service, "Study");
        var best = service.suggestions(draft.id()).suggestions().getFirst();
        assertEquals("R1", best.candidate().room().id());
        DraftEvent scheduled = service.confirmSuggestion(draft.id(), "R1", DAY, NINE);
        assertEquals(draft.id(), scheduled.id());
        assertEquals(EventStatus.SCHEDULED, scheduled.status());
        assertEquals("R1", scheduled.roomId());
        assertEquals(DAY, scheduledDate(scheduled));
        assertEquals(LocalTime.of(10, 0), scheduled.endTime());
        // Eligible invitees are not accepted automatically.
        assertTrue(scheduled.responses().values().stream().allMatch(r -> r == AttendanceResponse.PENDING));
        assertEquals(scheduled, service.event(draft.id()));
    }

    private static LocalDate scheduledDate(DraftEvent event) {
        return event.scheduledDate();
    }

    @Test
    void staleSuggestionIsRejectedAndNothingChanges() throws IOException {
        CampusPlanService service = service();
        DraftEvent first = newDraft(service, "First");
        DraftEvent second = newDraft(service, "Second");
        // Both drafts are shown R1 at 09:00, then the first one books it.
        assertEquals("R1", service.suggestions(second.id()).suggestions().getFirst().candidate().room().id());
        service.confirmSuggestion(first.id(), "R1", DAY, NINE);
        AppState before = service.state();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.confirmSuggestion(second.id(), "R1", DAY, NINE));
        assertTrue(error.getMessage().contains("no longer valid"));
        assertTrue(error.getMessage().contains("reserved by \"First\""));
        assertSame(before, service.state());
        assertEquals(EventStatus.DRAFT, service.event(second.id()).status());
    }

    @Test
    void invalidSelectionsAreRejectedWithoutChangingState() throws IOException {
        CampusPlanService service = service();
        DraftEvent draft = newDraft(service, "Study");
        AppState before = service.state();
        assertThrows(IllegalArgumentException.class, () -> service.confirmSuggestion(draft.id(), "nope", DAY, NINE));
        assertThrows(IllegalArgumentException.class,
                () -> service.confirmSuggestion(draft.id(), "R1", DAY.plusDays(1), NINE)); // outside window
        assertThrows(IllegalArgumentException.class,
                () -> service.confirmSuggestion(draft.id(), "R1", DAY, LocalTime.of(7, 30))); // before opening
        assertThrows(IllegalArgumentException.class,
                () -> service.confirmSuggestion(draft.id(), "R1", DAY, LocalTime.of(21, 30))); // ends after 22:00
        assertThrows(IllegalArgumentException.class,
                () -> service.confirmSuggestion(draft.id(), "R1", DAY, LocalTime.of(9, 15))); // not a boundary
        assertThrows(IllegalArgumentException.class,
                () -> service.confirmSuggestion(draft.id(), "R1", DAY, LocalTime.of(14, 0))); // nobody free
        assertThrows(IllegalArgumentException.class, () -> service.confirmSuggestion(draft.id(), "R1", null, NINE));
        assertThrows(IllegalArgumentException.class, () -> service.confirmSuggestion("missing", "R1", DAY, NINE));
        assertSame(before, service.state());
    }

    @Test
    void suggestionsThemselvesDoNotReserveAnything() throws IOException {
        CampusPlanService service = service();
        DraftEvent draft = newDraft(service, "Study");
        AppState before = service.state();
        service.suggestions(draft.id());
        service.suggestions(draft.id());
        assertSame(before, service.state());
        assertFalse(service.canUndo() && service.undo().startsWith("Schedule"));
    }

    @Test
    void scheduledAndCancelledEventsCannotBeScheduledAgain() throws IOException {
        CampusPlanService service = service();
        DraftEvent scheduled = book(service, "Study", "R1", NINE);
        assertThrows(IllegalArgumentException.class, () -> service.suggestions(scheduled.id()));
        assertThrows(IllegalArgumentException.class,
                () -> service.confirmSuggestion(scheduled.id(), "R2", DAY, NINE));
        service.cancelEvent(scheduled.id(), true);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.confirmSuggestion(scheduled.id(), "R1", DAY, NINE));
        assertEquals("Cancelled events cannot be scheduled again.", error.getMessage());
        assertThrows(IllegalArgumentException.class, () -> service.suggestions(scheduled.id()));
    }

    @Test
    void scheduledEventPlanningFieldsStayFixed() throws IOException {
        CampusPlanService service = service();
        DraftEvent scheduled = book(service, "Study", "R1", NINE);
        AppState before = service.state();
        assertThrows(IllegalArgumentException.class, () -> service.editDraft(scheduled.id(), "New", ids(1, 6),
                DAY, DAY, 60, Set.of(), 4));
        assertSame(before, service.state());
    }

    // ------------------------------------------------------------------ FR-05 cancel

    @Test
    void cancellationNeedsConfirmationFreesTheRoomAndKeepsTheRecordAndResponses() throws IOException {
        CampusPlanService service = service();
        DraftEvent scheduled = book(service, "Study", "R1", NINE);
        service.recordResponse(scheduled.id(), "s1", AttendanceResponse.ACCEPTED);
        AppState before = service.state();
        // Declining the confirmation changes nothing.
        assertThrows(IllegalArgumentException.class, () -> service.cancelEvent(scheduled.id(), false));
        assertSame(before, service.state());

        DraftEvent cancelled = service.cancelEvent(scheduled.id(), true);
        assertEquals(EventStatus.CANCELLED, cancelled.status());
        assertNull(cancelled.roomId());
        assertEquals(AttendanceResponse.ACCEPTED, cancelled.responses().get("s1"));
        assertEquals(1, service.state().events().size());
        // The room is free again, so another event can take it, and s1's commitment is released.
        DraftEvent other = book(service, "Other", "R1", NINE);
        service.recordResponse(other.id(), "s1", AttendanceResponse.ACCEPTED);
        assertEquals(AttendanceResponse.ACCEPTED, service.event(other.id()).responses().get("s1"));
    }

    @Test
    void onlyAScheduledEventCanBeCancelled() throws IOException {
        CampusPlanService service = service();
        DraftEvent draft = newDraft(service, "Study");
        assertThrows(IllegalArgumentException.class, () -> service.cancelEvent(draft.id(), true));
        DraftEvent scheduled = service.confirmSuggestion(draft.id(), "R1", DAY, NINE);
        service.cancelEvent(scheduled.id(), true);
        assertThrows(IllegalArgumentException.class, () -> service.cancelEvent(scheduled.id(), true));
        assertThrows(IllegalArgumentException.class, () -> service.cancelEvent("missing", true));
    }

    // ------------------------------------------------------------------ FR-06 responses

    @Test
    void acceptedNeedsFullAvailabilityAndEligibleCountsStaySeparateFromResponses() throws IOException {
        CampusPlanService service = service();
        DraftEvent scheduled = book(service, "Study", "R1", NINE);
        // Five students are eligible, yet every response is still PENDING.
        assertEquals(5, service.eligibleStudents(scheduled.id()).size());
        assertEquals(0, service.event(scheduled.id()).responses().values().stream()
                .filter(r -> r == AttendanceResponse.ACCEPTED).count());
        AppState before = service.state();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.recordResponse(scheduled.id(), "s6", AttendanceResponse.ACCEPTED));
        assertTrue(error.getMessage().contains("not available for the whole time"));
        assertSame(before, service.state());
        // DECLINED needs no availability.
        service.recordResponse(scheduled.id(), "s6", AttendanceResponse.DECLINED);
        service.recordResponse(scheduled.id(), "s1", AttendanceResponse.ACCEPTED);
        assertEquals(AttendanceResponse.DECLINED, service.event(scheduled.id()).responses().get("s6"));
        assertEquals(5, service.eligibleStudents(scheduled.id()).size());
    }

    @Test
    void overlappingAcceptedAttendanceIsRejectedAndReleasedByDeclinedOrPending() throws IOException {
        CampusPlanService service = service();
        DraftEvent first = book(service, "First", "R1", NINE);
        DraftEvent second = book(service, "Second", "R2", LocalTime.of(9, 30));
        service.recordResponse(first.id(), "s1", AttendanceResponse.ACCEPTED);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.recordResponse(second.id(), "s1", AttendanceResponse.ACCEPTED));
        assertTrue(error.getMessage().contains("already accepted \"First\" 09:00-10:00"));
        // The overlapping student is no longer eligible for the second event, but other students still are.
        assertFalse(service.eligibleStudents(second.id()).stream().anyMatch(s -> s.id().equals("s1")));
        // Setting PENDING releases the commitment.
        service.recordResponse(first.id(), "s1", AttendanceResponse.PENDING);
        service.recordResponse(second.id(), "s1", AttendanceResponse.ACCEPTED);
        // Now DECLINED on the second releases it for the first.
        service.recordResponse(second.id(), "s1", AttendanceResponse.DECLINED);
        service.recordResponse(first.id(), "s1", AttendanceResponse.ACCEPTED);
        assertEquals(AttendanceResponse.ACCEPTED, service.event(first.id()).responses().get("s1"));
    }

    @Test
    void adjacentEventsCanBothBeAccepted() throws IOException {
        CampusPlanService service = service();
        DraftEvent first = book(service, "First", "R1", NINE);
        DraftEvent second = book(service, "Second", "R2", LocalTime.of(10, 0));
        service.recordResponse(first.id(), "s1", AttendanceResponse.ACCEPTED);
        service.recordResponse(second.id(), "s1", AttendanceResponse.ACCEPTED);
        assertEquals(AttendanceResponse.ACCEPTED, service.event(second.id()).responses().get("s1"));
    }

    @Test
    void responseErrorsLeaveStateUnchanged() throws IOException {
        CampusPlanService service = service();
        DraftEvent draft = newDraft(service, "Draft");
        DraftEvent scheduled = book(service, "Study", "R1", NINE);
        AppState before = service.state();
        assertThrows(IllegalArgumentException.class,
                () -> service.recordResponse(draft.id(), "s1", AttendanceResponse.ACCEPTED)); // not scheduled
        assertThrows(IllegalArgumentException.class,
                () -> service.recordResponse(scheduled.id(), "other", AttendanceResponse.DECLINED));
        assertThrows(IllegalArgumentException.class, () -> service.recordResponse(scheduled.id(), "s1", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.recordResponse("missing", "s1", AttendanceResponse.DECLINED));
        assertThrows(IllegalArgumentException.class, () -> service.eligibleStudents(draft.id()));
        assertSame(before, service.state());
    }

    @Test
    void responsesOnACancelledEventAreFrozen() throws IOException {
        CampusPlanService service = service();
        DraftEvent scheduled = book(service, "Study", "R1", NINE);
        service.cancelEvent(scheduled.id(), true);
        assertThrows(IllegalArgumentException.class,
                () -> service.recordResponse(scheduled.id(), "s1", AttendanceResponse.DECLINED));
    }

    // ------------------------------------------------------------------ availability protection

    @Test
    void availabilityEditThatWouldBreakAcceptedAttendanceIsRejectedNamingTheEvent() throws IOException {
        CampusPlanService service = service();
        DraftEvent scheduled = book(service, "Study", "R1", NINE);
        service.recordResponse(scheduled.id(), "s1", AttendanceResponse.ACCEPTED);
        AppState before = service.state();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                service.setAvailability("s1", DAY, List.of(free("s1", DAY, "10:00", "12:00"))));
        assertTrue(error.getMessage().contains("\"Study\" 09:00-10:00"));
        assertSame(before, service.state());
        assertThrows(IllegalArgumentException.class, () -> service.setAvailability("s1", DAY, List.of()));
        assertSame(before, service.state());
        // An edit that still covers the event is fine, and merged touching intervals still cover it.
        service.setAvailability("s1", DAY, List.of(free("s1", DAY, "08:30", "09:30"), free("s1", DAY, "09:30", "11:00")));
        // Once the response is released the time can be removed.
        service.recordResponse(scheduled.id(), "s1", AttendanceResponse.PENDING);
        assertTrue(service.setAvailability("s1", DAY, List.of()).isEmpty());
    }

    @Test
    void availabilityEditsOnOtherDatesAndStudentsAreNotBlocked() throws IOException {
        CampusPlanService service = service();
        DraftEvent scheduled = book(service, "Study", "R1", NINE);
        service.recordResponse(scheduled.id(), "s1", AttendanceResponse.ACCEPTED);
        LocalDate other = DAY.plusDays(1);
        assertEquals(1, service.setAvailability("s1", other, List.of(free("s1", other, "10:00", "11:00"))).size());
        assertTrue(service.setAvailability("s2", DAY, List.of()).isEmpty());
    }

    // ------------------------------------------------------------------ FR-07 list and export

    @Test
    void eventsListByDateThenTimeAndFilterByStatusAndDate() throws IOException {
        CampusPlanService service = service();
        DraftEvent late = book(service, "Late", "R1", LocalTime.of(11, 0));
        DraftEvent early = book(service, "Early", "R2", NINE);
        DraftEvent draft = newDraft(service, "Plain draft");
        DraftEvent cancelled = book(service, "Gone", "R1", NINE);
        service.cancelEvent(cancelled.id(), true);

        assertEquals(List.of("Early", "Late", "Plain draft", "Gone"),
                service.listEvents(null, null).stream().map(DraftEvent::title).toList().stream()
                        .sorted(java.util.Comparator.comparingInt(List.of("Early", "Late", "Plain draft", "Gone")::indexOf))
                        .toList());
        assertEquals(List.of(early.id(), late.id()),
                service.listEvents(EventStatus.SCHEDULED, null).stream().map(DraftEvent::id).toList());
        assertEquals(List.of(draft.id()),
                service.listEvents(EventStatus.DRAFT, DAY).stream().map(DraftEvent::id).toList());
        assertEquals(List.of(cancelled.id()),
                service.listEvents(EventStatus.CANCELLED, null).stream().map(DraftEvent::id).toList());
        assertTrue(service.listEvents(null, DAY.plusDays(3)).isEmpty());
        assertEquals(4, service.listEvents(null, DAY).size());
    }

    @Test
    void rosterCsvHasTheRequiredHeaderAndQuotesCommasQuotesAndLineBreaks() throws IOException {
        List<Student> students = new ArrayList<>(students(3));
        students.set(0, new Student("s1", "Reed, Alex", "alex@campus.test"));
        students.set(1, new Student("s2", "Sam \"Sammy\" Park", "sam@campus.test"));
        students.set(2, new Student("s3", "Line\nBreak", "lb@campus.test"));
        AppState state = state(students, List.of(room("R1", 6)), freeAll(ids(1, 3), DAY, "09:00", "12:00"), List.of());
        CampusPlanService service = new CampusPlanService(new InMemoryStorage(state), CLOCK);
        DraftEvent draft = service.createDraft("Roster", ids(1, 3), DAY, DAY, 60, Set.of(), 2);
        service.confirmSuggestion(draft.id(), "R1", DAY, NINE);
        service.recordResponse(draft.id(), "s1", AttendanceResponse.ACCEPTED);
        service.recordResponse(draft.id(), "s2", AttendanceResponse.DECLINED);
        AppState before = service.state();
        String csv = service.rosterCsv(draft.id());
        assertEquals("student_id,name,email,response\r\n"
                + "s1,\"Reed, Alex\",alex@campus.test,ACCEPTED\r\n"
                + "s2,\"Sam \"\"Sammy\"\" Park\",sam@campus.test,DECLINED\r\n"
                + "s3,\"Line\nBreak\",lb@campus.test,PENDING\r\n", csv);
        assertSame(before, service.state());
        assertThrows(IllegalArgumentException.class, () -> service.rosterCsv("missing"));
    }

    @Test
    void rosterCsvWorksForDraftsAndCancelledEventsToo() throws IOException {
        CampusPlanService service = service();
        DraftEvent draft = newDraft(service, "Study");
        assertEquals(7, service.rosterCsv(draft.id()).split("\r\n").length);
        DraftEvent scheduled = service.confirmSuggestion(draft.id(), "R1", DAY, NINE);
        service.cancelEvent(scheduled.id(), true);
        assertTrue(service.rosterCsv(scheduled.id()).startsWith("student_id,name,email,response\r\n"));
    }
}
