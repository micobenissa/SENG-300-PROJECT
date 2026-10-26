# CampusPlan

CampusPlan is our event planning app for SENG 300. In P1, we built a CLI for managing students, filtering rooms, entering availability and creating draft events. We save the records locally in JSON so they can be loaded again after restarting.

## Repository

[micobenissa/SENG-300-PROJECT](https://github.com/micobenissa/SENG-300-PROJECT)

## Group

| Name | UCID |
| --- | --- |
| Raman Raut | 30247548 |
| Sardar Waheed | 30096015 |
| AbduMalek Ben Issa | 30240712 |
| Divakar Gaba | 30242369 |
| Saarim Ali | 30211679 |
| Selina Vuong | 30241862 |
| Abdullah Warraich | 30245056 |

## Requirements

- JDK 21, with `java` and `javac` on PATH.
- PowerShell on Windows, or a shell on macOS/Linux.
- Internet for the first dependency download; the app itself runs locally.

Check Java in the terminal:

```powershell
java -version
javac -version
```

Both should report version 21. If Java is not recognized, set `JAVA_HOME` to the JDK folder, add its `bin` folder to PATH, and open a new terminal. The wrapper downloads Maven 3.9.11; no separate Maven installation is needed.

## Build and run

Run these from the repository root, where `pom.xml` is located. Exit the CLI before rebuilding; Windows can lock an open JAR.

Build and run the tests:

```powershell
.\mvnw.cmd clean verify
```

Run the CLI:

```powershell
java -jar target\campusplan.jar
```

After editing code, rebuild with `.\mvnw.cmd package`. Use `.\mvnw.cmd test` for tests only.

On macOS/Linux, use `./mvnw clean verify` and `java -jar target/campusplan.jar` instead.

## First run

The app starts empty if there is no save file. Choose **5** to load our sample data before creating records, then **6** to save it.

| Main menu | What it does |
| --- | --- |
| 1 - Students | List, create, edit and search student records |
| 2 - Rooms | List rooms or filter by capacity and required features |
| 3 - Availability | View availability or replace the intervals on one date |
| 4 - Draft events | List, create, edit and view drafts |
| 5 - Load sample data | Import the sample file into an empty application |
| 6 - Save changes | Write the current records to the data file |
| 0 - Exit | Exit, with a save/discard/cancel prompt if changes are unsaved |

The top counts show loaded records. **Unsaved changes** means edits still need saving. Failed input does not replace existing records.

## Entering a draft

Select 2-20 different existing students using the numbers beside their names, such as `1,3`, rather than names, emails or IDs. The app stores their actual IDs.

**First date/Last date** are the earliest/latest possible dates for one event. The inclusive window is 1-7 days; use the same date twice for one day. Duration is separate: 30-180 minutes in steps of 30.

**Minimum attendance** is how many invitees must be able to attend: 2 through the number invited. Two invitees means enter `2`; six means choose `2` to `6`. P1 responses start `PENDING`; availability does not count as acceptance.

Features are `WHITEBOARD`, `PROJECTOR` and `ACCESSIBLE`, comma-separated; blank means none. Draft editing replaces all planning fields. Blank name/email fields keep previous values when editing a student.

## Data and sample files

The default save is `data/campusplan.json`, relative to the launch directory. It stores all four domain collections as UTF-8 JSON and is ignored by Git.

- `sample-data/seed.json`: 12 students, 6 rooms, 7 availability dates and 3 drafts, including a valid planning scenario.
- `sample-data/no-result.json`: the same records with no availability, so no scheduling candidate meets minimum attendance.

Choose another save/seed file with:

```powershell
java -jar target\campusplan.jar --data data\trial.json
java -jar target\campusplan.jar --data data\no-result-trial.json --seed sample-data\no-result.json
java -jar target\campusplan.jar --help
```

`--seed` selects the source; option **5** imports it only into completely empty state.

## Backup and reset

Close the app first. To reset the default save without losing it, rename it from the project terminal:

```powershell
Rename-Item .\data\campusplan.json ("campusplan-backup-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".json")
```

Restart to begin empty; choose **5** for fresh sample data. To restore, close the app, move the current save aside and rename the backup to `campusplan.json` in `data/`. For `--data`, back up the chosen file instead.

Corrupt storage is reported and kept unchanged. Restore a backup or choose another data path. A failed save keeps the old file and completed edits in memory so you can retry.

## Tests

Our P1 code passes **88 JUnit 5 tests**, with no failures or errors. `verify` creates the test report at `target/reports/surefire.html` and the coverage report at `target/site/jacoco/index.html`.

See the [test report](docs/test-report/README.md) for strategy, acceptance steps and regression evidence.

## Project layout

```text
src/main/java/campusplan/
  Main.java       Startup options and application wiring
  cli/            Menus, input and output
  application/    Application operations and date policy
  domain/         Students, rooms, intervals, drafts and state validation
  persistence/    Storage interface, JSON files and in-memory storage
src/test/java/    Domain, application, persistence and CLI tests
sample-data/     Positive and no-result datasets
docs/            Requirements, design, plan, decisions and test evidence
```

## P1 limits

Scheduling, reservations, response entry, roster export and undo/redo are P2. Closures/rescheduling are P3. Rooms are read-only; there is no delete-draft or unload menu.

New availability/draft windows must use today or later. Historical records and fixed-date samples still load/save. Availability is within 08:00-22:00 on 30-minute boundaries; clearing affects only the selected date.

## Documentation

| Submission document | Contents |
| --- | --- |
| [Requirements](docs/requirements.md) | Rules, use cases, acceptance criteria and client answers |
| [Design](docs/design.md) | Architecture, diagrams, contracts and three ADRs |
| [Plan](docs/plan.md) | Ownership, backlog, schedule, risks, review evidence and retrospective |
| [Traceability](docs/traceability.md) | Requirement, issue, implementation and test mapping |
| [Test report](docs/test-report/README.md) | Strategy, acceptance checks, results and coverage |
| [Release record](docs/release.md) | Code version, build details and test date |
| [Attribution](attribution.md) | Tools, sources and libraries |

Our written documents are Markdown. The test-report folder also includes the reports produced by Maven and JaCoCo.
