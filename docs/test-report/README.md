# P1 tests

We ran `.\mvnw.cmd clean verify` on 26 October 2026 using JDK 21 and Maven 3.9.11 on Windows 11 Home `10.0.26200`. All **88 tests passed**, with no failures, errors or skipped tests. The reports were regenerated from tested source commit `b161358302bf3a1630749cf1bd0faf09da387c4b`.

## Run the checks

From the project root:

```powershell
.\mvnw.cmd clean verify
```

For the service, date and storage tests only:

```powershell
.\mvnw.cmd "-Dtest=CampusPlanServiceTest,JsonStorageTest,PastDateTest" test
```

Close the CLI before rebuilding because Windows can lock the running JAR. On macOS/Linux, use `./mvnw` instead of `.\mvnw.cmd`.

## Included reports

- [JUnit test results](generated/tests/surefire.html)
- [JaCoCo coverage](generated/coverage/index.html)
- [Coverage XML](generated/coverage/jacoco.xml) and [CSV](generated/coverage/jacoco.csv)
- Raw JUnit XML files in `generated/junit/`.

Maven and JaCoCo produce the HTML reports automatically. We included their supporting files so the reports open locally. The written report is this Markdown file. Running `verify` again creates new reports under `target/reports/`, `target/surefire-reports/` and `target/site/jacoco/`.

## Test strategy

We test the domain rules first, then the service and storage, then the CLI workflows.

| Layer | What we test |
| --- | --- |
| Domain | Student and room limits, duplicate emails/IDs, references, interval boundaries and merging, draft limits and PENDING responses. |
| Application | Create/edit/search/filter, replacing one date of availability, seed loading, saving and keeping the old state after invalid input. |
| Storage | Real JSON round trips in temporary folders, UTF-8, stable IDs, missing/corrupt files and failed replacement. |
| CLI | Scripted input for the P1 workflows, invalid forms, empty input, save/discard/cancel and seed errors. |
| Packaged app | Starting the JAR, saving and restarting, corrupt files, backup/reset and a Windows file-lock save failure. |

We use boundary values and invalid inputs rather than testing only the sample data. These include capacities 0/1/100/101, 1/2/20/21 invitees, 1/7/8-day windows, invalid duration steps, minimum attendance outside the invitee count, and times at 08:00 and 22:00. We also test adjacent/overlapping intervals, incomplete coverage, malformed emails and unknown references.

Date tests use fixed clocks. We check yesterday, today and tomorrow, preserve the old state after a rejected edit, and reload older saved records.

## Regression tests

| Problem | What we changed and tested |
| --- | --- |
| Student IDs used `int` while the other components used String | We changed the student ID type and added `StudentTest.seedAndGeneratedStringIdsSurviveEdits`. The fix is [fad5ed5](https://github.com/micobenissa/SENG-300-PROJECT/commit/fad5ed593ec71301a98da74b1a16ca636ea62f13). |
| Jackson converted decimal values into integers before validation | We disabled `ACCEPT_FLOAT_AS_INT`. `JsonStorageTest.fractionalIntegersAreRejectedWithoutOverwritingStorage` checks capacity, duration and minimum attendance, including unchanged file bytes. |
| Date checks could break older saves or fail as time passed | We put the current-date check in the service and injected a `Clock`. `PastDateTest` covers fixed dates, rejected edits and historical reloads. |
| A failed save could damage the old file | We write a temporary file and replace the destination atomically. `failedAtomicReplacementKeepsPreviousValidBytesAndRemovesTemporaryFile` checks the old bytes and cleanup. We also tested a real Windows replacement failure. |

## Acceptance checks

We ran these checks using the packaged JAR and separate trial save files. All passed.

### M01 Main P1 workflows and restart

Start with a missing `--data` path and load the seed through option 5. Create, edit and search a student, then try a duplicate email with different case/spaces. Enter overlapping and adjacent availability, filter rooms by capacity/features, create a draft, save and restart using the same path.

**Result:** the duplicate was rejected, intervals merged, filters returned matching rooms, and saved IDs and records survived restart. Invalid input left the existing state unchanged.

### M02 Storage errors and backup/reset

Launch with malformed JSON at a trial `--data` path. Repeat with null/missing-field data, unknown references and decimal values in whole-number fields. Try a failed save replacement. For reset, close the app, rename a valid save to a backup, and restart using the original path.

**Result:** bad files were rejected with an error and kept unchanged. Failed replacement preserved the old file and current edits. A missing save started empty and offered seed loading.

### M03 Edit a draft minimum

Create a three-invitee draft with minimum 2 and a current/future window. Save, note its ID, change the minimum to 3, save and restart. Then try minimum 4.

**Result:** the ID stayed the same and minimum 3 reloaded. Minimum 4 was rejected without changing the draft. Responses stayed PENDING.

### M04 Sample scenarios

Check the counts in both sample files. In `seed.json`, E1 has six invitees, a 120-minute duration, minimum four and WHITEBOARD. On its fixture date, S1-S5 are available from 12:00-14:00 and Cedar 101 holds all six invitees. `no-result.json` has no availability.

**Result:** sample counts and references passed. A separate calculation found 15 valid E1 candidates and 90 E3 candidates in the main seed, and zero for every event in the no-result file. These checks validate our sample data; scheduling itself is planned for P2.

## Generated test results

The included JUnit XML records the following totals. Parameterized test cases count as separate tests.

| Test class | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| [CampusPlanServiceTest](../../src/test/java/campusplan/application/CampusPlanServiceTest.java) | 10 | 0 | 0 | 0 |
| [PastDateTest](../../src/test/java/campusplan/application/PastDateTest.java) | 6 | 0 | 0 | 0 |
| [ConsoleAppTest](../../src/test/java/campusplan/cli/ConsoleAppTest.java) | 8 | 0 | 0 | 0 |
| [AppStateTest](../../src/test/java/campusplan/domain/AppStateTest.java) | 3 | 0 | 0 | 0 |
| [AvailabilityTest](../../src/test/java/campusplan/domain/AvailabilityTest.java) | 9 | 0 | 0 | 0 |
| [DraftEventTest](../../src/test/java/campusplan/domain/DraftEventTest.java) | 3 | 0 | 0 | 0 |
| [RoomTest](../../src/test/java/campusplan/domain/RoomTest.java) | 1 | 0 | 0 | 0 |
| [StudentTest](../../src/test/java/campusplan/domain/StudentTest.java) | 32 | 0 | 0 | 0 |
| [JsonStorageTest](../../src/test/java/campusplan/persistence/JsonStorageTest.java) | 15 | 0 | 0 | 0 |
| [SampleDataTest](../../src/test/java/campusplan/persistence/SampleDataTest.java) | 1 | 0 | 0 | 0 |
| **Total** | **88** | **0** | **0** | **0** |

## Coverage and limits

These values come from the included JaCoCo report.

| Package | Lines covered | Line coverage | Branches covered | Branch coverage |
| --- | ---: | ---: | ---: | ---: |
| `campusplan/domain` | 152/152 | 100.0% | 140/142 | 98.6% |
| `campusplan/cli` | 270/310 | 87.1% | 76/105 | 72.4% |
| `campusplan` | 0/36 | 0.0% | 0/12 | 0.0% |
| `campusplan/application` | 71/73 | 97.3% | 44/44 | 100.0% |
| `campusplan/persistence` | 43/43 | 100.0% | 6/6 | 100.0% |

The two missed domain branches are short-circuit paths in `AppState.isEmpty`. The two missed application lines are the default-clock constructor. The packaged-JAR checks exercise production startup and `Main`, but those separate processes are not included in the unit-test coverage.

The uncovered CLI branches include some back/invalid/empty-list paths, malformed-time recovery, EOF save failure and long-heading wrapping. Our tests cover the main P1 workflows and failure paths.

## P2 and P3 tests

For P2, we will add suggestion ranking/tie cases, room and participant conflicts, stale confirmation, cancellation, responses, CSV quoting and undo/redo. We will check at least 20 operations, redo after undo, and a new edit clearing redo. NFR-03 also needs the specified 10-run benchmark.

For P3, we will test overlapping/duplicate closures, affected-event flags, P2-file migration and backup, failed migration, replacement previews, response resets, rollback and complete rescheduling undo/redo.
