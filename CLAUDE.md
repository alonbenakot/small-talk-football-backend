# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

SmallTalkFootball backend: a Spring Boot 3.2.2 / Java 17 REST API that aggregates football (soccer) data from
apifootball.com into MongoDB and uses OpenAI (via Spring AI) to generate short conversational "one-liners" about
matches, plus curated educational articles and info snippets. The audience is people who want to hold a casual
football conversation without prior knowledge.

## Commands

```bash
./mvnw spring-boot:run              # run the app
./mvnw clean package                # build (add -DskipTests to skip)
./mvnw test                         # run all tests (~15s, no Docker or network needed)
./mvnw test -Dtest=ClassName        # run a single test class
./mvnw test -Dtest=ClassName#method # run a single test
./mvnw test -Dtest=ClassA,ClassB    # comma-separated, not '+'
```

### Required environment variables

`application.properties` resolves everything from the environment: `MONGODB_URI`, `API_FOOTBALL_KEY`,
`OPENAI_API_KEY`, `EMAIL_API_KEY`, and the optional `JWT_EXPIRATION` (hours, default 720), `MAX_MATCH_DAYS`
(default 7), `MATCH_DAYS_INTO_FUTURE` (default 7). These are needed only to run the app — the tests supply their
own dummy values.

## Tests

`src/test/resources/application.properties` **shadows** the main one rather than merging with it, so any new
property the context needs must be added there too. It sets `spring.data.mongodb.auto-index-creation=false`
because index creation runs eagerly and would otherwise require a live MongoDB just to load the context.

Test doubles live in `testsupport/`: `TestFixtures` is an object mother for domain objects, `MatchDtoJson` builds
apifootball match payloads (`MatchDto` has no setters, so JSON is the only way to construct one), and
`JsonFixtures` parses with the production `apiClientObjectMapper` so tests and production cannot drift apart.

Conventions worth matching when adding tests:

- Mapper, service, and filter tests are plain JUnit or Mockito — no Spring context, so they run in milliseconds.
- `FootballApiService` is tested against `MockRestServiceServer.bindTo(RestClient.Builder)`, which needs no extra
  dependency. Note `getMatches` issues one request per `Competition`, so expectations need `ExpectedCount`.
- `@WebMvcTest` pulls `Filter` beans into the slice, so controller tests exclude `JwtAuthFilter` via
  `excludeFilters`; auth is covered exhaustively in `JwtAuthFilterTest` instead.
- Several tests deliberately **pin known defects** rather than assert desirable behavior. Each carries a comment
  saying so and what to change if the defect is fixed — if one of those fails, the fix is to update the test, not
  to restore the old behavior. They cover the unbound `MatchDto` fields, the bracketed-score crash, the
  unauthenticated `GET /articles/pending`, `updateArticle` unpublishing, and the signup NPE.
- There are no MongoDB integration tests yet; repository queries and the `MongoTemplate` upsert in
  `TeamDataService` are currently only exercised through mocks.

## Architecture

Standard layering under `com.smalltalk.SmallTalkFootball`: `controllers` → `services` → `repositories`
(Spring Data MongoDB) over `domain` (`@Document` entities: `Fixture`, `TeamData`, `CompetitionData`, `Article`,
`SmallInfo`, `User`). `models` holds embedded/value types, `models/dto` holds the wire shapes of the external
football API, and `system/utils/mappers` converts DTO → domain.

### External football API boundary

`FootballApiService` is the only caller of apifootball.com. Every call goes through
`ResponseHandler.process(...)`, which never throws: it logs and returns `Optional.empty()` on a non-2xx body, an
HTML error page, or a parse failure — so callers get empty lists rather than exceptions. Preserve that contract
when adding endpoints.

Two `ObjectMapper` beans exist (`ObjectMapperConfig`): the `@Primary` one is `LOWER_CAMEL_CASE` for our own API,
and the `@Qualifier("apiClient")` one is `SNAKE_CASE` and must be used for anything deserialized from apifootball.

The `Competition` enum is the whitelist of tracked leagues, mapping our names to apifootball league ids. Adding a
competition here automatically widens fixture, standings, and team fetching, since those all iterate
`Competition.values()`.

### Fixture ingestion

`FixtureService.fetchAndSaveFixtures(matchDays, matchDaysIntoFuture)` fetches a window around today, maps each
`MatchDto` through `FullFixtureMapper` → `FixtureAssembler`, then:
- skips fixtures whose stored copy is already `finished` (finished matches are never re-fetched),
- carries the existing Mongo `_id` over via `externalId` so re-fetched fixtures update in place,
- enriches missing team name/coach/crest and derives the winner in `TeamDataService.enrichTeamsData`,
- deletes fixtures older than the window.

`FixtureAssembler` treats both `"Finished"` and `"After Pen."` match statuses as finished.

### Scheduled jobs

`FixturesJob` and `StandingsJob` (`services/jobs`) run on cron expressions in the `Asia/Jerusalem` zone;
`@EnableScheduling`/`@EnableAsync` are on the application class. The same work is also exposed manually as
admin-only endpoints (`POST /fixtures`, `POST /teams`, `PATCH /teams/standings`).

### AI one-liners

`OneLinersService.getOneLiner(fixtureId, teamType, lang)` returns a cached one-liner when the fixture is finished
and one already exists for that (teamType, language) pair — `OneLiner`'s `equals`/`hashCode` deliberately ignore
`text`, so the `Set<OneLiner>` on `Fixture` is keyed by team + language. Only finished fixtures persist their
one-liners; upcoming ones are regenerated every request.

`PromptBuilderFactory` picks between `FinishedFixtureOneLinerPromptBuilder` and
`UpcomingFixtureOneLinerPromptBuilder` (the latter additionally fetches head-to-head and standings data). All
builders implement `PromptBuilder`, whose default `buildPrompt()` assembles a fixed template from
`role/task/style/structure/constraints/examples/data` — add prompt variants by implementing those methods rather
than by writing new template strings. WORLD_CUP fixtures have coaches nulled out before prompt building.

### Security

There is no Spring Security. `JwtAuthFilter` is a plain `OncePerRequestFilter` that hard-codes which URI/method
combinations require a JWT and which require `Role.ADMIN`, so **adding a protected endpoint means editing the
`isJwtRequired*` / `isAdminOnlyRequest` methods in that filter**. Passwords are stored and compared in plaintext,
and the JWT signing key is a constant in `JwtUtil`.

### Responses and errors

Controllers return `SmallTalkResponse<T>`, a uniform envelope of `data`, `systemMessage`, `jwt`, and
`statusCode`. Domain code throws subclasses of `SmallTalkException` (400) or `NotFoundException` (404), which
`advices/ControllerAdvice` converts into the same envelope. User-facing strings live as constants in
`system/messages/Messages`.

### Seeded content

Articles and info snippets ship as JSON files (no extension) under `src/main/resources/data/articles/` and
`src/main/resources/data/infos/<category>/`. `POST /articles/init` and `POST /small-infos/init` **wipe the
collection and reload from disk**. `ArticleReader`/`SmallInfosReader` read via relative path
`src/main/resources/...`, so those endpoints only work when the process runs from the project root, not from a
packaged jar. Info files carry per-language variants (`AMERICAN`, `BRITISH`, `HEBREW`) in `infoTexts`.

### Email

`UserService` publishes a `UserCreatedEvent`; `EmailEventListener` handles it `@Async` and delegates to
`ResendEmailService` (the `EmailService` implementation backed by Resend).

## Conventions

- Lombok everywhere (`@Data`, `@Builder`, `@RequiredArgsConstructor`/`@AllArgsConstructor`, `@Slf4j`); logging is
  Log4j2 (Spring Boot's default logging starter is excluded).
- Mappers implement the shared `Mapper<S, T>` interface and are injected by `@Qualifier` name, since several
  implementations share the interface.
- CORS origins are hard-coded in `WebConfig` (production domains plus `http://localhost:5173`).
