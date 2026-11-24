package campusplan.application.scheduling;

import campusplan.domain.AppState;
import campusplan.domain.DraftEvent;
import campusplan.domain.Student;

import java.util.HashMap;
import java.util.Map;

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

}