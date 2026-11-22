package campusplan.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record AppState(List<Student> students, List<Room> rooms,
                       List<Availability> availability, List<DraftEvent> events) {
    public AppState {
        Rules.items(students, "Students");
        Rules.items(rooms, "Rooms");
        Rules.items(availability, "Availability");
        Rules.items(events, "Events");
        // Keep callers from changing the lists after this state has been checked.
        students = List.copyOf(students);
        rooms = List.copyOf(rooms);
        events = List.copyOf(events);

        Set<String> studentIds = new HashSet<>();
        Set<String> emails = new HashSet<>();
        for (Student student : students) {
            if (!studentIds.add(student.id()) || !emails.add(student.email())) {
                throw new IllegalArgumentException("Student IDs and emails must be unique.");
            }
        }
        Set<String> roomIds = new HashSet<>();
        Set<String> roomNames = new HashSet<>();
        for (Room room : rooms) {
            if (!roomIds.add(room.id()) || !roomNames.add(room.name().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Room IDs and names must be unique.");
            }
        }
        Set<String> eventIds = new HashSet<>();
        for (DraftEvent event : events) {
            if (!eventIds.add(event.id())) {
                throw new IllegalArgumentException("Event IDs must be unique.");
            }
            if (!studentIds.containsAll(event.inviteeIds())) {
                throw new IllegalArgumentException("Event contains an unknown student ID.");
            }
        }
        for (Availability interval : availability) {
            if (!studentIds.contains(interval.studentId())) {
                throw new IllegalArgumentException("Availability contains an unknown student ID.");
            }
        }
        availability = mergeIntervals(availability);
        checkSchedule(rooms, availability, events);
    }

    // Reservations and accepted attendance must never conflict, in any state the app can hold or load.
    private static void checkSchedule(List<Room> rooms, List<Availability> availability, List<DraftEvent> events) {
        Set<String> roomIds = new HashSet<>();
        rooms.forEach(room -> roomIds.add(room.id()));
        List<DraftEvent> scheduled = events.stream().filter(DraftEvent::isScheduled).toList();
        for (int i = 0; i < scheduled.size(); i++) {
            DraftEvent event = scheduled.get(i);
            if (!roomIds.contains(event.roomId())) {
                throw new IllegalArgumentException("Event \"" + event.title() + "\" uses an unknown room.");
            }
            for (int j = i + 1; j < scheduled.size(); j++) {
                DraftEvent other = scheduled.get(j);
                if (event.roomId().equals(other.roomId())
                        && event.overlaps(other.scheduledDate(), other.startTime(), other.endTime())) {
                    throw new IllegalArgumentException("Events \"" + event.title() + "\" and \"" + other.title()
                            + "\" reserve the same room at overlapping times.");
                }
                for (String studentId : event.inviteeIds()) {
                    if (event.responses().get(studentId) == AttendanceResponse.ACCEPTED
                            && other.responses().get(studentId) == AttendanceResponse.ACCEPTED
                            && event.overlaps(other.scheduledDate(), other.startTime(), other.endTime())) {
                        throw new IllegalArgumentException("A student accepted overlapping events \""
                                + event.title() + "\" and \"" + other.title() + "\".");
                    }
                }
            }
            for (String studentId : event.inviteeIds()) {
                if (event.responses().get(studentId) == AttendanceResponse.ACCEPTED
                        && availability.stream().noneMatch(interval -> interval.studentId().equals(studentId)
                        && interval.covers(event.scheduledDate(), event.startTime(), event.endTime()))) {
                    throw new IllegalArgumentException("A student accepted \"" + event.title()
                            + "\" without availability for the whole time.");
                }
            }
        }
    }

    public AppState withStudents(List<Student> students) {
        return new AppState(students, rooms, availability, events);
    }

    public AppState withAvailability(List<Availability> availability) {
        return new AppState(students, rooms, availability, events);
    }

    public AppState withEvents(List<DraftEvent> events) {
        return new AppState(students, rooms, availability, events);
    }

    /** Sorts intervals and joins overlapping or touching ones for each student and date. */
    public static List<Availability> mergeIntervals(List<Availability> intervals) {
        // Put each student's intervals in order before joining touching ones.
        List<Availability> sorted = intervals.stream().sorted(Comparator
                .comparing(Availability::studentId).thenComparing(Availability::date)
                .thenComparing(Availability::start).thenComparing(Availability::end)).toList();
        List<Availability> result = new ArrayList<>();
        for (Availability next : sorted) {
            if (!result.isEmpty()) {
                Availability last = result.getLast();
                if (last.studentId().equals(next.studentId()) && last.date().equals(next.date())
                        && !next.start().isAfter(last.end())) {
                    // Joined intervals also cover events crossing their shared boundary.
                    result.set(result.size() - 1, new Availability(last.studentId(), last.date(),
                            last.start(), last.end().isAfter(next.end()) ? last.end() : next.end()));
                    continue;
                }
            }
            result.add(next);
        }
        return List.copyOf(result);
    }

    public static AppState empty() {
        return new AppState(List.of(), List.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
        return students.isEmpty() && rooms.isEmpty() && availability.isEmpty() && events.isEmpty();
    }
}