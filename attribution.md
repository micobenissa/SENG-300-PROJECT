# P1 attribution

## How we used AI

During planning and setup, we used Google AI Overviews for questions about the assignment, technical explanations when we got stuck, and design ideas. We checked the answers against the assignment and client emails.

We also read Google AI Overviews when looking up questions about our approaches and edge cases we might have missed. This helped us find things to check against the project requirements.

## Team members and roles

| Member | P1 role |
| --- | --- |
| AbduMalek Ben Issa | Project setup, build, CLI and integration |
| Divakar Gaba | Student records, validation and tests |
| Selina Vuong | Room catalogue rules and tests |
| Saarim Ali | Availability rules and tests |
| Sardar Waheed | Draft events, responses and AppState |
| Abdullah Warraich | Application service, storage and integration tests |
| Raman Raut | Sample datasets and final checks |

The roles above come from our issues and PRs. The individual descriptions below are drafts; each member's AI-use details and dates still need their confirmation.

## Individual AI-use descriptions

### AbduMalek Ben Issa

i needed a refresher on some of the things i wanted to do with the cli, so i used google ai overviews for guidance. i explored different layout and menu ideas and worked through a few designs before settling on the one we have now. i used it for design ideas, not to write the code.

### Divakar Gaba

i needed a refresher on validation, so i looked up questions on google and read the ai overviews while planning the student records.

### Selina Vuong

i used google ai overviews to help me understand parts of the room catalogue requirements and think about how to organize my part.

### Saarim Ali

i had some questions about handling dates and times, so i used google ai overviews for guidance while working out the availability rules.

### Sardar Waheed

i looked things up on google when i needed help understanding parts of the draft events. the ai overviews helped me get a clearer idea of how to approach them.

### Abdullah Warraich

i used google ai overviews for a refresher on json storage and to help me think through how the service and storage would fit together.

### Raman Raut

i used google ai overviews to help me plan the layout of the sample data and figure out how to organize it for our app.

## Other sources and libraries

| Source | Where we used it | Licence or source status |
| --- | --- | --- |
| [Jackson 2.18.3](https://github.com/FasterXML/jackson-databind/tree/jackson-databind-2.18.3), including Java time support | JSON persistence; dependencies in `pom.xml` | [Apache License 2.0](https://github.com/FasterXML/jackson-databind/blob/jackson-databind-2.18.3/LICENSE) |
| [JUnit Jupiter 5.11.4](https://github.com/junit-team/junit5/tree/r5.11.4) | Tests under `src/test/java` | [Eclipse Public License 2.0](https://github.com/junit-team/junit5/blob/r5.11.4/LICENSE.md) |
| [JaCoCo 0.8.13](https://www.jacoco.org/jacoco/) | Test coverage through `pom.xml` | [Eclipse Public License 2.0](https://www.jacoco.org/jacoco/trunk/doc/license.html) |
| [Apache Maven and Maven Wrapper](https://maven.apache.org/tools/wrapper/), plus Apache Maven build plugins | Wrapper 3.3.4, Maven 3.9.11, wrapper scripts, `.mvn/wrapper/` and build configuration | [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
