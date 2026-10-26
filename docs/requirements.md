# CampusPlan requirements

We built CampusPlan for one organizer to manage fictional students and plan events on one computer. P1 includes student records, room filtering, availability, draft events and JSON storage. Real invitations, university bookings and accounts are outside our scope.

We based these requirements on sections 2-7 of the project brief and the [client's answers](#client-questions-and-decisions).

## Release scope

| ID | Release | Required result |
| --- | --- | --- |
| FR-01 | P1 | Create, view, edit and search students; display/filter the room catalogue; validate fields and unique emails. |
| FR-02 | P1 | Enter/update availability and create/view/edit drafts; reject invalid intervals, references and limits. |
| FR-03 | P1 | Save/reload students, rooms, availability and drafts with stable IDs; handle missing/corrupt storage. |
| FR-04 | P2 | Generate and explain the top five valid scheduling suggestions, including ranking and no-result counts. |
| FR-05 | P2 | Revalidate confirmation, reserve a room, reject conflicts atomically and cancel with confirmation. |
| FR-06 | P2 | Record PENDING/ACCEPTED/DECLINED; enforce full availability and accepted-participant conflicts. |
| FR-07 | P2 | List by date/status, show event details and export a correctly quoted UTF-8 roster CSV. |
| FR-08 | P2 | Undo/redo at least 20 successful mutations as complete operations; failed actions and exports do not enter history. |
| FR-09 | P3 | Record/remove closures, identify affected scheduled events and migrate valid P2 files. |
| FR-10 | P3 | Preview/revalidate/confirm replacements, preserve history and undo/redo the complete change. |
| FR-11 | P3 | Deliver the integrated app, clear errors, user guide, repeatable release and known limitations. |

## Domain rules

| Area | Rules |
| --- | --- |
| Students | Generated immutable ID. Trimmed name: 1-100 characters. Email: exactly one `@`, nonempty text on both sides, no whitespace; compare after trim and lowercase. Partial name/email search. No physical deletion. |
| Rooms | Immutable ID; unique name ignoring case; trimmed name 1-60 characters; capacity 1-100; any subset of WHITEBOARD, PROJECTOR and ACCESSIBLE. Catalogue comes from seed JSON. |
| Availability | One student and date, start before end, within 08:00-22:00, on exact 30-minute boundaries. Merge overlapping/adjacent intervals for the same student/date. Missing availability means unavailable. |
| Time | Use local dates/times without conversion. Intervals are half-open, so an end at 14:00 does not overlap a start at 14:00. Full-event coverage is needed for availability. |
| Drafts | Immutable ID; title 1-100 characters; 2-20 distinct existing invitees; inclusive 1-7 day window; duration 30-180 minutes in steps of 30; required room features; minimum attendance 2 through invitee count. |
| Responses | Each new invitee starts PENDING. Availability is not acceptance. P1 does not record ACCEPTED or DECLINED. |
| Storage | All domain collections as UTF-8 JSON through a storage interface. Missing file starts empty; corrupt data is reported and kept. A failed save must preserve the previous valid file. |
| Samples | At least 12 students, 6 varied rooms, 7 days of varied availability and 3 events, plus valid-result and no-result scenarios. |

We reject past dates when entering or editing availability and drafts. Previously saved records still load after their dates pass. Our reason is recorded in the [client decisions](#client-questions-and-decisions).

## Use cases

### UC-01 Manage students

**Actor:** organizer. **Before:** the app is running with a valid state.

1. Open Students and choose Create, Edit, List or Search.
2. For Create, enter a name and email. For Edit, choose a listed student and enter replacements; blank fields keep previous values.
3. The service validates the record and email uniqueness. A new student gets a generated ID; an edit keeps the old ID.
4. List/Search shows matching records. A successful mutation marks the session unsaved.

**Alternatives:** a duplicate or invalid email/name is rejected; an unknown student selection is rejected; a search with no matches reports that result. An invalid mutation keeps the previous state. **After:** a valid replacement is in memory and can be saved.

### UC-02 Browse and filter rooms

**Actor:** organizer. **Before:** the catalogue is loaded from valid saved or seed data.

1. Open Rooms and choose List or Filter.
2. For Filter, enter a minimum capacity and zero or more required features.
3. Show rooms meeting the capacity and every requested feature, with their IDs.

**Alternatives:** unknown features and capacity outside 1-100 are rejected. No matches produces a clear message. A missing catalogue can be loaded through the empty-state seed workflow. **After:** no domain data changes.

### UC-03 Replace availability for a date

**Actor:** organizer. **Before:** the chosen student exists.

1. Open Availability, choose Replace one date, and select the student.
2. Enter a current/future date and 0-28 intervals. Zero clears that date.
3. Enter each start/end using 30-minute boundaries within opening hours.
4. Validate the complete replacement, then merge overlapping/adjacent intervals and show the result.

**Alternatives:** an invalid date/time, nonpositive interval, bad reference or past date is rejected. No part of an incomplete form is applied. Other students and dates are preserved. **After:** only the selected student's selected date is replaced; the session is unsaved.

### UC-04 Create a draft

**Actor:** organizer. **Before:** at least two student records exist.

1. Open Draft events and choose Create.
2. Enter a title and 2-20 distinct student list numbers.
3. Enter the earliest/latest possible dates, duration, required room features and minimum attendance.
4. Validate all limits and references, then add a new DRAFT with a generated ID and PENDING responses.

**Alternatives:** duplicate/missing invitees, an invalid window, duration or minimum are rejected. The same first/last date is allowed. Missing availability does not prevent an invitation. **After:** the draft exists in memory; no room or student time is reserved.

### UC-05 View and edit a draft

**Actor:** organizer. **Before:** the selected draft exists.

1. Open Draft events and choose Details or Edit.
2. Details shows the ID, window, duration, required features, minimum and invitees/responses.
3. Edit shows the existing plan, collects replacement planning fields and validates them as one operation.
4. Replace the draft under the same ID. Minimum attendance may change if it remains valid.

**Alternatives:** an unknown draft or invalid replacement is rejected without changing the old draft. An expired draft must be moved to a current/future window when edited. **After:** viewing changes nothing; a valid edit marks the session unsaved.

### UC-06 Save, restart and recover storage

**Actor:** organizer. **Before:** the app has a selected data-file path.

1. On startup, load and validate the JSON state, or start empty if the file is missing.
2. If empty, optionally load sample data through the menu.
3. Save completed edits. Write a temporary file beside the destination and replace the destination atomically.
4. Restart with the same path and see the same IDs and records.

**Alternatives:** corrupt storage stops startup and remains unchanged. Seed loading into nonempty state is rejected. A failed save leaves the previous file and in-memory edits available. On exit, the operator can save, discard or cancel. End of input saves completed changes, not an unfinished record. **After:** a successful save is durable; failed operations do not silently overwrite data.

## Acceptance criteria

| ID | Given / when | Expected result |
| --- | --- | --- |
| AC-01 | A student exists; create/edit another with the same normalized email. | Reject the duplicate and preserve existing records. |
| AC-02 | Rooms have varied capacities/features; request capacity 6, WHITEBOARD and ACCESSIBLE. | Show only rooms meeting all three conditions. |
| AC-03 | One student has unsorted overlapping/adjacent intervals on a date. | Merge them without crossing students or dates; reject invalid replacements atomically. |
| AC-04 | Create/edit a draft at the allowed boundaries and then try an invalid limit/reference. | Valid draft has stable identity and PENDING responses; invalid input changes nothing. |
| AC-05 | A three-invitee draft has minimum 2; edit its minimum to 3 and restart after saving. | Keep its ID and reload minimum 3. A minimum of 4 is rejected. |
| AC-06 | Save a UTF-8 name, availability and a draft, then restart. | Reload all collections and preserve IDs and relationships. |
| AC-07 | The save file is missing, corrupt, or cannot be atomically replaced. | Start empty only for missing data; report other failures without destroying the old file. |
| AC-08 | Enter yesterday, today and tomorrow for new availability/drafts. | Reject yesterday, allow today/tomorrow, and still load older valid files. |

## Quality requirements

| ID | Measure | Evidence / release |
| --- | --- | --- |
| NFR-01 | Clean build and tests work with JDK 21 using documented wrapper commands. | Our Maven Wrapper build passes; commands are in the README and results are in the test report. |
| NFR-02 | Invalid mutations keep existing records/reservations unchanged. | P1 state-preservation tests; extend to reservations in P2. |
| NFR-03 | Suggestions for 20 invitees, 20 rooms, 7 days and 100 events finish within 2 seconds over each of 10 measured runs, excluding startup. | P2 benchmark with machine/JDK details and individual timings. |
| NFR-04 | Application operations can run without the UI. | Service tests with in-memory storage and a fixed clock. |
| NFR-05 | Normal restart keeps data; failed saves report errors and preserve the previous valid file. | Real JSON round trips and failed-replacement tests. |

## Later-release acceptance

P2 suggestions must hold every invitee in the room, provide all required features, avoid scheduled room conflicts, and meet the chosen eligible-attendance threshold. Eligibility requires full availability and no overlapping ACCEPTED attendance elsewhere. Rank by eligible count descending, date/time ascending, unused seats ascending, then room ID ascending. Show at most five, names/reasons, and separate nonoverlapping room/attendance rejection counts when none qualify.

Confirmation revalidates the proposal. Cancellation frees commitments but retains the record/responses. PENDING/DECLINED do not reserve student time. Reject availability edits that invalidate accepted attendance. Roster CSV must quote commas, quotes and line breaks. Undo/redo must restore complete state, support at least 20 operations, skip failures/exports and clear redo after a new mutation.

P3 closures can overlap and act as their union; exact duplicates are rejected. Affected scheduled events retain reservations and receive a derived NEEDS_REPLAN flag. P2 files must migrate without losing IDs/responses and be backed up before the first migrated save. Replacement previews show impacts; failed/cancelled previews change nothing. A changed date/time resets all responses; a room-only change preserves them. History, closures and responses survive restart, and undo/redo restores the complete rescheduling change.

## Client questions and decisions

Our TA, Mahfuzur Rahman Emon, answered these questions as the client. We used the email replies to make the following decisions.

### Questions answered

| Question | TA answer | Our decision |
| --- | --- | --- |
| Can availability be entered for past dates? | Either allowing or rejecting is permitted. Current/future dates are recommended. | Reject new entries and edits before today. Keep valid historical files loadable. |
| Is the date window start/end or start plus a number of days? | Either works if it covers 1-7 consecutive days. | Use inclusive start/end dates. The same date twice means one day. |
| Must the operator be able to add rooms? | Load the seed catalogue and support display/filtering. Adding/editing rooms is optional. | Keep the room catalogue read-only. |
| Can minimum attendance change while the event is a draft? | Yes, if it remains within the specified limits. | Allow the draft edit to change it, from 2 up to the invitee count. |
| Can the four-minute demo be edited and narrated? | Yes, if it stays within four minutes and accurately shows the working app. | Keep the recording below four minutes and show the working P1 slice. |
| Are PRs, issues and commits enough for the worklog? | The follow-up says they are sufficient. | Use Git evidence for the worklog rather than duplicating it. Each member still needs their required individual evidence. |

### Other decisions

| Decision | Reason and check |
| --- | --- |
| Use String IDs throughout | The service generates UUID strings and the seed uses IDs such as `S1`. One type avoids broken references between components. See [PR #11](https://github.com/micobenissa/SENG-300-PROJECT/pull/11) and `StudentTest`. |
| Select invitees by list number | The operator can choose visible records without typing an ID. Numbers are converted to stored IDs; duplicates and missing references are rejected. |
| Ask for minimum attendance | This is the organizer's required threshold, not a calculated response count. For two invitees the only valid answer is 2. The brief's six-invitee/minimum-four example shows why it cannot always equal the invitee count. |
| Save explicitly | Completed edits stay in memory until Save. Exiting asks whether to save, discard or cancel. A failed save can be retried. |
| Load seed only into empty state | This is required by the brief and avoids replacing existing records. Missing storage itself starts empty. |
| Keep responses PENDING in P1 | Availability and responses are separate facts. Response entry and acceptance conflicts belong to P2. |
| No delete-draft or unload action | Neither is a required P1 workflow. The README explains backup/reset instead. |

### Assumptions and open points

- We apply the same current/future policy to new draft windows as to availability. The TA's explicit answer covers availability; the draft policy is our additional design choice.
- An old valid record is not corrupt just because time passed. Historical data can reload, but a new edit must use a valid current/future date. To reuse an expired draft, move its window forward.
- We use the computer's local date to decide today. Stored dates and times are not converted between time zones.
- P2/P3 ownership in our plan is a proposed split for the next releases.
