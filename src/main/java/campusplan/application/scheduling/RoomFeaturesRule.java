package campusplan.application.scheduling;

import campusplan.domain.Feature;

import java.util.Optional;
import java.util.stream.Collectors;

// making sure room must provide every feature the event requires
public final class RoomFeaturesRule implements CandidateRule {
    @Override
    public String name() { // for identifying in logs
        return "room features";
    }

    @Override
    public Kind kind() { // room features is a room issue
        return Kind.ROOM;
    }

    @Override
    public Optional<String> check(Candidate candidate, PlanningContext context) { // checking room features to ensure that room is valid if all features the requester needs is in the room
        String missing = context.event().requiredFeatures().stream().filter(feature -> !candidate.room().features().contains(feature)).sorted().map(Feature::name).collect(Collectors.joining(", "));
        if (!missing.isEmpty()) { // if room is missing a feature, return what feature the room is missing
            return Optional.of(candidate.room().name() + " lacks " + missing);
        }
        return Optional.empty(); // otherwise return room if it has everything the booker needs
    }
}