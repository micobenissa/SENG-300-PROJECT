package campusplan.application.scheduling;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import campusplan.domain.AppState;
import campusplan.domain.Availability;
import campusplan.domain.DraftEvent;
import campusplan.domain.Feature;
import campusplan.domain.Room;
import campusplan.domain.Student;


public final class PlanFixture {
    public static final LocalDate DAY = LocalDate.of(2026, 11, 16);

    private PlanFixture() {
    }

    /**
     * Creates sample students with sequential IDs, names, and campus test email addresses.
     *
     * @param count - number of students to create; values less than or equal to zero produce an empty list
     * 
     * @return - the generated students
     */
    public static List<Student> students(int count) {
        List<Student> students = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            students.add(new Student("s" + i, "Student " + i, "s" + i + "@campus.test"));
        }

        return students;
    }

    /**
     * Creates a list of sequential ids.
     * 
     * @param first - what the first id number will be
     * @param last - what the last id number will be
     * 
     * @return - list of ids
     */
    public static List<String> ids(int first, int last) {
        List<String> ids = new ArrayList<>();
        for (int i = first; i <= last; i++) {
            ids.add("s" + i);
        }
        return ids;
    }

    /**
     * Creates a new room.
     * 
     * @param id - id of room
     * @param capacity - maximum number of people the room can hold
     * @param features - features of room
     *
     * @return - a new room
 */
    public static Room room(String id, int capacity, Feature... features) {
        return new Room(id, "Room " + id, capacity, features.length == 0 ? Set.of() : Set.of(features));
    }

    /**
     * Creates an availability record for a student
     *
     * @param studentId - id
     * @param date - date the student is available
     * @param start - starting time 
     * @param end - ending time 
     *
     * @return - availability record
     */
    public static Availability free(String studentId, LocalDate date, String start, String end) {
        return new Availability(studentId, date, LocalTime.parse(start), LocalTime.parse(end));
    }

    /**
     * Creates availability records for multiple students using the same date and time
     *
     * @param studentIds - list of ids
     * @param date - date the students are available
     * @param start - starting time of availability
     * @param end - ending time of availability
     *
     * @return - list of availability records
     */
    public static List<Availability> freeAll(List<String> studentIds, LocalDate date, String start, String end) {
        return studentIds.stream().map(id -> free(id, date, start, end)).toList();
    }

    /**
     * Creates a draft event with the given invitees, duration, minimum attendance, and features
     *
     * @param id - id of event
     * @param invitees - list of student ids i
     * @param minutes - duration of the event 
     * @param minimum - minimum number of students required
     * @param features - features required for the event
     *
     * @return - a new draft event 
     */
    public static DraftEvent draft(String id, List<String> invitees, int minutes, int minimum, Feature... features) {
        return new DraftEvent(id, "Event " + id, invitees, DAY, DAY, minutes,
                features.length == 0 ? Set.of() : Set.of(features), minimum);
    }

    /**
     * Creates an application state containing the given students, rooms, availability records, and events.
     *
     * @param students - list of students
     * @param rooms - list of rooms
     * @param availability - list of student availability records
     * @param events - list of draft events
     *
     * @return - a new application state 
     */
    public static AppState state(List<Student> students, List<Room> rooms, List<Availability> availability,
                                List<DraftEvent> events) {
        return new AppState(students, rooms, availability, events);
    }

}