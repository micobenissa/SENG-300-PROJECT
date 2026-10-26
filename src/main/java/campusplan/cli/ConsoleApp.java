package campusplan.cli;

import campusplan.application.CampusPlanService;
import campusplan.domain.Availability;
import campusplan.domain.DraftEvent;
import campusplan.domain.Feature;
import campusplan.domain.Student;
import campusplan.persistence.Storage;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ConsoleApp {
    private static final int BOX_WIDTH = 64;
    // the service owns the records; the menu just collects input and shows results
    private final CampusPlanService service;
    // sample data stays separate from the user's saved data
    private final Storage seed;
    private final BufferedReader input;
    private final PrintWriter out;
    // this stays true until the current changes have actually been saved
    private boolean dirty;

    public ConsoleApp(CampusPlanService service, Storage seed, Reader input, PrintWriter out) {
        this.service = service;
        this.seed = seed;
        // every menu reads from the same input, including the tests
        this.input = new BufferedReader(input);
        this.out = out;
    }

    public void run() throws IOException {
        boolean running = true;
        // each action returns here, so a bad entry does not end the app
        while (running) {
            try {
                printMainMenu();
                switch (read("Choice")) {
                    case "1" -> students();
                    case "2" -> rooms();
                    case "3" -> availability();
                    case "4" -> drafts();
                    case "5" -> loadSampleData();
                    case "6" -> save();
                    // exit can return true when the user cancels the exit prompt
                    case "0" -> running = exit();
                    default -> out.println("Choose a listed option.");
                }
            } catch (EOFException end) {
                out.println("\nInput ended.");
                // input can end halfway through a form, so only save completed changes
                if (dirty) {
                    out.println("Saving pending changes before closing.");
                    try {
                        save();
                    } catch (IOException error) {
                        // there is no more input to retry with, so report the failed save
                        out.println("Pending changes could not be saved: " + error.getMessage());
                        out.flush();
                        throw error;
                    }
                }
                running = false;
            } catch (IllegalArgumentException error) {
                // show validation errors and let the next menu choice try again
                out.println("\n  Error: " + error.getMessage());
            } catch (IOException error) {
                // keep the app open so saving can be tried again
                out.println("\n  Storage error: " + error.getMessage());
                out.println("  Changes are still in memory. Try Save again or make a backup before restarting.");
            }
        }
        // flush the last message before returning control to the terminal
        out.println("CampusPlan closed.");
        out.flush();
    }

    private void printMainMenu() {
        heading("CampusPlan | P1");
        var state = service.state();
        boxRow(String.format("%d students  /  %d rooms  /  %d drafts", state.students().size(),
                state.rooms().size(), state.events().size()));
        boxRow("Status: " + (dirty ? "Unsaved changes" : "No pending changes"));
        if (state.isEmpty()) {
            boxRow("No saved records. Select [5] to load sample data.");
        }
        menuSection("WORKSPACE");
        option("1", "Students", "Create, edit and search");
        option("2", "Rooms", "Browse and filter");
        option("3", "Availability", "View and update times");
        option("4", "Draft events", "Create and edit plans");
        menuSection("DATA");
        option("5", "Load sample data");
        option("6", "Save changes");
        border();
        option("0", "Exit");
        border();
    }

    private void loadSampleData() throws IOException {
        var sample = seed.load();
        // a missing sample file loads as empty, which is not useful to import
        if (sample.isEmpty()) {
            throw new IllegalArgumentException("Sample data is empty or the sample file is missing.");
        }
        // the service stops samples from replacing records that are already there
        service.loadSeed(sample);
        // loading samples changes the session too, so they still need saving
        changed("Sample data loaded.");
    }

    private void students() throws IOException {
        heading("Students");
        option("1", "List students");
        option("2", "Create student");
        option("3", "Edit student");
        option("4", "Search students");
        border();
        option("0", "Back");
        border();
        out.println();
        switch (read("Choice")) {
            case "1" -> printStudents(service.state().students());
            case "2" -> {
                String name = read("Name");
                // let the service validate the email and create the student id
                Student student = service.createStudent(name, read("Email"));
                changed("Student created: " + student.id());
            }
            case "3" -> {
                // choose the record before asking for its replacement fields
                Student student = selectStudent();
                String name = read("Name [" + student.name() + "]");
                String email = read("Email [" + student.email() + "]");
                // blank fields keep the old values, and the student keeps the same id
                service.editStudent(student.id(), name.isEmpty() ? student.name() : name,
                        email.isEmpty() ? student.email() : email);
                changed("Student updated: " + student.id());
            }
            // search uses the current records, including edits that are not saved yet
            case "4" -> printStudents(service.searchStudents(read("Partial name or email")));
            case "0" -> { }
            default -> out.println("Choose a listed option.");
        }
    }

    private void rooms() throws IOException {
        heading("Rooms | open 08:00-22:00");
        option("1", "List rooms");
        option("2", "Filter rooms");
        border();
        option("0", "Back");
        border();
        out.println();
        String choice = read("Choice");
        // backing out should not ask for any filter values
        if (choice.equals("0")) {
            return;
        }
        if (!choice.equals("1") && !choice.equals("2")) {
            out.println("Choose a listed option.");
            return;
        }
        // listing uses the widest filter; only option 2 asks for restrictions
        int capacity = choice.equals("2") ? integer("Minimum capacity") : 1;
        Set<Feature> required = choice.equals("2") ? features() : Set.of();
        // the service checks capacity and makes sure every requested feature matches
        var matches = service.filterRooms(capacity, required);
        section("ROOM CATALOGUE");
        if (matches.isEmpty()) {
            out.println("No rooms match these requirements.");
        }
        // keep room ids visible even though this menu does not edit rooms
        for (var room : matches) {
            out.printf("    %s | %d seats | %s%n", room.name(), room.capacity(),
                    featureNames(room.features()));
            out.println("      ID: " + room.id());
            out.println();
        }
    }

    private void availability() throws IOException {
        heading("Availability");
        option("1", "View availability");
        option("2", "Replace one date");
        border();
        option("0", "Back");
        border();
        out.println();
        String choice = read("Choice");
        if (choice.equals("0")) {
            return;
        }
        if (!choice.equals("1") && !choice.equals("2")) {
            out.println("Choose a listed option.");
            return;
        }
        // both actions work on one selected student
        Student student = selectStudent();
        if (choice.equals("1")) {
            printAvailability(student);
        } else {
            replaceAvailability(student);
        }
    }

    private void printAvailability(Student student) {
        // show only this student's intervals, across all their recorded dates
        var intervals = service.state().availability().stream()
                .filter(interval -> interval.studentId().equals(student.id())).toList();
        section("AVAILABILITY / " + student.name());
        out.println("  Student ID: " + student.id());
        out.println();
        if (intervals.isEmpty()) {
            out.println("No availability recorded.");
        }
        // show the dates as well, since one student can have several days recorded
        if (!intervals.isEmpty()) {
            out.printf("    %-12s %s%n", "DATE", "AVAILABLE");
            out.printf("    %-12s %s%n", "----------", "-----------");
            intervals.forEach(interval -> out.printf("    %-12s %s-%s%n", interval.date(), interval.start(), interval.end()));
        }
    }

    private void replaceAvailability(Student student) throws IOException {
        // replacing one date leaves the student's other dates alone
        LocalDate date = date("Date (YYYY-MM-DD, today or later)");
        int count = integer("Number of intervals (0 clears this date)");
        // there are at most 28 half-hour slots between 08:00 and 22:00
        if (count < 0 || count > 28) {
            throw new IllegalArgumentException("Enter 0-28 intervals for one date.");
        }
        // finish reading the form first so bad input cannot leave half an update
        List<Availability> intervals = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            LocalTime start = time("Interval " + (i + 1) + " start (HH:MM)");
            LocalTime end = time("Interval " + (i + 1) + " end (HH:MM)");
            // the interval checks its times before it can reach the service
            intervals.add(new Availability(student.id(), date, start, end));
        }
        // an empty list clears the date; overlapping or touching intervals get merged
        var merged = service.setAvailability(student.id(), date, intervals);
        changed("Availability updated for " + student.name() + " on " + date + ".");
        // print the merged result so the user can see what was kept
        merged.forEach(interval -> out.printf("%s-%s%n", interval.start(), interval.end()));
    }

    private void drafts() throws IOException {
        heading("Draft events");
        option("1", "List drafts");
        option("2", "Create draft");
        option("3", "Edit draft");
        option("4", "View details");
        border();
        option("0", "Back");
        border();
        out.println();
        switch (read("Choice")) {
            case "1" -> printEvents();
            // the same form handles a new draft or an existing draft
            case "2" -> editDraft(null);
            case "3" -> editDraft(selectEvent());
            case "4" -> printEvent(selectEvent());
            case "0" -> { }
            default -> out.println("Choose a listed option.");
        }
    }

    private void editDraft(DraftEvent current) throws IOException {
        // show the old fields first when this is an edit
        if (current != null) {
            printEvent(current);
            out.println("Enter replacement planning fields below. The event ID stays the same.");
        }
        String title = read("Title");
        // the form uses menu numbers, then turns those into student ids
        List<String> invitees = readInviteeIds();
        LocalDate start = date("First date (YYYY-MM-DD, today or later)");
        LocalDate end = date("Last date (YYYY-MM-DD)");
        // keep all the planning fields local until the whole form has been read
        int duration = integer("Duration in minutes (30-180, steps of 30)");
        Set<Feature> required = features();
        int minimum = integer("Minimum attendance");
        // only a new draft gets a new id; editing passes the existing id back
        DraftEvent event = current == null
                ? service.createDraft(title, invitees, start, end, duration, required, minimum)
                : service.editDraft(current.id(), title, invitees, start, end, duration, required, minimum);
        // mark it changed only after the service accepts the completed form
        changed("Draft " + (current == null ? "created" : "updated") + ": " + event.id());
        printEvent(event);
    }

    private List<String> readInviteeIds() throws IOException {
        printStudents(service.state().students());
        out.println("  Choose 2-20 different students from the list above.");
        out.println("  Use the number before each name, not their ID, name or email.");
        out.println("  Example: 1,2 selects the first and second students.");
        String selected = read("Student list numbers, separated by commas");
        List<String> invitees = new ArrayList<>();
        // keep empty entries so a trailing comma is caught as bad input too
        for (String number : selected.split(",", -1)) {
            int index = parseInteger(number.trim(), "Student list number");
            // store the actual id, not the position shown in the menu
            invitees.add(at(service.state().students(), index, "student").id());
        }
        return invitees;
    }

    private Student selectStudent() throws IOException {
        // show the list again so the selection uses the current ordering
        printStudents(service.state().students());
        return at(service.state().students(), integer("Student number"), "student");
    }

    private DraftEvent selectEvent() throws IOException {
        // select from the current list, including newly created drafts
        printEvents();
        return at(service.state().events(), integer("Event number"), "event");
    }

    private <T> T at(List<T> items, int number, String kind) {
        // reject the menu number before using it as a list position
        if (number < 1 || number > items.size()) {
            throw new IllegalArgumentException("Choose a listed " + kind + " number.");
        }
        // menu numbers start at 1, but list positions start at 0
        return items.get(number - 1);
    }

    private void printStudents(List<Student> students) {
        section("STUDENT RECORDS");
        // an empty search or list should still give the user a clear result
        if (students.isEmpty()) {
            out.println("No students found.");
        }
        for (int i = 0; i < students.size(); i++) {
            var student = students.get(i);
            // the number is for choosing a row; the id belongs to the stored record
            out.printf("    %d. %s | %s%n", i + 1, student.name(), student.email());
            out.println("       ID: " + student.id());
            out.println();
        }
    }

    private void printEvents() {
        var events = service.state().events();
        section("DRAFT RECORDS");
        if (events.isEmpty()) {
            out.println("No draft events.");
        }
        // p1 events are drafts, so this list does not try to schedule or confirm them
        for (int i = 0; i < events.size(); i++) {
            var event = events.get(i);
            out.printf("    %d. %s | DRAFT%n", i + 1, event.title());
            out.printf("       %s to %s | %d minutes%n", event.startDate(), event.endDate(), event.durationMinutes());
            out.println("       ID: " + event.id());
            out.println();
        }
    }

    private void printEvent(DraftEvent event) {
        section("DRAFT DETAILS");
        // show the id here too so an edited draft can be recognised later
        out.println("  Title: " + event.title());
        out.println("  ID: " + event.id());
        out.println("  Status: DRAFT");
        out.println();
        // show the planning window and constraints before the invitee responses
        out.printf("  Window: %s to %s | Duration: %d minutes%n", event.startDate(), event.endDate(), event.durationMinutes());
        out.printf("  Minimum attendance: %d of %d%n", event.minimumAttendance(), event.inviteeIds().size());
        out.println("  Required features: " + featureNames(event.requiredFeatures()));
        section("INVITEES");
        // responses stay pending in p1; saving a draft does not confirm attendance
        for (String id : event.inviteeIds()) {
            out.println("    " + service.student(id).name() + " | PENDING");
        }
    }

    private Set<Feature> features() throws IOException {
        String value = read("Features (WHITEBOARD, PROJECTOR, ACCESSIBLE; blank for none)");
        Set<Feature> result = EnumSet.noneOf(Feature.class);
        // leaving this blank means there are no required room features
        if (!value.isEmpty()) {
            for (String item : value.split(",", -1)) {
                try {
                    // accept different letter cases and spaces around each feature
                    result.add(Feature.valueOf(item.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException error) {
                    // show which entry failed instead of exposing the enum error
                    throw new IllegalArgumentException("Unknown feature: " + item.trim());
                }
            }
        }
        return result;
    }

    private String featureNames(Set<Feature> features) {
        // keep the display order stable, even if the set was built in another order
        return features.isEmpty() ? "none" : features.stream().sorted().map(Enum::name)
                .reduce((first, next) -> first + ", " + next).orElse("none");
    }

    private LocalDate date(String prompt) throws IOException {
        try {
            return LocalDate.parse(read(prompt));
        } catch (DateTimeParseException error) {
            // turn the parser's message into the format the menu expects
            throw new IllegalArgumentException("Use a valid date in YYYY-MM-DD format.");
        }
    }

    private LocalTime time(String prompt) throws IOException {
        try {
            return LocalTime.parse(read(prompt));
        } catch (DateTimeParseException error) {
            // actual time boundaries are checked when the interval is created
            throw new IllegalArgumentException("Use a valid time in HH:MM format.");
        }
    }

    private int integer(String prompt) throws IOException {
        // reuse the same number check for menu selections and planning fields
        return parseInteger(read(prompt), prompt);
    }

    private int parseInteger(String value, String label) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException error) {
            // include the field name so the user knows which number was wrong
            throw new IllegalArgumentException(label + " must be a whole number.");
        }
    }

    private String read(String prompt) throws IOException {
        out.print("  " + prompt + " > ");
        // print the prompt before waiting for input, even without a newline
        out.flush();
        String line = input.readLine();
        // eof is different from a blank answer and is handled by the main loop
        if (line == null) {
            throw new EOFException();
        }
        // ignore accidental spaces around an answer
        return line.trim();
    }

    private void heading(String title) {
        // use one layout for the main menu and each submenu
        out.println();
        border();
        int padding = Math.max(0, (BOX_WIDTH - title.length()) / 2);
        boxRow(" ".repeat(padding) + title);
        border();
    }

    private void section(String title) {
        heading(title);
        out.println();
    }

    private void menuSection(String title) {
        border();
        boxRow(title);
    }

    private void border() {
        out.println("  +" + "-".repeat(BOX_WIDTH + 2) + "+");
    }

    private void boxRow(String text) {
        // Wrap long record headings instead of letting them break the right border.
        while (text.length() > BOX_WIDTH) {
            int split = text.lastIndexOf(' ', BOX_WIDTH);
            if (split <= 0) {
                split = BOX_WIDTH;
            }
            out.printf("  | %-" + BOX_WIDTH + "s |%n", text.substring(0, split));
            text = text.substring(split).stripLeading();
        }
        out.printf("  | %-" + BOX_WIDTH + "s |%n", text);
    }

    private void option(String key, String label) {
        boxRow("  [" + key + "] " + label);
    }

    private void option(String key, String label, String description) {
        boxRow(String.format("  [%s] %-18s %s", key, label, description));
    }

    private void changed(String message) {
        // successful edits need a save, but viewing records does not
        dirty = true;
        out.println("\n  " + message);
    }

    private void save() throws IOException {
        service.save();
        // only clear this after saving works, so a failed save keeps the reminder
        dirty = false;
        out.println("\n  Saved.");
    }

    private boolean exit() throws IOException {
        // there is nothing to ask about when the session has no unsaved changes
        if (!dirty) {
            return false;
        }
        // true keeps the main loop running; false actually exits
        return switch (read("Save changes before exit? (yes/no/cancel)").toLowerCase(Locale.ROOT)) {
            case "yes", "y" -> {
                // if this fails, the main loop handles the error and stays open
                save();
                yield false;
            }
            case "no", "n" -> {
                // discard the session changes only when the user chooses to
                out.println("Closing without saving changes.");
                yield false;
            }
            // cancel or an unrecognised answer returns to the main menu
            default -> true;
        };
    }
}
