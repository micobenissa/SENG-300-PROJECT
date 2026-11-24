package campusplan.domain;

import java.time.LocalDate;
import java.time.LocalTime;


/**
 * These are the attendance rules shared by scheduling suggestions and response entry.
 * 
 * A student can attend an interval when one availability interval
 * covers all of it and the student has no accepted another scheduled event that overlaps it.
 * pending and declined responses, drafts and cancelled events never block anyone. 
 * Intervals are half open so times to not overlap.
 * Eligibility
 */
public class Eligibility {
    private Eligibility() {}

    /*This checks whether the student’s availability covers the whole time */
    public static boolean isAvailable(AppState state, String studentId, LocalDate date,
        LocalTime start, LocalTime end) {
    return state.availability().stream().anyMatch(interval -> interval.studentId().equals(studentId)
    && interval.covers(date, start, end));                                
    }


    
}
