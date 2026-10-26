package campusplan.application;

import campusplan.domain.AppState;
import campusplan.domain.Availability;
import campusplan.domain.Student;
import campusplan.persistence.InMemoryStorage;
import campusplan.persistence.JsonStorage;
import campusplan.persistence.Storage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PastDateTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);
    private static final Clock CLOCK = clockOn(TODAY);
    @TempDir Path directory;

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void todayAndFutureDatesCanBeEnteredAndEdited(int daysAhead) throws IOException {
        var service = withStudents(new InMemoryStorage(), CLOCK);
        List<String> ids = service.state().students().stream().map(Student::id).toList();
        LocalDate date = TODAY.plusDays(daysAhead);
        service.setAvailability(ids.getFirst(), date, List.of(interval(ids.getFirst(), date)));
        var draft = service.createDraft("Study", ids, date, date, 30, Set.of(), 2);
        service.editDraft(draft.id(), "Updated", ids, date, date.plusDays(6), 60, Set.of(), 2);
        assertEquals(date, service.state().availability().getFirst().date());
        assertEquals(date, service.event(draft.id()).startDate());
        assertEquals("Updated", service.event(draft.id()).title());
    }

    @Test
    void pastAvailabilityEntryAndClearAreRejectedWithoutChangingState() throws IOException {
        var storage = new InMemoryStorage();
        var earlier = withStudents(storage, clockOn(TODAY.minusDays(1)));
        String id = earlier.state().students().getFirst().id();
        LocalDate yesterday = TODAY.minusDays(1);
        earlier.setAvailability(id, yesterday, List.of(interval(id, yesterday)));
        earlier.save();
        var service = new CampusPlanService(storage, CLOCK);
        AppState before = service.state();

        var error = assertThrows(IllegalArgumentException.class,
                () -> service.setAvailability(id, yesterday, List.of(interval(id, yesterday))));
        assertTrue(error.getMessage().contains("Availability date cannot be in the past"));
        assertTrue(error.getMessage().contains(TODAY.toString()));
        assertThrows(IllegalArgumentException.class, () -> service.setAvailability(id, yesterday, List.of()));
        assertSame(before, service.state());
        assertEquals(before, storage.load());
    }

    @Test
    void pastDraftCreationAndEditsAreRejectedWithoutChangingState() throws IOException {
        var service = withStudents(new InMemoryStorage(), CLOCK);
        List<String> ids = service.state().students().stream().map(Student::id).toList();
        var draft = service.createDraft("Study", ids, TODAY, TODAY, 30, Set.of(), 2);
        AppState before = service.state();
        LocalDate yesterday = TODAY.minusDays(1);

        // A window starting yesterday is invalid even when it ends in the future.
        var error = assertThrows(IllegalArgumentException.class, () -> service.createDraft(
                "Past", ids, yesterday, TODAY.plusDays(1), 30, Set.of(), 2));
        assertTrue(error.getMessage().contains("Event window start date cannot be in the past"));
        assertThrows(IllegalArgumentException.class, () -> service.editDraft(
                draft.id(), "Past edit", ids, yesterday, yesterday, 30, Set.of(), 2));
        assertSame(before, service.state());
    }

    @Test
    void expiredRecordsStillLoadSaveAndCanBeMovedToAFutureWindow() throws IOException {
        var storage = new JsonStorage(directory.resolve("history.json"));
        var earlier = withStudents(storage, CLOCK);
        List<String> ids = earlier.state().students().stream().map(Student::id).toList();
        earlier.setAvailability(ids.getFirst(), TODAY, List.of(interval(ids.getFirst(), TODAY)));
        var draft = earlier.createDraft("Study", ids, TODAY, TODAY, 30, Set.of(), 2);
        earlier.save();
        AppState saved = earlier.state();

        LocalDate laterDate = TODAY.plusDays(7);
        var later = new CampusPlanService(storage, clockOn(laterDate));
        assertEquals(saved, later.state());
        later.save();
        assertEquals(saved, storage.load());
        var empty = new CampusPlanService(new InMemoryStorage(), clockOn(laterDate));
        empty.loadSeed(saved);
        assertEquals(saved, empty.state());

        later.editDraft(draft.id(), "Moved", ids, laterDate, laterDate, 30, Set.of(), 2);
        later.save();
        assertEquals(laterDate, new CampusPlanService(storage, clockOn(laterDate))
                .event(draft.id()).startDate());
    }

    @Test
    void todayUsesTheClocksLocalDate() throws IOException {
        Clock localClock = Clock.fixed(Instant.parse("2026-10-26T01:00:00Z"),
                ZoneId.of("America/Toronto"));
        var service = withStudents(new InMemoryStorage(), localClock);
        String id = service.state().students().getFirst().id();
        LocalDate localToday = LocalDate.of(2026, 10, 25);
        service.setAvailability(id, localToday, List.of(interval(id, localToday)));
        assertThrows(IllegalArgumentException.class, () -> service.setAvailability(
                id, localToday.minusDays(1), List.of(interval(id, localToday.minusDays(1)))));
        assertEquals(localToday, service.state().availability().getFirst().date());
    }

    private static Clock clockOn(LocalDate date) {
        return Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    }

    private static CampusPlanService withStudents(Storage storage, Clock clock) throws IOException {
        var service = new CampusPlanService(storage, clock);
        service.createStudent("Alex", "alex@example.test");
        service.createStudent("Sam", "sam@example.test");
        return service;
    }

    private static Availability interval(String studentId, LocalDate date) {
        return new Availability(studentId, date, LocalTime.of(9, 0), LocalTime.of(10, 0));
    }
}
