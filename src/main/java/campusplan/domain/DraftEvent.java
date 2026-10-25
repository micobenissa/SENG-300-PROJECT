package campusplan.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record DraftEvent(String id, String title, List<String> inviteeIds,
                         LocalDate startDate, LocalDate endDate, int durationMinutes,
                         Set<Feature> requiredFeatures, int minimumAttendance,
                         EventStatus status, Map<String, AttendanceResponse> responses) {
    public DraftEvent(String id, String title, List<String> inviteeIds,
                      LocalDate startDate, LocalDate endDate, int durationMinutes,
                      Set<Feature> requiredFeatures, int minimumAttendance) {
        this(id, title, inviteeIds, startDate, endDate, durationMinutes, requiredFeatures,
                minimumAttendance, EventStatus.DRAFT, pendingResponses(inviteeIds));
    }

    public DraftEvent {
        id = Rules.text(id, "Event ID", Integer.MAX_VALUE);
        title = Rules.text(title, "Event title", 100);
        Rules.items(inviteeIds, "Invitees");
        inviteeIds = List.copyOf(inviteeIds);
        if (inviteeIds.size() < 2 || inviteeIds.size() > 20
                || new HashSet<>(inviteeIds).size() != inviteeIds.size()
                || inviteeIds.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("An event needs 2-20 distinct student IDs.");
        }
        Rules.present(startDate, "Window start date");
        Rules.present(endDate, "Window end date");
        // Both dates count, so a seven-day window has six days between its endpoints.
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days < 0 || days > 6) {
            throw new IllegalArgumentException("Date window must cover 1-7 consecutive days.");
        }
        if (durationMinutes < 30 || durationMinutes > 180 || durationMinutes % 30 != 0) {
            throw new IllegalArgumentException("Duration must be 30-180 minutes in 30-minute steps.");
        }
        Rules.items(requiredFeatures, "Required features");
        requiredFeatures = Set.copyOf(requiredFeatures);
        if (minimumAttendance < 2 || minimumAttendance > inviteeIds.size()) {
            throw new IllegalArgumentException("Minimum attendance must be 2 through the invitee count.");
        }
        if (status != EventStatus.DRAFT) {
            throw new IllegalArgumentException("P1 events must have DRAFT status.");
        }
        // Availability does not count as accepting an invitation.
        Rules.present(responses, "Attendance responses");
        if (!responses.keySet().equals(new HashSet<>(inviteeIds))
                || responses.values().stream().anyMatch(response -> response != AttendanceResponse.PENDING)) {
            throw new IllegalArgumentException("Each invitee must have exactly one PENDING response in P1.");
        }
        responses = Map.copyOf(responses);
    }

    private static Map<String, AttendanceResponse> pendingResponses(List<String> inviteeIds) {
        Map<String, AttendanceResponse> responses = new LinkedHashMap<>();
        if (inviteeIds != null) {
            for (String id : inviteeIds) {
                responses.put(id, AttendanceResponse.PENDING);
            }
        }
        return responses;
    }
}