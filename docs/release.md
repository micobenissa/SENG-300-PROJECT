# P1 release record

## Version and build

| Item | Value |
| --- | --- |
| Repository | [micobenissa/SENG-300-PROJECT](https://github.com/micobenissa/SENG-300-PROJECT) |
| Java | Eclipse Temurin OpenJDK `21.0.12.1+1`, 64-bit |
| Maven | Wrapper 3.3.4, Maven 3.9.11 |
| Test machine | Windows 11 Home `10.0.26200` |
| Test date | 26 October 2026, America/Toronto |
| Tested source commit | `1ced4091d9c6e4fbd5b65c2303370697d69fc81a` |
| Branch | `main` |
| Release tag | [`p1`](https://github.com/micobenissa/SENG-300-PROJECT/tree/p1), pointing to the tested source commit |
| Archive contents | Source from the tested commit, with refreshed test reports and release details |
| Build command | `.\mvnw.cmd clean verify` |
| Result | 88 tests passed, 0 failures, 0 errors, 0 skipped |

## What we delivered in P1

We implemented student create/edit/view/search, room filtering, availability entry and merging, draft create/edit/view, and JSON save/reload. Invalid input leaves the existing records unchanged. Missing storage starts empty; corrupt files and failed saves show an error and preserve the previous file.

Our sample files include 12 students, 6 rooms and 3 drafts. The main seed has 7 dates of availability and a valid planning scenario. The second file has no availability, giving us a no-result scenario for P2.

The [test report](test-report/README.md) contains our test strategy, acceptance checks, results and coverage. Scheduling, responses, roster export and undo/redo are planned for P2. Closures and rescheduling are planned for P3.

## Repository status

Our three code commits are `40d35ac` for JSON integer validation, `e97cd6e` for past dates and `ff2d5d6` for CLI improvements. These changes and the documentation are on `main` at `1ced409`. We reran the build and packaged-app checks before creating the archive. The archive includes the refreshed reports from that run; no source code changed during packaging.

The `p1` tag is published at `1ced4091d9c6e4fbd5b65c2303370697d69fc81a`. The submission contains the source, `pom.xml`, Maven Wrapper, README, attribution, sample data and docs. We leave out `target/`, `.git/`, IDE caches, credentials and private individual submissions. The P1 demonstration is a separate `p1_demo.mp4` of at most four minutes.
