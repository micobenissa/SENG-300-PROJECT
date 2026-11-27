package campusplan.application.scheduling;

import campusplan.domain.DraftEvent;

import java.util.Optional;

// checking to make sure that the booking time is valid for the room, no overlapping time slots with other bookings.
public final class RoomFreeRule implements CandidateRule {
    @Override
    public String name() { // identifying name for logs
        return "room reservation";
    }

    @Override
    public Kind kind() { // falls under room issue (double booked room)
        return Kind.ROOM;
    }

    @Override // checks for other events that overlap with this room at the proposed time.
    public Optional<String> check(Candidate candidate, PlanningContext context) {
        // if there's an event that overlaps, names the room and the event that's hosting it so the booker knows who to talk to or if they should reschedule
        return context.state().events().stream().filter(other -> !other.id().equals(context.event().id())).filter(other -> candidate.room().id().equals(other.roomId())).filter(other -> other.overlaps(candidate.date(), candidate.start(), candidate.end())).findFirst().map((DraftEvent other) -> candidate.room().name() + " is reserved by \"" + other.title() + "\"");
    }
}