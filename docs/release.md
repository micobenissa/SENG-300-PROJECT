# P1 release record

## Version and build

| Item | Value |
| --- | --- |
| Repository | [micobenissa/SENG-300-PROJECT](https://github.com/micobenissa/SENG-300-PROJECT) |
| Java | Eclipse Temurin OpenJDK `21.0.12.1+1`, 64-bit |
| Maven | Wrapper 3.3.4, Maven 3.9.11 |
| Test machine | Windows 11 Home `10.0.26200` |
| Test date | 26 October 2026, America/Toronto |
| Tested code commit | `ff2d5d67dadf616ad5b35f63463e8fd95c500296` |
| Local branch | `try/p1-validation` |
| Release tag | `p1`, not created yet |
| Final release commit | Pending final review, build and the `p1` tag |
| Build command | `.\mvnw.cmd clean verify` |
| Result | 88 tests passed, 0 failures, 0 errors, 0 skipped |

## What we delivered in P1

We implemented student create/edit/view/search, room filtering, availability entry and merging, draft create/edit/view, and JSON save/reload. Invalid input leaves the existing records unchanged. Missing storage starts empty; corrupt files and failed saves show an error and preserve the previous file.

Our sample files include 12 students, 6 rooms and 3 drafts. The main seed has 7 dates of availability and a valid planning scenario. The second file has no availability, giving us a no-result scenario for P2.

The [test report](test-report/README.md) contains our test strategy, acceptance checks, results and coverage. Scheduling, responses, roster export and undo/redo are planned for P2. Closures and rescheduling are planned for P3.

## Local status

Our three code commits are `40d35ac` for JSON integer validation, `e97cd6e` for past dates and `ff2d5d6` for CLI improvements. They are local and have not been pushed. The documentation and reports are recorded in separate local commits on `try/p1-validation`. Main is unchanged.

We will record the final release hash and create the `p1` tag after review and the final build. The submission contains the source, `pom.xml`, Maven Wrapper, README, attribution, sample data and docs. We leave out `target/`, `.git/`, IDE caches, credentials and private individual submissions. The P1 demonstration is a separate `p1_demo.mp4` of at most four minutes.
