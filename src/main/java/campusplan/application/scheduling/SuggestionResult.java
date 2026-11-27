package campusplan.application.scheduling;

import java.util.List;
import java.util.Map;

// top suggestions for an event along with rooms that were rejected
public record SuggestionResult(List<Suggestion> suggestions, int candidatesConsidered, int roomRejections, int attendanceRejections, Map<String, Integer> rejectionsByRule) {
    public boolean isEmpty() { // if no suggestions are found, returns as output
        return suggestions.isEmpty();
    }

    // explains why no room suggestions were found. displays how many candidates checked and goes into why they were rejected (room vs attendance issues)
    public String noResultReport() { // room failures counted before attendance failures to prevent double counting
        return "No suitable time and room found. " + candidatesConsidered + " candidates were checked: " + roomRejections + " rejected for room constraints and " + attendanceRejections + " for insufficient eligible attendance.";
    }
}