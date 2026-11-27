package campusplan.application.scheduling;

import campusplan.domain.Room;

import java.time.LocalDate;
import java.time.LocalTime;

// one room and one start/end interval that might host an event
public record Candidate(Room room, LocalDate date, LocalTime start, LocalTime end) { }