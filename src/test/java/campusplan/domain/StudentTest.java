package campusplan.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class StudentTest {
    @Test
    void trimsNameAndEmail() {
        Student student = new Student(1, "  Ava Chen ", " ava@example.com ");

        assertEquals(1, student.id());
        assertEquals("Ava Chen", student.name());
        assertEquals("ava@example.com", student.email());
    }

    @Test
    void acceptsNameLengthBoundaries() {
        assertEquals("A", new Student(1, "A", "a@x.ca").name());
        String longest = "N".repeat(Student.MAX_NAME_LENGTH);
        assertEquals(longest, new Student(1, longest, "a@x.ca").name());
    }

    @Test
    void rejectsNameOverLimit() {
        String tooLong = "N".repeat(Student.MAX_NAME_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> new Student(1, tooLong, "a@x.ca"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsMissingName(String name) {
        assertThrows(IllegalArgumentException.class, () -> new Student(1, name, "a@x.ca"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveId(int id) {
        assertThrows(IllegalArgumentException.class, () -> new Student(id, "Ava", "a@x.ca"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "ava.example.com", "@example.com", "ava@", "a@b@c.ca", "ava chen@x.ca", "ava@x .ca", "ava@\tx.ca"})
    void rejectsInvalidEmail(String email) {
        assertThrows(IllegalArgumentException.class, () -> new Student(1, "Ava", email));
    }

    @Test
    void acceptsShortestValidEmail() {
        assertEquals("a@b", new Student(1, "Ava", "a@b").email());
    }

    @Test
    void emailKeyIgnoresCaseAndSurroundingSpaces() {
        Student student = new Student(1, "Ava", "Ava.Chen@Example.COM");

        assertEquals("ava.chen@example.com", student.emailKey());
        assertEquals(student.emailKey(), Student.emailKey("  ava.chen@example.com "));
    }

    @Test
    void editsKeepIdAndValidate() {
        Student student = new Student(7, "Ava", "ava@x.ca");

        Student renamed = student.withName(" Ava Chen ");
        Student moved = student.withEmail("ava.chen@x.ca");

        assertEquals(7, renamed.id());
        assertEquals("Ava Chen", renamed.name());
        assertEquals(7, moved.id());
        assertEquals("ava.chen@x.ca", moved.email());
        assertThrows(IllegalArgumentException.class, () -> student.withName(" "));
        assertThrows(IllegalArgumentException.class, () -> student.withEmail("no-at-sign"));
        assertEquals("Ava", student.name());
    }

    @Test
    void matchesPartialNameOrEmailIgnoringCase() {
        Student student = new Student(1, "Ava Chen", "ava.chen@campus.ca");

        assertTrue(student.matches("chen"));
        assertTrue(student.matches(" AVA "));
        assertTrue(student.matches("@CAMPUS"));
        assertFalse(student.matches("noah"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void rejectsBlankSearch(String query) {
        Student student = new Student(1, "Ava", "ava@x.ca");
        assertThrows(IllegalArgumentException.class, () -> student.matches(query));
    }
}
