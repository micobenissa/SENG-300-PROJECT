package campusplan.application.scheduling;

import campusplan.domain.Student;

import java.util.List;

// a valid candidate (room with required features, no overlap) with the people who can and cannot attend the event
public record Suggestion(Candidate candidate, List<Student> eligible, List<PlanningContext.Ineligible> ineligible, int invitees, int unusedSeats) {
    public int eligibleCount() { // getting number of people that can attend
        return eligible.size();
    }

    // short description of room and number of attendees that can come
    public String explanation() {
        return eligible.size() + " of " + invitees + " invitees are available, all room requirements are met.";
    }
}