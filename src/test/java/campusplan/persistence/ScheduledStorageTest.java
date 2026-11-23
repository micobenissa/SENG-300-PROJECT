package campusplan.persistence;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.io.TempDir;

import campusplan.domain.AppState;
import campusplan.domain.AttendanceResponse;
import campusplan.domain.Availability;
import campusplan.domain.DraftEvent;
import campusplan.domain.Room;
import campusplan.domain.Student;

class ScheduledStorageTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 16);
    @TempDir Path directory;

    /**
     * This file will create sample application data to be used by other tests. It creates:
     *  - 2 students
     *  - two rooms
     *  - set availability
     *  - three events
     * 
     *  @return - app state to test with
     */
    private static AppState scheduledState() {
        List<Student> students = List.of(new Student("s1", "Zoë Ångström", "zoe@x.ca"), new Student("s2", "Sam", "sam@x.ca"));

        List<Room> rooms = List.of(new Room("R1", "Cedar", 4, Set.of()), new Room("R2", "Maple", 4, Set.of()));

        List<Availability> availability = List.of(new Availability("s1", DAY, LocalTime.of(9, 0), LocalTime.of(11, 0)));

        DraftEvent draft = new DraftEvent("e0", "Plain draft", List.of("s1", "s2"), DAY, DAY, 30, Set.of(), 2);

        DraftEvent scheduled = new DraftEvent("e1", "Study, \"group\"", List.of("s1", "s2"), DAY, DAY, 60, Set.of(), 2)
                .scheduledAt("R1", DAY, LocalTime.of(9, 0)).withResponse("s1", AttendanceResponse.ACCEPTED)
                .withResponse("s2", AttendanceResponse.DECLINED);

        DraftEvent cancelled = new DraftEvent("e2", "Gone", List.of("s1", "s2"), DAY, DAY, 60, Set.of(), 2)
                .scheduledAt("R2", DAY, LocalTime.of(9, 0)).withResponse("s2", AttendanceResponse.DECLINED).cancelled();

        return new AppState(students, rooms, availability, List.of(draft, scheduled, cancelled));
    }

}