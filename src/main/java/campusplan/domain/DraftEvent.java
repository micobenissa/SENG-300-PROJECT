package campusplan.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An event, from its first draft through scheduling and cancellation.
 *
 * <p>The class keeps its P1 name so existing code and saved files still fit. The planning fields
 * never change after creation (only a DRAFT can be replaced as a whole). Scheduling adds a room and a
 * start/end time, which are present exactly when the status is SCHEDULED.
 */
public record DraftEvent(String id, String title, List<String> inviteeIds,
                         LocalDate startDate, LocalDate endDate, int durationMinutes,
                         Set<Feature> requiredFeatures, int minimumAttendance,
                         EventStatus status, Map<String, AttendanceResponse> responses,
                         String roomId, LocalDate scheduledDate, LocalTime startTime, LocalTime endTime) {
    private static final LocalTime OPENS = LocalTime.of(8, 0);
    private static final LocalTime CLOSES = LocalTime.of(22, 0);

    /** A new draft: every invitee starts PENDING and nothing is reserved. */
    public DraftEvent(String id, String title, List<String> inviteeIds,
                      LocalDate startDate, LocalDate endDate, int durationMinutes,
                      Set<Feature> requiredFeatures, int minimumAttendance) {
        this(id, title, inviteeIds, startDate, endDate, durationMinutes, requiredFeatures,
                minimumAttendance, EventStatus.DRAFT, pendingResponses(inviteeIds), null, null, null, null);
    }

    /** An event with a status and responses but no room or time (a draft, or a cancelled event). */
    public DraftEvent(String id, String title, List<String> inviteeIds,
                      LocalDate startDate, LocalDate endDate, int durationMinutes,
                      Set<Feature> requiredFeatures, int minimumAttendance,
                      EventStatus status, Map<String, AttendanceResponse> responses) {
        this(id, title, inviteeIds, startDate, endDate, durationMinutes, requiredFeatures,
                minimumAttendance, status, responses, null, null, null, null);
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
        Rules.present(status, "Event status");
        // Availability does not count as accepting an invitation, so every invitee needs a response.
        Rules.present(responses, "Attendance responses");
        if (!responses.keySet().equals(new HashSet<>(inviteeIds))) {
            throw new IllegalArgumentException("Each invitee must have exactly one attendance response.");
        }
        Rules.items(responses.values(), "Attendance responses");
        responses = Map.copyOf(responses);

        switch (status) {
            case DRAFT -> {
                if (roomId != null || scheduledDate != null || startTime != null || endTime != null) {
                    throw new IllegalArgumentException("A draft event cannot have a room or time.");
                }
                if (responses.values().stream().anyMatch(response -> response != AttendanceResponse.PENDING)) {
                    throw new IllegalArgumentException("Draft invitees must all be PENDING.");
                }
            }
            case SCHEDULED -> {
                roomId = Rules.text(roomId, "Room ID", Integer.MAX_VALUE);
                Rules.present(scheduledDate, "Scheduled date");
                Rules.present(startTime, "Start time");
                Rules.present(endTime, "End time");
                if (scheduledDate.isBefore(startDate) || scheduledDate.isAfter(endDate)) {
                    throw new IllegalArgumentException("Scheduled date must be inside the event's date window.");
                }
                if (startTime.isBefore(OPENS) || endTime.isAfter(CLOSES)) {
                    throw new IllegalArgumentException("Scheduled time must be within 08:00-22:00.");
                }
                if (!boundary(startTime) || !boundary(endTime)
                        || ChronoUnit.MINUTES.between(startTime, endTime) != durationMinutes) {
                    throw new IllegalArgumentException(
                            "Scheduled time must use 30-minute boundaries and match the duration.");
                }
            }
            case CANCELLED -> {
                // Cancelling frees the room, but the record and its responses stay.
                if (roomId != null || scheduledDate != null || startTime != null || endTime != null) {
                    throw new IllegalArgumentException("A cancelled event cannot hold a room or time.");
                }
            }
        }
    }

    private static boolean boundary(LocalTime time) {
        return time.getMinute() % 30 == 0 && time.getSecond() == 0 && time.getNano() == 0;
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

    public boolean isScheduled() {
        return status == EventStatus.SCHEDULED;
    }

    /** The same event reserved in a room at a start time. Responses stay as they are (PENDING). */
    public DraftEvent scheduledAt(String room, LocalDate date, LocalTime start) {
        return new DraftEvent(id, title, inviteeIds, startDate, endDate, durationMinutes, requiredFeatures,
                minimumAttendance, EventStatus.SCHEDULED, responses, room, date, start,
                start == null ? null : start.plusMinutes(durationMinutes));
    }

    /** The same event with its room and time released and its responses kept. */
    public DraftEvent cancelled() {
        return new DraftEvent(id, title, inviteeIds, startDate, endDate, durationMinutes, requiredFeatures,
                minimumAttendance, EventStatus.CANCELLED, responses, null, null, null, null);
    }

    public DraftEvent withResponse(String studentId, AttendanceResponse response) {
        Rules.present(response, "Attendance response");
        if (!responses.containsKey(studentId)) {
            throw new IllegalArgumentException("Student is not invited to this event.");
        }
        Map<String, AttendanceResponse> updated = new LinkedHashMap<>();
        for (String invitee : inviteeIds) {
            updated.put(invitee, invitee.equals(studentId) ? response : responses.get(invitee));
        }
        return new DraftEvent(id, title, inviteeIds, startDate, endDate, durationMinutes, requiredFeatures,
                minimumAttendance, status, updated, roomId, scheduledDate, startTime, endTime);
    }

    /** True when this event is scheduled and its time overlaps the given half-open interval. */
    public boolean overlaps(LocalDate date, LocalTime start, LocalTime end) {
        return isScheduled() && scheduledDate.equals(date) && startTime.isBefore(end) && start.isBefore(endTime);
    }
}