package campusplan.application.scheduling;

import java.util.Optional;

// room holds every invitee even when only some of them can attend
public final class RoomCapacityRule implements CandidateRule {
    @Override
    public String name() { // for identifying in logs when rule is invoked
        return "room capacity";
    }

    // failures are reported as room problems bc capacity is a room property
    @Override
    public Kind kind() {
        return Kind.ROOM;
    }

    // checking room capacity to the number of people invited
    @Override
    public Optional<String> check(Candidate candidate, PlanningContext context) {
        int invited = context.event().inviteeIds().size(); // counting invitees
        if (candidate.room().capacity() < invited) { // only reject when invitees > room capacity
            return Optional.of(candidate.room().name() + " holds " + candidate.room().capacity() + " but " + invited + " are invited");
        }
        return Optional.empty(); // returns room if invitees <= room capacity
    }
}