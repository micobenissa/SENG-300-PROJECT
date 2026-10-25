package campusplan.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** 
 * Tests the Availability rules valid boundaries and full event coverage.
 */
class AvailabilityTest {
    // A fixed date used by every test so results never depend on todays date.
    private static final LocalDate DAY = LocalDate.of(2026, 10, 26);

    /**
     * Runs once per row of the source and each row is a start,end pair
     * that must be rejected with an exception.
     *
     * 07:30,08:00 which starts before opening 
     * 21:30,22:30 which ends after closing 
     * 10:00,10:00 which has zero length 
     * 11:00,10:00 which starts after ends
     * 10:15,11:00 which starts not on a 30 minute boundary
     * 10:00,11:15 which ends not on a 30 minute boundary
     * 10:00:01,11:00 which starts with seconds
     * 10:00,11:00:01 which ends with seconds
     */
    @ParameterizedTest
    @CsvSource({"07:30,08:00", "21:30,22:30", "10:00,10:00", "11:00,10:00", "10:15,11:00", "10:00,11:15", "10:00:01,11:00", "10:00,11:00:01"})
    void rejectsInvalidAvailabilityBoundaries(String start, String end) {
        assertThrows(IllegalArgumentException.class,
                () -> interval("s1", DAY, start, end));
    }
    /**
     * Checks that covers() is true only when the whole event fits on the
     * same date and that missing or non boundary values are rejected.
     */
    @Test
    void availabilityRequiresFullCoverageOnSameDate() {
        // An interval for the whole open day
        Availability interval = interval("s1", DAY, "08:00", "22:00");
        // The full day fits exactly 
        assertTrue(interval.covers(DAY, LocalTime.of(8, 0), LocalTime.of(22, 0)));
        // Different date
        assertFalse(interval.covers(DAY.plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0)));
        // Event starts before the interval
        assertFalse(interval.covers(DAY, LocalTime.of(7, 30), LocalTime.of(9, 0)));
        // Event ends after the interval
        assertFalse(interval.covers(DAY, LocalTime.of(21, 0), LocalTime.of(22, 30)));
        // Zero length event
        assertFalse(interval.covers(DAY, LocalTime.of(10, 0), LocalTime.of(10, 0)));
        // Missing date, start or end must be rejected when creating an interval
        assertThrows(IllegalArgumentException.class,() -> new Availability("s1", null, LocalTime.NOON, LocalTime.of(13, 0)));
        assertThrows(IllegalArgumentException.class, () -> new Availability("s1", DAY, null, LocalTime.of(13, 0)));
        assertThrows(IllegalArgumentException.class,() -> new Availability("s1", DAY, LocalTime.NOON, null));


        // 1 nanosecond breaks the 30 minute rule
        assertThrows(IllegalArgumentException.class,
                () -> new Availability("s1", DAY, LocalTime.of(10, 0, 0, 1), LocalTime.of(11, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> new Availability("s1", DAY, LocalTime.of(10, 0), LocalTime.of(11, 0, 0, 1)));
    }
    /**
     * Helper that builds an Availability from text times like 08:00
     * so the test rows stay short and readable.
     */
    private static Availability interval(String student, LocalDate day, String start, String end) {
        return new Availability(student, day, LocalTime.parse(start), LocalTime.parse(end));
    }
}