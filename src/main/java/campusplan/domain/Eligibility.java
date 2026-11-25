package campusplan.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;


/**
 * These are the attendance rules shared by scheduling suggestions and response entry.
 * 
 * A student can attend an interval when one availability interval
 * covers all of it and the student has not accepted another scheduled event that overlaps it.
 * pending and declined responses, drafts and cancelled events never block anyone. 
 * Intervals are half open so times do not overlap.
 */
public final class Eligibility {
    private Eligibility() {}

    /*This checks whether the student’s availability covers the whole time */
    public static boolean isAvailable(AppState state, String studentId, LocalDate date,
        LocalTime start, LocalTime end) {
        
        return state.availability().stream().anyMatch(interval -> interval.studentId().equals(studentId)
         && interval.covers(date, start, end));                                
    }

    /** The other scheduled event that the student has already accepted at this time */
    public static Optional<DraftEvent> acceptedConflict(AppState state, String studentId, LocalDate date,
        LocalTime start, LocalTime end, String ignoredEvent){
          
        return state.events().stream()
            .filter(event -> !event.id().equals(ignoredEvent))
            .filter(event -> event.responses().get(studentId) == AttendanceResponse.ACCEPTED)
            .filter(event -> event.overlaps(date,start, end))
            .findFirst();
    } 
    
    /**
     * Why the student cannot attend the interval,
     * This combines the two checks and returns the reason a student can’t attend
     * 
     * @param ignoredEventId an event whose own commitments are not counted
     */
    public static Optional<String> ineligibleReason(AppState state, String studentId, LocalDate date,
        LocalTime start, LocalTime end, String ignoredEventId) {

        if (!isAvailable(state, studentId, date, start, end)) {
            return Optional.of("not available for the whole time");
        }
        return acceptedConflict(state, studentId, date, start, end, ignoredEventId).map(event ->
                "already accepted \"" + event.title() + "\" " + event.startTime() + "-" + event.endTime());
    }

}
