package campusplan.application.scheduling;

import campusplan.domain.AppState;
import campusplan.domain.DraftEvent;
import campusplan.domain.Room;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// comes up with possible times and rooms for an event, throws out the ones that don't work, ranks what's left (FR-04).
public final class SuggestionEngine {
    // candidate rooms are every 30 min start in event's date window and room's opening hours. then we iterate through CandidateRules
    // ranked (first to last) by eligible count, date + time, unused seats (room capacity - # of invitees), room ID.

    public static final int MAX_SUGGESTIONS = 5; // most suggestions a search will return
    private static final LocalTime OPENS = LocalTime.of(8, 0); // earliest an event can start
    private static final LocalTime CLOSES = LocalTime.of(22, 0); // latest an event can end

    private final List<CandidateRule> rules; //the rules to apply

    // engine based on rules to apply and the order they should be run
    public SuggestionEngine(List<CandidateRule> rules) {
        this.rules = List.copyOf(rules); // copy to ensure that changes don't affect engine
    }

    // builds engine based on standard set of rules (room rules come before attendance rules)
    public static SuggestionEngine standard() {
        return new SuggestionEngine(List.of(new RoomCapacityRule(), new RoomFeaturesRule(),
                new RoomFreeRule(), new MinimumAttendanceRule()));
    }

    // looks for best rooms and times for an event
    public SuggestionResult suggest(AppState state, DraftEvent event) {
        PlanningContext context = new PlanningContext(state, event);

        // start all rules at 0 so we list all the rules (including the ones that don't reject anything)
        Map<String, Integer> byRule = new LinkedHashMap<>();
        rules.forEach(rule -> byRule.put(rule.name(), 0));

        // initializing variables
        int considered = 0;
        int room = 0;
        int attendance = 0;
        List<Suggestion> valid = new ArrayList<>();

        // going through each room, every day in the event window, and every possible start time
        for (Room candidateRoom : state.rooms()) {
            for (LocalDate date = event.startDate(); !date.isAfter(event.endDate()); date = date.plusDays(1)) {
                // stop offering start times once event begins to run past the closing time
                for (LocalTime start = OPENS; !start.plusMinutes(event.durationMinutes()).isAfter(CLOSES); start = start.plusMinutes(30)) {
                    Candidate candidate = new Candidate(candidateRoom, date, start, start.plusMinutes(event.durationMinutes()));
                    considered++;

                    // find first rule (if any) that rejects the candidate
                    CandidateRule failed = firstFailure(candidate, context).map(Failure::rule).orElse(null);
                    if (failed == null) { // if null, that means it passed every rule so it could work
                        valid.add(suggestion(candidate, context));
                    } else {
                        byRule.merge(failed.name(), 1, Integer::sum); // keeping track of rejection (count, rule, category)
                        if (failed.kind() == CandidateRule.Kind.ROOM) {
                            room++;
                        } else {
                            attendance++;
                        }
                    }
                }
            }
        }
        // best first, then keep only top few
        valid.sort(RANKING);
        return new SuggestionResult(List.copyOf(valid.subList(0, Math.min(MAX_SUGGESTIONS, valid.size()))),
                considered, room, attendance, Map.copyOf(byRule));
    }

    // checking one specific candidate and says why its rejected if it is
    public Optional<String> rejection(AppState state, DraftEvent event, Candidate candidate) {
        return firstFailure(candidate, new PlanningContext(state, event)).map(Failure::reason);
    }

    // runs a candidate through the rules (in order) and stops at first one that rejects
    private Optional<Failure> firstFailure(Candidate candidate, PlanningContext context) {
        for (CandidateRule rule : rules) {
            Optional<String> reason = rule.check(candidate, context);
            if (reason.isPresent()) {
                return Optional.of(new Failure(rule, reason.get()));
            }
        }
        return Optional.empty();
    }

    // turns valid candidate into a full suggestion with more details (who can/can't come)
    private Suggestion suggestion(Candidate candidate, PlanningContext context) {
        PlanningContext.Attendance attendance = context.attendance(candidate.date(), candidate.start(), candidate.end());
        int invitees = context.event().inviteeIds().size();
        return new Suggestion(candidate, attendance.eligible(), attendance.ineligible(), invitees, candidate.room().capacity() - invitees);
    }

    // rule that rejected a candidate with an explanation
    private record Failure(CandidateRule rule, String reason) { }

    // ranking order for suggestions, with each step only mattering if the step before is a tie.
    // room id at the end guarantees order is always the same
    private static final Comparator<Suggestion> RANKING = Comparator
            .comparingInt(Suggestion::eligibleCount).reversed()
            .thenComparing((Suggestion s) -> s.candidate().date())
            .thenComparing(s -> s.candidate().start())
            .thenComparingInt(Suggestion::unusedSeats)
            .thenComparing(s -> s.candidate().room().id());
}