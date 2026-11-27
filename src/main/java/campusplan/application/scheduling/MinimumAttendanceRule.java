package campusplan.application.scheduling;

import java.util.Optional;

// making sure enough invitees are able to come at the proposed time
public final class MinimumAttendanceRule implements CandidateRule {
    @Override
    public String name() {
        return "minimum attendance"; // name for logs
    }

    @Override
    public Kind kind() { // whether enough people can come, so considered to be an attendance issue
        return Kind.ATTENDANCE;
    }

    // compares invitees that can come to event's proposed time to the event's minimum attendance
    @Override
    public Optional<String> check(Candidate candidate, PlanningContext context) {
        int eligible = context.attendance(candidate.date(), candidate.start(), candidate.end()).eligible().size(); // number of ppl that can come during this time
        int minimum = context.event().minimumAttendance();
        if (eligible < minimum) { // if too little people can come, returns both numbers to see how many invitees short we are
            return Optional.of("only " + eligible + " eligible, minimum is " + minimum);
        }
        return Optional.empty(); // returns room if enough people can come
    }
}