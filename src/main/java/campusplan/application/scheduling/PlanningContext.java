package campusplan.application.scheduling;

import campusplan.domain.AppState;
import campusplan.domain.DraftEvent;
import campusplan.domain.Eligibility;
import campusplan.domain.Student;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the rules need to know about one planning run
 * The current state and who can attend each interval.
 * 
 * Eligibility depends only on the time and not the room so it is worked out
 * once per interval and reused across rooms 
 */
public final class PlanningContext{
    private final AppState state;
    private final DraftEvent event;
    private final Map<String, Student> students = new HashMap<>();
    private final Map<String, Attendance> cache = new HashMap<>();

    public PlanningContext(AppState state, DraftEvent event) {
        this.state = state;
        this.event = event;
        state.students().forEach(student -> students.put(student.id(), student));
        
    }
     public AppState state() {
        return state;
    }

    public DraftEvent event() {
        return event;
    }

    
    /** Who can and cannot attend an interval, in invitee order. */
    public Attendance attendance(LocalDate date, LocalTime start, LocalTime end){
        return cache.computeIfAbsent(date + "T" + start, key -> {
            List<Student> eligible = new ArrayList<>();
            List<Ineligible> ineligible = new ArrayList<>();
            for (String id : event.inviteeIds()) {
                Optional<String> reason = Eligibility.ineligibleReason(state, id, date, start, end, event.id());
                if (reason.isPresent()) {
                    ineligible.add(new Ineligible(students.get(id), reason.get()));
                } else {
                    eligible.add(students.get(id));
                }
            }
            return new Attendance(List.copyOf(eligible), List.copyOf(ineligible));
        });
    }

    
    public record Ineligible(Student student, String reason) {}

    public record Attendance(List<Student> eligible, List<Ineligible> ineligible) {}

}