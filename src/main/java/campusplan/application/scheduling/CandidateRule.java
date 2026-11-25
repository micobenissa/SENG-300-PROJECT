package campusplan.application.scheduling;

import java.util.Optional;

/** one scheduling rule (strategy interface)
 * suggestion engine holds an ordered list of rules and stops at the first one that rejects a candidate
 * each rule only answers one question and gives its own reason, which is what lets the app
 * explain a rejection and count rejections without overlap
 */
public interface CandidateRule {
    // keeping track of whether a failure is a room problem or an attendance problem (reported as separate counts)
    enum Kind { ROOM, ATTENDANCE }

    String name(); // helps with identifying this rule

    Kind kind();

    // empty when the candidate passes, otherwise the reason it is rejected.
    Optional<String> check(Candidate candidate, PlanningContext context);
}