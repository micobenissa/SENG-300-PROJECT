# CampusPlan plan

For P1, we built student records, room filtering, availability, draft events and JSON storage. P2 adds scheduling and undo/redo. P3 adds closures and rescheduling.

## P1 ownership and evidence

We split the implementation by feature. The issues and PRs below link to each person's work.

| Member | P1 area | Issue and implementation evidence |
| --- | --- | --- |
| AbduMalek Ben Issa | Project setup, build, CLI and integration | [#1](https://github.com/micobenissa/SENG-300-PROJECT/issues/1), [PR #9](https://github.com/micobenissa/SENG-300-PROJECT/pull/9); [#7](https://github.com/micobenissa/SENG-300-PROJECT/issues/7), [PR #15](https://github.com/micobenissa/SENG-300-PROJECT/pull/15) |
| Divakar Gaba | Student records, validation and tests | [#2](https://github.com/micobenissa/SENG-300-PROJECT/issues/2), [PR #11](https://github.com/micobenissa/SENG-300-PROJECT/pull/11), fix `fad5ed5` |
| Selina Vuong | Room catalogue domain rules and tests | [#3](https://github.com/micobenissa/SENG-300-PROJECT/issues/3), [PR #10](https://github.com/micobenissa/SENG-300-PROJECT/pull/10) |
| Saarim Ali | Availability boundaries and tests | [#4](https://github.com/micobenissa/SENG-300-PROJECT/issues/4), [PR #12](https://github.com/micobenissa/SENG-300-PROJECT/pull/12) |
| Sardar Waheed | Draft events, responses and AppState | [#5](https://github.com/micobenissa/SENG-300-PROJECT/issues/5), [PR #13](https://github.com/micobenissa/SENG-300-PROJECT/pull/13) |
| Abdullah Warraich | Service operations, storage and integration tests | [#6](https://github.com/micobenissa/SENG-300-PROJECT/issues/6), [PR #14](https://github.com/micobenissa/SENG-300-PROJECT/pull/14) |
| Raman Raut | Sample datasets and final checks | [#8](https://github.com/micobenissa/SENG-300-PROJECT/issues/8), [PR #16](https://github.com/micobenissa/SENG-300-PROJECT/pull/16) |

## Backlog

All FR and NFR items are required. We prioritized FR-01 to FR-03 for P1 and kept the later features in the next two iterations.

| Item | Priority / release | Acceptance and current position |
| --- | --- | --- |
| FR-01 | Must / P1 | Student operations and room filtering pass; AC-01/02. |
| FR-02 | Must / P1 | Availability and draft validation/editing pass; AC-03/04/05/08. |
| FR-03 | Must / P1 | Save/restart and failure protection pass; AC-06/07. |
| FR-04 | Must / P2 | Top five explained suggestions, exact tie-break order and no-result categories. Planned. |
| FR-05 | Must / P2 | Revalidated confirmation/cancellation with atomic reservations. Planned. |
| FR-06 | Must / P2 | Response entry, accepted conflicts and invalidating-availability protection. Planned. |
| FR-07 | Must / P2 | Date/status listing and escaped UTF-8 roster export. Planned. |
| FR-08 | Must / P2 | At least 20 complete undo/redo operations and redo invalidation. Planned. |
| FR-09 | Must / P3 | Closure union, affected events and safe P2-file migration. Planned. |
| FR-10 | Must / P3 | Explained previews, atomic replacement, response/history handling and reversal. Planned. |
| FR-11 | Must / P3 | Integrated acceptance suite, final guide and reproducible handover. Planned. |
| NFR-01 | Must / every release | JDK 21 wrapper build passes; repeat the build from the final release tag. |
| NFR-02 | Must / every release | P1 invalid-state preservation passes; extend to scheduled reservations/history. |
| NFR-03 | Must / P2 | Measure the specified maximum-size suggestion case over 10 runs; each under 2 seconds excluding startup. |
| NFR-04 | Must / every release | Service operations tested without the CLI; retain this boundary in later releases. |
| NFR-05 | Must / every release | Restart/failed-save protection passes; extend to P3 migration and history. |

The [requirements](requirements.md) give the rules and acceptance criteria. [Traceability](traceability.md) links each requirement to the code and tests.

## Iteration schedule

| Iteration | Work | Acceptance criteria |
| --- | --- | --- |
| P1, weeks 1-3 | Client questions, use cases, architecture, contracts, student/room/availability/draft features and JSON storage | FR-01 to FR-03 work through the CLI; invalid input leaves records unchanged; saved records survive restart. Our 88 tests pass. |
| P2, weeks 4-5 | Extend the event model, add suggestions, confirmation, cancellation, responses, event lists, roster export and undo/redo | FR-04 to FR-08 meet the ranking, conflict, export and history rules. |
| P2, week 6 | Integration tests, performance test, P2 sample file, release and demo | Run all regressions and the 10-run benchmark. Retain a P2 save for migration tests. Brief deadline: 16 November 2026. |
| P3, week 7 | Change-impact analysis, revised acceptance criteria, migration plan and an updated ADR | Explain the changes needed for closures and rescheduling before implementing them. |
| P3, week 8 | Closures, affected events, replacement previews and confirmation | FR-09 and FR-10 preserve reservations, responses and history correctly, including failed changes and undo. |
| P3, week 9 | Full regression tests, updated diagrams, user guide and final handover | FR-11, migration tests and final demonstrations are complete. Brief deadline: 9 December 2026. |

The brief gives two different P1 deadlines. We record that open point in [requirements](requirements.md#assumptions-and-open-points).

## Proposed next owners

This is our proposed split for P2/P3, including release coordination. These assignments are subject to change if problems arise or anyone's availability changes. We will agree on any changes as a group.

| Area | P2 lead | P3 continuation |
| --- | --- | --- |
| Scheduling enumeration/ranking and benchmark support | Saarim | Apply closure eligibility to alternatives |
| Room reservation/conflict rules | Selina | Closures and affected-event detection |
| Event lifecycle, listing and roster export | Sardar | Replacement plans and read-only rescheduling history |
| Participant responses/conflicts | Divakar | Response-reset and accepted-attendance regressions |
| Persistence and command/history infrastructure | Abdullah | Versioned migration, backups and complete reversal |
| CLI workflows and integration | AbduMalek | Closure/preview/confirmation screens and integration |
| Acceptance datasets, release checks and demo | Raman | Migration fixtures, integrated acceptance and handover |

We propose Raman for P2 release coordination and Divakar for P3 so the role rotates. Each member will also own tests and review another member's work.

## Dependencies

1. Agree the shared event, ID and time contracts before adding P2 operations.
2. Extend `DraftEvent` into `Event`/`ScheduledPlan` before confirmation and response entry.
3. Build eligibility and ranking before confirmation. Confirmation must rerun the same checks.
4. Route successful edits through the history manager before adding undo/redo.
5. Keep a real P2 save for the P3 migration tests.
6. Add closures before replacement suggestions, then test response resets, rollback and undo together.

## Five main risks

| Risk | Impact | Mitigation and owner |
| --- | --- | --- |
| Shared model contracts diverge | Components stop compiling or references break. | Keep String IDs and explicit contracts; integrate early. Divakar/Abdullah. PR #11 already exposed this risk. |
| Save or migration damages valid data | Plans are lost across restart. | Validate JSON, use atomic replacement/backups and failure tests. Abdullah. |
| Time/window/conflict boundaries are wrong | Adjacent events conflict, invalid intervals pass, or availability is overcounted. | Fixed clocks, inclusive-window limits and half-open/full-coverage tests. Saarim/Selina. |
| Compound changes cannot be reversed coherently | Undo restores only part of a reservation or reschedule. | Immutable before/after state, one history entry and rollback tests. Abdullah/Sardar. |
| Integration and release evidence fall behind code | A working local app differs from the tagged/submitted version. | Small reviewed PRs, linked tests, final clone check, release record and recording check. Raman/AbduMalek. |

For NFR-03, we will test 20 invitees, 20 rooms, a seven-day window and 100 events. We will record all 10 timings and the machine/JDK used. Each run must finish within two seconds, excluding startup.

## Review evidence

Our P1 work is in PRs #9-#16. Reviews helped us catch an ID mismatch before integration: Selina found that the student code used `int` IDs, while the other components used String IDs. Divakar fixed it in [fad5ed5](https://github.com/micobenissa/SENG-300-PROJECT/commit/fad5ed593ec71301a98da74b1a16ca636ea62f13) and requested another review.

| PR | Recorded review |
| --- | --- |
| [#9 setup](https://github.com/micobenissa/SENG-300-PROJECT/pull/9) | Divakar approved and included local build evidence. |
| [#10 rooms](https://github.com/micobenissa/SENG-300-PROJECT/pull/10) | Saarim approved. |
| [#11 students](https://github.com/micobenissa/SENG-300-PROJECT/pull/11) | Selina reported the ID mismatch; Divakar fixed it. The available records do not show a final approval after that fix. |
| [#12 availability](https://github.com/micobenissa/SENG-300-PROJECT/pull/12) | Sardar approved. |
| [#13 drafts](https://github.com/micobenissa/SENG-300-PROJECT/pull/13) | Abdullah approved. |
| [#14 service/storage](https://github.com/micobenissa/SENG-300-PROJECT/pull/14) | AbduMalek approved. |
| [#15 CLI](https://github.com/micobenissa/SENG-300-PROJECT/pull/15#issuecomment-6406542926) | Raman confirmed it was good to merge in a PR comment. |
| [#16 samples](https://github.com/micobenissa/SENG-300-PROJECT/pull/16#issuecomment-6406873931) | Divakar recorded passing tests and approval in a PR comment. |

## Working agreements

Our process is to track tasks in issues, use feature branches and PRs, and include the issue link, changes and test results in each PR. Another teammate must review and approve code before it is merged. Changes made after review need another check. Main must stay buildable and protected from direct pushes.

We will keep weekly decisions and blockers in the issue board and raise unresolved blockers to the TA within two teaching days. The TA confirmed that PRs, issues and commits are sufficient worklog evidence. Each member still submits their private evidence summary.

## P1 retrospective

### What worked

Splitting the work by feature gave us clear owners for the domain classes, service, CLI and sample data. We could test the domain and service without running the menus. The storage and CLI tests then checked how the parts worked together, including saving and restarting.

### What we fixed

The String/int ID mismatch showed why we need shared types before working on separate components. We also found that Jackson accepted decimal values such as `180.9` for whole-number fields. We disabled that conversion and added regression tests. Our date checks now reject new past dates while still loading older saves, and fixed clocks keep those tests repeatable.

### What we will change for P2

We will agree shared contracts earlier, update the docs with each feature and review the final changed code before merging. We will add ranking, conflict and undo tests before connecting the P2 workflows to the CLI.
