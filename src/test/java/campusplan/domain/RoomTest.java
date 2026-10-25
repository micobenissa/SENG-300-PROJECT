package campusplan.domain;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


class RoomTest {
    @Test
    void roomValidatesLimitsAndCopiesFeatures() { // checking that room enforces its validation limits and defensively copies its features
        Set<Feature> features = new HashSet<>(Set.of(Feature.WHITEBOARD)); // mutable input set to prove that Room doesn't share this set

        // valid room at lower capacity boundary (1). surrounding whitespace around the name that should be trimmed
        Room room = new Room("r1", " Study Room ", 1, features);

        // modifying original set afterwards shouldnt affect the Room
        features.clear();

        // name was trimmed/normalized
        assertEquals("Study Room", room.name());

        // features still contain whiteboard despite clearing the features.
        assertEquals(Set.of(Feature.WHITEBOARD), room.features());

        // Room's own feature set is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> room.features().clear());

        // checking boundary limits (character limit, room capacity)
        assertEquals(100, new Room("r2", "x".repeat(60), 100, Set.of()).capacity());
        assertThrows(IllegalArgumentException.class, () -> new Room("r1", "Room", 0, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Room("r1", "Room", 101, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Room("r1", "x".repeat(61), 1, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Room("r1", "Room", 1, null)); // null feature set rejected

        // feature set with null element is rejected
        Set<Feature> invalid = new HashSet<>();
        invalid.add(null);
        assertThrows(IllegalArgumentException.class, () -> new Room("r1", "Room", 1, invalid));
    }
}