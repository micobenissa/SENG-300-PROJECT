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
     * Helper that builds an Availability from text times like 08:00
     * so the test rows stay short and readable.
     */
    private static Availability interval(String student, LocalDate day, String start, String end) {
        return new Availability(student, day, LocalTime.parse(start), LocalTime.parse(end));
    }
}