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
    void trimsIdNameAndEmail() {
        Student student = new Student(" S1 ", "  Ava Chen ", " AVA@EXAMPLE.COM ");

        assertEquals("S1", student.id());
        assertEquals("Ava Chen", student.name());
        assertEquals("ava@example.com", student.email());
    }

    @Test
    void acceptsNameLengthBoundaries() {
        assertEquals("A", new Student("S1", "A", "a@x.ca").name());
        String longest = "N".repeat(Student.MAX_NAME_LENGTH);
        assertEquals(longest, new Student("S1", longest, "a@x.ca").name());
    }

    @Test
    void rejectsNameOverLimit() {
        String tooLong = "N".repeat(Student.MAX_NAME_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> new Student("S1", tooLong, "a@x.ca"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsMissingName(String name) {
        assertThrows(IllegalArgumentException.class, () -> new Student("S1", name, "a@x.ca"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsMissingId(String id) {
        assertThrows(IllegalArgumentException.class, () -> new Student(id, "Ava", "a@x.ca"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"S1", "550e8400-e29b-41d4-a716-446655440000"})
    void seedAndGeneratedStringIdsSurviveEdits(String id) {
        Student student = new Student(id, "Ava", "a@x.ca");

        assertEquals(id, student.id());
        assertEquals(id, student.withName("Ava Chen").id());
        assertEquals(id, student.withEmail("ava.chen@x.ca").id());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "ava.example.com", "@example.com", "ava@", "a@b@c.ca", "ava chen@x.ca", "ava@x .ca", "ava@\tx.ca", "ava@x\u00a0.ca", "ava@x\u0085.ca"})
    void rejectsInvalidEmail(String email) {
        assertThrows(IllegalArgumentException.class, () -> new Student("S1", "Ava", email));
    }

    @Test
    void acceptsShortestValidEmail() {
        assertEquals("a@b", new Student("S1", "Ava", "a@b").email());
    }

    @Test
    void emailKeyIgnoresCaseAndSurroundingSpaces() {
        Student student = new Student("S1", "Ava", "Ava.Chen@Example.COM");

        assertEquals("ava.chen@example.com", student.email());
        assertEquals("ava.chen@example.com", student.emailKey());
        assertEquals(student.emailKey(), Student.emailKey("  ava.chen@example.com "));
    }

    @Test
    void editsKeepIdAndValidate() {
        Student student = new Student("S7", "Ava", "ava@x.ca");

        Student renamed = student.withName(" Ava Chen ");
        Student moved = student.withEmail(" AVA.CHEN@X.CA ");

        assertEquals("S7", renamed.id());
        assertEquals("Ava Chen", renamed.name());
        assertEquals("S7", moved.id());
        assertEquals("ava.chen@x.ca", moved.email());
        assertThrows(IllegalArgumentException.class, () -> student.withName(" "));
        assertThrows(IllegalArgumentException.class, () -> student.withEmail("no-at-sign"));
        assertEquals("Ava", student.name());
    }

    @Test
    void matchesPartialNameOrEmailIgnoringCase() {
        Student student = new Student("S1", "Ava Chen", "ava.chen@campus.ca");

        assertTrue(student.matches("chen"));
        assertTrue(student.matches(" AVA "));
        assertTrue(student.matches("@CAMPUS"));
        assertFalse(student.matches("noah"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void rejectsBlankSearch(String query) {
        Student student = new Student("S1", "Ava", "ava@x.ca");
        assertThrows(IllegalArgumentException.class, () -> student.matches(query));
    }
}
