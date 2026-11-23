package campusplan.persistence;

import campusplan.domain.AppState;
import campusplan.domain.AttendanceResponse;
import campusplan.domain.Availability;
import campusplan.domain.DraftEvent;
import campusplan.domain.EventStatus;
import campusplan.domain.Room;
import campusplan.domain.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledStorageTest {

}