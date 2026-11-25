package campusplan.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 
 * Tests the Availability rules valid boundaries and full event coverage.
 */
class AvailabilityTest {
    // A fixed date used by every test so results never depend on todays date.
    private static final LocalDate DAY = LocalDate.of(2026, 10, 26);


    /**
     * for an interval whose end comes before its start is rejected
     * 
     * 11:00,10:00 ends one hour before it starts
     * 10:30,10:00 ends one slot before it starts
     * 22:00,08:00 the whole day backwards
     */
    @ParameterizedTest
    @CsvSource({"11:00,10:00", "10:30,10:00", "22:00,08:00"})
    void endBeforeStartIsRejected(String start, String end) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> interval("s1", DAY, start, end));
        assertEquals("Start time must be before end time.", error.getMessage());
    }

    /**
     * A length zero interval is a bad range
     */
    @ParameterizedTest
    @CsvSource({"08:00,08:00", "10:00,10:00", "22:00,22:00"})
    void zeroLengthIsRejected(String start, String end) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> interval("s1", DAY, start, end));
        assertEquals("Start time must be before end time.", error.getMessage());
    }

    /** 
     * Start or end off the 30 minute mark is rejected
     * 
     * so that means that
     * 10:15,11:00 and 10:45,11:30 start on a quarter hour
     * 10:00,11:15 ends on a quarter hour
     * 10:01,11:00 and 10:00,10:59 are one minute off
     * 10:00:01,11:00 and 10:00,11:00:01 are one second off
     */
    @ParameterizedTest 
    @CsvSource ({"10:15,11:00", "10:45,11:30", "10:00, 11:15", "10:01,11:00", "10:00, 10:59", "10:00:01,11:00", 
        "10:00,11:00:01"
    })
    void timesOffTheThirtyMinuteMarkAreRejected(String start, String end){
        IllegalArgumentException error = assertThrows (IllegalArgumentException.class, () -> interval("s1", DAY,start,end));
        assertEquals("Times must use 30-minute boundaries.", error.getMessage());
    }

    @Test 
    void aNanosecondOffTheMarkIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Availability("s1", DAY, LocalTime.of(10, 0, 0, 1), LocalTime.of(11, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> new Availability("s1", DAY, LocalTime.of(10, 0), LocalTime.of(11, 0, 0, 1)));
    }

    @ParameterizedTest 
    @CsvSource ({"10:00,10:30", "10:30,11:00", "10:30,11:30", "10:00,11:00"})
    void startAndEndOnTheHourOrHalfAreAccepted(String start, String end){
        Availability slot = interval("s1", DAY, start,end);
        assertEquals(LocalTime.parse(start),slot.start());
        assertEquals(LocalTime.parse(end),slot.end());
    }


    /**
     * The earliest slot, the latest slot and the whole day are inside 08:00-22:00
     */
    @ParameterizedTest 
    @CsvSource({"08:00,08:30", "21:30,22:00", "08:00,22:00"})
    void intervalsTouchingOpeningAndClosingAreAccepted(String start, String end){
        Availability slot = interval("s1", DAY,start,end);
        assertEquals(DAY, slot.date());
        assertEquals(LocalTime.parse(start), slot.start());
        assertEquals(LocalTime.parse(end), slot.end());
    }
    /**
     * One slot outside opening hours on either side is rejected.
     *
     * 07:30,08:00 ends exactly at opening but starts before it
     * 07:30,09:00 starts before opening
     * 00:00,08:00 starts at midnight
     * 21:30,22:30 ends after closing
     * 22:00,22:30 starts exactly at closing
     */
    @ParameterizedTest
    @CsvSource({"07:30,08:00", "07:30,09:00", "00:00,08:00", "21:30,22:30", "22:00,22:30"})
    void intervalsOutsideOpeningHoursAreRejected(String start, String end) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> interval("s1", DAY, start, end));
        assertEquals("Availability must be within 08:00-22:00.", error.getMessage());
    }

    @Test
    void missingFieldsAreRejected(){
        assertThrows(IllegalArgumentException.class,
            () -> new Availability("s1", null, LocalTime.NOON, LocalTime.of(13, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> new Availability("s1", DAY, null, LocalTime.of(13, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> new Availability("s1", DAY, LocalTime.NOON, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Availability(null, DAY, LocalTime.NOON, LocalTime.of(13, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> new Availability("   ", DAY, LocalTime.NOON, LocalTime.of(13, 0)));
    }

    @Test 
    void studentIdIsTrimmed() {
        assertEquals("s1", interval(" s1 ", DAY, "10:00", "11:00").studentId());
    }

    

    /** covers() is true only when the whole event fits on the same date. */
    @Test
    void coverageRequiresTheWholeEventOnTheSameDate() {
        Availability day = interval("s1", DAY, "08:00", "22:00");
        assertTrue(day.covers(DAY, LocalTime.of(8, 0), LocalTime.of(22, 0)));
        assertFalse(day.covers(DAY.plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0)));
        assertFalse(day.covers(DAY.minusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0)));
        assertFalse(day.covers(DAY, LocalTime.of(7, 30), LocalTime.of(9, 0)));
        assertFalse(day.covers(DAY, LocalTime.of(21, 0), LocalTime.of(22, 30)));
        assertFalse(day.covers(DAY, LocalTime.of(10, 0), LocalTime.of(10, 0)));
    }

    /**
     * Inside a 10:00-12:00 interval, an event may start at 10:00 and end at 12:00,
     * but not stick out by even one slot on either side.
     *
     * Columns: event start, event end, expected coverage.
     */
    @ParameterizedTest
    @CsvSource({
            "10:00,12:00,true",  // exactly the interval
            "10:00,10:30,true", // first slot
            "11:30,12:00,true",  // last slot
            "10:30,11:30,true", // strictly inside
            "09:30,10:30,false", // starts one slot early
            "11:30,12:30,false", // ends one slot late
            "09:00,13:00,false", // contains the interval
            "09:00,10:00,false",// ends where the interval starts
            "12:00,13:00,false", // starts where the interval ends
            "11:00,10:00,false"  // backwards event
    })
    void coverageBoundariesAreHalfOpen(String start, String end, String expected) {
        Availability slot = interval("s1", DAY, "10:00", "12:00");
        assertEquals(Boolean.parseBoolean(expected),
                slot.covers(DAY, LocalTime.parse(start), LocalTime.parse(end)));
    }
    

    @Test
    void overlappingIntervalsMergeIntoOne() {
        List<Availability> merged = AppState.mergeIntervals(List.of(
                interval("s1", DAY, "10:00", "12:00"),
                interval("s1", DAY, "11:00", "13:00")));
        assertEquals(List.of(interval("s1", DAY, "10:00", "13:00")), merged);
    }

    /** Touching intervals join so an event across the shared boundary is covered. */
    @Test
    void adjacentIntervalsMergeAndCoverAnEventAcrossTheBoundary() {
        List<Availability> merged = AppState.mergeIntervals(List.of(
                interval("s1", DAY, "10:00", "11:00"),
                interval("s1", DAY, "11:00", "12:00")));
        assertEquals(List.of(interval("s1", DAY, "10:00", "12:00")), merged);
        assertTrue(merged.get(0).covers(DAY, LocalTime.of(10, 30), LocalTime.of(11, 30)));
    }

    @Test
    void containedAndDuplicateIntervalsAreAbsorbed() {
        List<Availability> merged = AppState.mergeIntervals(List.of(
                interval("s1", DAY, "10:00", "14:00"),
                interval("s1", DAY, "11:00", "12:00"),
                interval("s1", DAY, "10:00", "14:00")));
        assertEquals(List.of(interval("s1", DAY, "10:00", "14:00")), merged);
    }

    /** A 30 minute gap keeps intervals apart, so an event across the gap is not covered. */
    @Test
    void aGapKeepsIntervalsSeparate() {
        List<Availability> merged = AppState.mergeIntervals(List.of(
                interval("s1", DAY, "10:00", "11:00"),
                interval("s1", DAY, "11:30", "12:30")));
        assertEquals(2, merged.size());
        assertTrue(merged.stream()
                .noneMatch(slot -> slot.covers(DAY, LocalTime.of(10, 30), LocalTime.of(12, 0))));
    }

    @Test
    void differentStudentsAndDatesNeverMerge() {
        List<Availability> merged = AppState.mergeIntervals(List.of(
                interval("s1", DAY, "10:00", "12:00"),
                interval("s2", DAY, "11:00", "13:00"),
                interval("s1", DAY.plusDays(1), "11:00", "13:00")));
        assertEquals(3, merged.size());
    }

    /** Input order does not matter so an unsorted chain still becomes one interval. */
    @Test
    void mergingDoesNotDependOnInputOrder() {
        List<Availability> merged = AppState.mergeIntervals(List.of(
                interval("s1", DAY, "13:00", "14:00"),
                interval("s1", DAY, "10:00", "11:00"),
                interval("s1", DAY, "11:00", "13:00")));
        assertEquals(List.of(interval("s1", DAY, "10:00", "14:00")), merged);
    }
    /**
     * Helper that builds an Availability from text times like 08:00
     * so the test rows stay short and readable.
     */
    private static Availability interval(String student, LocalDate day, String start, String end) {
        return new Availability(student, day, LocalTime.parse(start), LocalTime.parse(end));
    }



}