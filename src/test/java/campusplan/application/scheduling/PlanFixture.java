package campusplan.application.scheduling;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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


}