# Requirements traceability

We use this table to link each requirement to its acceptance criteria, issue, code and tests. P1 rows show the checks we passed. P2/P3 rows show the work and tests we have planned.

## Requirement mapping

| requirement_id | acceptance_scenario | issue | implementation_link | test_id | test_type | release | status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| FR-01 | AC-01 student fields, normalized duplicates, edit/search | [#2](https://github.com/micobenissa/SENG-300-PROJECT/issues/2), [#7](https://github.com/micobenissa/SENG-300-PROJECT/issues/7) | [Student](../src/main/java/campusplan/domain/Student.java), [service](../src/main/java/campusplan/application/CampusPlanService.java) | T01, T06, T09 | Unit / acceptance | P1 | Verified |
| FR-01 | AC-02 catalogue and all-feature filtering | [#3](https://github.com/micobenissa/SENG-300-PROJECT/issues/3) | [Room](../src/main/java/campusplan/domain/Room.java), [service](../src/main/java/campusplan/application/CampusPlanService.java) | T02, T06, T09 | Unit / acceptance | P1 | Verified |
| FR-02 | AC-03 interval limits, replace and merge | [#4](https://github.com/micobenissa/SENG-300-PROJECT/issues/4) | [Availability](../src/main/java/campusplan/domain/Availability.java), [AppState](../src/main/java/campusplan/domain/AppState.java) | T03, T05, T06, T09 | Unit / acceptance | P1 | Verified |
| FR-02 | AC-04 draft limits, references and PENDING responses | [#5](https://github.com/micobenissa/SENG-300-PROJECT/issues/5) | [DraftEvent](../src/main/java/campusplan/domain/DraftEvent.java), [service](../src/main/java/campusplan/application/CampusPlanService.java) | T04, T05, T06, T09 | Unit / acceptance | P1 | Verified |
| FR-02 | AC-05 stable draft ID and valid minimum edit | [#5](https://github.com/micobenissa/SENG-300-PROJECT/issues/5), [#6](https://github.com/micobenissa/SENG-300-PROJECT/issues/6) | [service](../src/main/java/campusplan/application/CampusPlanService.java) | T06, T07, M03 | Unit / integration | P1 | Verified |
| FR-02 | AC-08 current/future policy and historical load | [#4](https://github.com/micobenissa/SENG-300-PROJECT/issues/4), [#5](https://github.com/micobenissa/SENG-300-PROJECT/issues/5) | [date policy](../src/main/java/campusplan/application/CampusPlanService.java) | T09, T10 | Unit / acceptance | P1 | Verified |
| FR-03 | AC-06 UTF-8 round trip and restart with IDs | [#6](https://github.com/micobenissa/SENG-300-PROJECT/issues/6) | [JSON storage](../src/main/java/campusplan/persistence/JsonStorage.java) | T07, T09, M01 | Integration / acceptance | P1 | Verified |
| FR-03 | AC-07 missing/corrupt storage and failed save | [#6](https://github.com/micobenissa/SENG-300-PROJECT/issues/6) | [JSON storage](../src/main/java/campusplan/persistence/JsonStorage.java), [startup](../src/main/java/campusplan/Main.java) | T07, T09, M02 | Integration / acceptance | P1 | Verified |
| FR-03 | Required sample counts and valid/no-result fixtures | [#8](https://github.com/micobenissa/SENG-300-PROJECT/issues/8) | [samples](../sample-data/seed.json) | T08, M04 | Integration / fixture | P1 | Verified |
| FR-04 | Top five, eligibility, tie-breaks and rejection counts | P2 backlog | [scheduling contract](design.md#planned-scheduling-operations) | Planned ranking/boundary/no-result suite | Unit / acceptance | P2 | Planned |
| FR-05 | Revalidation, atomic reservation and cancellation | P2 backlog | [scheduling contract](design.md#planned-scheduling-operations) | Planned stale/conflict/cancel suite | Integration | P2 | Planned |
| FR-06 | Accepted conflicts, releases and availability edits | P2 backlog | [response contract](design.md#planned-scheduling-operations) | Planned response/conflict suite | Unit / integration | P2 | Planned |
| FR-07 | Date/status listing and quoted roster CSV | P2 backlog | [listing/export contract](design.md#planned-scheduling-operations) | Planned CSV comma/quote/newline cases | Integration | P2 | Planned |
| FR-08 | 20 undo operations, redo and failed-action exclusion | P2 backlog | [history contract](design.md#planned-reversible-operations) | Planned history/state-restoration suite | Unit / integration | P2 | Planned |
| FR-09 | Closure union/flags and P2-file migration | P3 backlog | [P3 contract](design.md#planned-p3-operations) | Planned closure/migration suite | Integration | P3 | Planned |
| FR-10 | Preview, reset/preserve responses, rollback and undo | P3 backlog | [P3 contract](design.md#planned-p3-operations) | Planned rescheduling regression suite | Integration / acceptance | P3 | Planned |
| FR-11 | Integrated guide, errors and repeatable handover | P3 backlog | [release plan](plan.md#iteration-schedule) | Planned final acceptance suite | Acceptance | P3 | Planned |
| NFR-01 | Clean JDK 21 wrapper build | [#1](https://github.com/micobenissa/SENG-300-PROJECT/issues/1), [#8](https://github.com/micobenissa/SENG-300-PROJECT/issues/8) | [pom](../pom.xml), [README](../README.md) | All T01-T10, M01 | Build / acceptance | P1-P3 | P1 build passed; tag pending |
| NFR-02 | Rejected mutations preserve whole state | [#5](https://github.com/micobenissa/SENG-300-PROJECT/issues/5), [#6](https://github.com/micobenissa/SENG-300-PROJECT/issues/6) | [immutable state](../src/main/java/campusplan/domain/AppState.java), [service](../src/main/java/campusplan/application/CampusPlanService.java) | T05, T06, T09, T10 | Unit / acceptance | P1-P3 | P1 verified; later reservations planned |
| NFR-03 | Specified maximum-size case under 2 seconds over 10 runs | P2 backlog | [benchmark plan](plan.md#five-main-risks) | Planned 10-run benchmark | Performance | P2 | Not measured yet |
| NFR-04 | Operations work without ConsoleApp | [#6](https://github.com/micobenissa/SENG-300-PROJECT/issues/6) | [service](../src/main/java/campusplan/application/CampusPlanService.java), [Storage](../src/main/java/campusplan/persistence/Storage.java) | T06, T10 | Unit | P1-P3 | Verified |
| NFR-05 | Restart and failed save preserve data | [#6](https://github.com/micobenissa/SENG-300-PROJECT/issues/6) | [JSON storage](../src/main/java/campusplan/persistence/JsonStorage.java) | T07, T09, M02 | Integration / acceptance | P1-P3 | P1 verified; P3 migration planned |

P2/P3 rows link to the planned contracts. We will add their issue and test links as we implement those features.

## Test IDs

| ID | Existing test class / key behaviour |
| --- | --- |
| T01 | [StudentTest](../src/test/java/campusplan/domain/StudentTest.java): names/IDs, email partitions, edits and partial search |
| T02 | [RoomTest](../src/test/java/campusplan/domain/RoomTest.java): limits and immutable features |
| T03 | [AvailabilityTest](../src/test/java/campusplan/domain/AvailabilityTest.java): boundaries and full coverage |
| T04 | [DraftEventTest](../src/test/java/campusplan/domain/DraftEventTest.java): invitees, dates, duration, minimum and responses |
| T05 | [AppStateTest](../src/test/java/campusplan/domain/AppStateTest.java): duplicates, references, merging and immutable lists |
| T06 | [CampusPlanServiceTest](../src/test/java/campusplan/application/CampusPlanServiceTest.java): operations and atomic rejection |
| T07 | [JsonStorageTest](../src/test/java/campusplan/persistence/JsonStorageTest.java): round trips, corruption, decimal rejection and failed replacement |
| T08 | [SampleDataTest](../src/test/java/campusplan/persistence/SampleDataTest.java): seed counts and no-result dataset |
| T09 | [ConsoleAppTest](../src/test/java/campusplan/cli/ConsoleAppTest.java): working slice, edits, errors, EOF, save/exit and seed handling |
| T10 | [PastDateTest](../src/test/java/campusplan/application/PastDateTest.java): yesterday/today/future, local-date clock and historical restart |

M01-M04 are the reproducible acceptance checks in the [test report](test-report/README.md#acceptance-checks). The [generated results](test-report/README.md#generated-test-results) record our test results.
