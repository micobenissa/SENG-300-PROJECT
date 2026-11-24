package campusplan.application;

import campusplan.domain.DraftEvent;
import campusplan.domain.Student;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Formats one event's roster as CSV text with the header student_id,name,email,response. */
final class RosterCsv {
    static final String HEADER = "student_id,name,email,response";

    private RosterCsv() { }

    static String format(DraftEvent event, List<Student> students) {
        Map<String, Student> byId = students.stream().collect(Collectors.toMap(Student::id, Function.identity()));
        StringBuilder csv = new StringBuilder(HEADER).append("\r\n");
        for (String id : event.inviteeIds()) {
            Student student = byId.get(id);
            csv.append(field(student.id())).append(',').append(field(student.name())).append(',')
                    .append(field(student.email())).append(',').append(event.responses().get(id).name())
                    .append("\r\n");
        }
        return csv.toString();
    }

    /** Quotes a field that contains a comma, a quote or a line break, and doubles any quotes in it. */
    static String field(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
