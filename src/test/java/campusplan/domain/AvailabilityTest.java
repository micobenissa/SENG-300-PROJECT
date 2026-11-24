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
     * Helper that builds an Availability from text times like 08:00
     * so the test rows stay short and readable.
     */
    private static Availability interval(String student, LocalDate day, String start, String end) {
        return new Availability(student, day, LocalTime.parse(start), LocalTime.parse(end));
    }
}