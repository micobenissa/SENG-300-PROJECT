package campusplan.domain;

import java.time.LocalDate;
import java.time.LocalTime;



/**
 * One block of time when a single student is free, on one date.
 *
 * This is a record that once created, the student, date and times
 * cannot change. A record is always valid because the constructor
 * rejects bad input.
 *
 * Intervals are half open meaning that for example
 * 13:00-14:00 does not overlap 14:00-15:00.
 * A student with no availability record for an event's time is unavailable.
 *
 * @param studentId ID of the student this availability belongs to
 * @param date the single date the interval is on
 * @param start start time (inclusive), on a 30 minute boundary
 * @param end end time (exclusive), on a 30 minute boundary
 */
public record Availability(String studentId, LocalDate date, LocalTime start, LocalTime end) {

    /**
     * Validates every field when an availability is created.
     * If any rule fails  an exception is thrown and no object is created,
     * so invalid data can never be stored.
     */
    public Availability {

        // checks the student ID text is valid
        studentId = Rules.text(studentId, "Student ID", Integer.MAX_VALUE);

        // rejects null values and with a message
        Rules.present(date, "Availability date");
        Rules.present(start, "Start time");
        Rules.present(end, "End time");

        // Start must be before end.
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }

        // The interval must fit inside opening hours which is from 08:00 to 22:00.
        if (start.isBefore(LocalTime.of(8, 0)) || end.isAfter(LocalTime.of(22, 0))) {
            throw new IllegalArgumentException("Availability must be within 08:00-22:00.");
        }

        // Both times must be on a 30 minute boundary 
        if (!boundary(start) || !boundary(end)) {
            throw new IllegalArgumentException("Times must use 30-minute boundaries.");
        }
    }

     /**
     * Checks that a time is exactly on a 30 minute boundary.
     *
     * @param time the time to check
     * @return true if the minute is 0 or 30 and there are no seconds or
     * fractions of a second (so 10:00:01 is rejected)
     */
    private static boolean boundary(LocalTime time) {
        return time.getMinute() % 30 == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    /**
     * Says whether this interval covers a whole event.
     * A student only counts as available if the entire event fits inside
     * one interval.
     *
     * @param day the date of the event
     * @param eventStart the event's start time
     * @param eventEnd the event's end time
     * @return true if the dates match, the event starts at or after this
     * interval's start, ends at or before its end, and has a positive length
     */
    public boolean covers(LocalDate day, LocalTime eventStart, LocalTime eventEnd) {
        // The whole event has to fit inside this interval.
        return date.equals(day) && !eventStart.isBefore(start) && !eventEnd.isAfter(end)
                && eventStart.isBefore(eventEnd);
    }
}
