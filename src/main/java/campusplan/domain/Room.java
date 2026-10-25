package campusplan.domain;

import java.util.Set;

/* immutable domain object representing a bookable room on campus
 * id = unique identifier for room
 * name = human readable room name
 * capacity = # of ppl the room holds
 * features = equipment/etc in the room (whiteboard, projector, etc)
 */
public record Room(String id, String name, int capacity, Set<Feature> features) {
    // validates all components before assigning fields
    public Room {
        id = Rules.text(id, "Room ID", Integer.MAX_VALUE); // validate id by normalizing (could be trimming) it
        name = Rules.text(name, "Room name", 60); // max character limit is 60
        if (capacity < 1 || capacity > 100) { // capacity from 1-100
            throw new IllegalArgumentException("Room capacity must be 1-100.");
        }
        Rules.items(features, "Room features"); // reject null set
        features = Set.copyOf(features); // creates an unmodifiable snapshot so features can't be modified by the user/callers
    }
}