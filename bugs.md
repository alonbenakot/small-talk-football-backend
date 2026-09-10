# Known bugs

Found while building the test suite. Except where an entry is struck through and marked **fixed**, the tests
**pin current behaviour** so the suite stays green, and each pinning test carries a comment saying what to change
once the bug is fixed. If you fix one of these, the named test is expected to fail; update the test rather than
restoring the old behaviour, and record the fix on the entry as #3 does.

"Confirmed by test" means a test executes the faulty path and asserts the wrong-but-current outcome. Two entries
are marked "by inspection" because nothing automated covers them.

| # | Severity | Summary |
|---|----------|---------|
| 1 | High | `GET /articles/pending` requires no authentication |
| 2 | High | A bracketed score aborts the entire fixture ingestion run |
| 3 | ~~Medium~~ | ~~Three `MatchDto` fields never deserialize~~ — **fixed** |
| 4 | Medium | `PATCH /articles` silently unpublishes an article |
| 5 | Medium | Signup without `userIndications` returns 500 |
| 6 | Medium | Out-of-range request parameters return 500, not 400 |
| 7 | Medium | Welcome emails can never be sent — wrong placeholder syntax |
| 8 | Low | One malformed league id aborts a whole refresh |
| 9 | Low | Builder-made `Fixture` cannot accept a one-liner |
| 10 | Low | Expired tokens throw instead of validating to `false` |
| 11 | Medium | A player who leaves the tracked leagues is never deleted |

---

## 1. `GET /articles/pending` requires no authentication — High

**Where:** `security/JwtAuthFilter.java:84`, with `isJwtRequiredArticles` at `:118`

```java
if ("articles/pending".equals(uri)) {   // uri is "/articles/pending"
    return true;
}
```

**What happens:** Unpublished, unreviewed articles are readable by anyone.

**Why:** Two gaps line up. `isAdminOnlyRequest` compares against `"articles/pending"` with no leading slash, but
`HttpServletRequest.getRequestURI()` always returns `/articles/pending`, so that branch is unreachable. And
`isJwtRequired` only demands a token for `POST` and `PATCH` on `/articles`, so a `GET` never reaches the admin
check at all. The dead branch hides the fact that the endpoint was never gated.

**Confirmed by test:** `JwtAuthFilterTest.KnownGaps.pendingArticlesAreReadableWithoutAnyToken`

**Suggested fix:** Add the leading slash *and* make `isJwtRequired` cover `GET /articles/pending`. Fixing only the
slash is not enough — without the token requirement the request still never reaches `isAdminOnlyRequest`.

---

## 2. A bracketed score aborts the entire fixture ingestion run — High

**Where:** `system/utils/mappers/FixtureAssembler.java:232` (`deriveScoreFromGoals`), reached from `:180` and `:185`

```java
// mapSingleGoal, line 131 - strips the brackets:
String rawScore = goalDto.getScore().replace("[", "").replace("]", "");

// deriveScoreFromGoals, line 232 - does not:
String[] scoreParts = lastGoal.getScore().split(" - ");
int homeScore = Integer.parseInt(scoreParts[0].trim());   // "[3" -> NumberFormatException
```

**What happens:** apifootball reports running scores bracketed (`"[3 - 2]"`) in some payloads.
`mapSingleGoal` strips the brackets; `deriveScoreFromGoals` does not, so `Integer.parseInt("[3")` throws.

The blast radius is what makes this High rather than Medium. Neither caller guards the call — the one at `:180`
is *inside* the `catch` block, where a new exception propagates rather than being caught — so the throw escapes
`assembleFromFullMatch`, escapes the `.map()` in `FixtureService.fetchNewFixtures`, and kills the whole
`FixturesJob` run. One malformed match loses every fixture in that pass, not just its own.

Both fallback paths are affected: the "After Pen." path (`:185`) and the unparseable-reported-score path (`:180`).

**Confirmed by test:** `FixtureAssemblerTest.ScoreMapping.throwsWhenDerivingAScoreFromABracketedRunningScore` and
`throwsWhenTheReportedScoreIsMissingAndTheRunningScoreIsBracketed`

**Suggested fix:** Extract the bracket-stripping and score-splitting that `mapSingleGoal` already does into one
helper and use it in both places. Consider also making `FixtureService` resilient to a single bad match rather
than letting one failure discard the batch.

---

## 3. Three `MatchDto` fields never deserialize — Medium — FIXED

**Where:** `models/dto/MatchDto.java` — `matchHometeamName` (`:21`), `matchAwayteamName` (`:6`), `lineup` (`:7`)

```java
private String matchHometeamName;              // implicit name: matchHometeamName
public String getMatchHomeTeamName(){ ... }    // implicit name: matchHomeTeamName  <- different
```

**What happens:** `getMatchHomeTeamName()`, `getMatchAwayTeamName()` and `getMatchLineup()` always return `null`,
no matter what the payload contains. Consequently `FixtureAssembler.getCoach()` is dead code that always returns
`null`, and every fixture comes out of the assembler with null team names and null coaches.

**Why:** The fields are private with no setters. Jackson can pull in a non-visible field only when a visible
accessor shares its *implicit name* (`MapperFeature.INFER_PROPERTY_MUTATORS`). Here the getters are spelled
differently from their fields, so Jackson sees two unrelated properties: a private, invisible field with no
mutator, and a read-only getter. Nothing binds. The whole `lineup` subtree has the same problem at every level
(`MatchLineup.home`/`getHomeLineUp`, `LineUp.coach`/`getCoaches`).

Fields whose getter matches the field name — `matchHometeamSystem`, `statistics`, `goalscorer`, `substitutions` —
bind correctly, which is why this is easy to miss.

**Why nothing visibly broke:** `TeamDataService.enrichTeamsData` backfills names, crests and coaches from the
`get_teams` endpoint and derives the winner afterwards, so the pipeline self-heals. The cost is that the
enrichment is load-bearing rather than a fallback, and `Score.winner` is computed from a null name first.

**Confirmed by test:** `FixtureAssemblerTest.TeamNameAndCoachBinding` (4 cases, named `UnboundFields` while
this was still open)

**Suggested fix:** Rename the getters to match their fields (`getMatchHometeamName`, `getLineup`), or add
`@JsonProperty` to the fields. Do the same through `MatchLineup` and `LineUp`. Expect `enrichTeamsData` to become
a genuine fallback afterwards.

**Fixed:** `@JsonProperty` with the verbatim snake_case wire name was added to `MatchDto.matchHometeamName`
(`"match_hometeam_name"`), `MatchDto.matchAwayteamName` (`"match_awayteam_name"`), `MatchDto.lineup` (`"lineup"`),
`MatchLineup.home`/`away`, and `LineUp.coach`. The explicit name is required because Jackson does not apply the
`SNAKE_CASE` strategy to an explicitly named property. `FixtureAssemblerTest.TeamNameAndCoachBinding` (was
`UnboundFields`) now asserts correct binding, and `MatchDtoTest` exercises the deserialization directly.
`TeamDataService.enrichTeamsData` is now a genuine fallback rather than load-bearing.

---

## 4. `PATCH /articles` silently unpublishes an article — Medium

**Where:** `services/ArticleService.java:91` in `updateNonNullFields`

```java
if (article.getTitle() != null)  { articleFromDb.setTitle(article.getTitle()); }
if (article.getAuthor() != null) { articleFromDb.setAuthor(article.getAuthor()); }
if (article.getText() != null)   { articleFromDb.setText(article.getText()); }
articleFromDb.setPublished(article.isPublished());   // unconditional
```

**What happens:** Every other field is copied only when present, but `published` is copied unconditionally.
`Article.published` is a primitive `boolean`, so a PATCH body that omits it deserializes to `false` — retitling a
live article takes it off the site with no error and no indication.

**Confirmed by test:** `ArticleServiceTest.Updating.silentlyUnpublishesWhenThePatchOmitsThePublishedFlag`

**Suggested fix:** Change the field to `Boolean` and null-check it like the others, or drop the line entirely and
let `publishArticle` / `removeArticle` own that transition — they already do, and they maintain the admin pending
indication, which this path bypasses.

---

## 5. Signup without `userIndications` returns 500 — Medium

**Where:** `services/UserService.java:32`

```java
user.setUserIndications(new UserIndications(false, user.getUserIndications().getPreferredLanguage()));
```

**What happens:** A signup payload with no `userIndications` object throws `NullPointerException`. Nothing handles
it — `ControllerAdvice` only covers `SmallTalkException` and `NotFoundException` — so the client gets a bare 500.

**Confirmed by test:** `UserServiceTest.failsWithNullPointerWhenTheSignupOmitsUserIndications`

**Suggested fix:** Default the language when `userIndications` is absent, and/or add `@NotNull` validation on the
signup body so it fails as a 400 with a message.

---

## 6. Out-of-range request parameters return 500, not 400 — Medium

**Where:** `controllers/FixtureController.java:20,28,29` and `advices/ControllerAdvice.java`

**What happens:** The `@Min`/`@Max` constraints on `POST /fixtures` are enforced, but the resulting
`jakarta.validation.ConstraintViolationException` has no handler, so an out-of-range `matchDays` surfaces as a
bare 500 instead of a 400 explaining the bound. The validation works; only the reporting is wrong.

**Confirmed by test:** `FixtureControllerTest.enforcesTheWindowBoundsWithoutTranslatingTheFailure`

**Suggested fix:** Add a `@ExceptionHandler(ConstraintViolationException.class)` to `ControllerAdvice` returning a
400 `SmallTalkResponse`, consistent with the rest of the API.

---

## 7. Welcome emails can never be sent — Medium *(by inspection, not covered by a test)*

**Where:** `src/main/resources/application.properties:23`

```properties
email.api.key=%{EMAIL_API_KEY}
```

**What happens:** `%` instead of `$` means this is not a placeholder. Spring injects the literal string
`%{EMAIL_API_KEY}` into `EmailConfig.resendClient`, so every send is rejected. `ResendEmailService.sendWelcomeMail`
catches `ResendException` and only logs it, so signups appear to succeed and no welcome email ever arrives.

Not covered by a test because it is a configuration typo rather than a code path — the test properties define
their own value.

**Suggested fix:** `email.api.key=${EMAIL_API_KEY}`. Worth checking production logs for
`Failed to send welcome email` to see how long this has been silent.

---

## 8. One malformed league id aborts a whole refresh — Low

**Where:** `system/utils/mappers/StandingMapper.java:20` and `services/CompetitionDataService.java:22`

```java
.competition(Competition.fromCode(Integer.parseInt(standingsDtoItem.getLeagueId())))   // both can throw
.filter(competitionDto -> Competition.isValidCode(Integer.parseInt(competitionDto.getLeagueId())))
```

**What happens:** `Competition.fromCode` throws on an unknown code and `Integer.parseInt` throws on a non-numeric
one. Neither is guarded, so a single unexpected league id aborts the entire standings or competitions refresh.
Note the asymmetry: `CompetitionDataService` filters out unknown-but-numeric ids gracefully, yet throws on a
non-numeric one.

`FixtureAssembler.mapCompetition` does guard this — it catches `IllegalArgumentException`, which covers
`NumberFormatException` too — so the same input is tolerated in one place and fatal in another.

Low severity because both are currently fed one known competition at a time, so it is not reachable today.

**Confirmed by test:** `StandingMapperTest.throwsOnALeagueIdOutsideTheWhitelist`,
`CompetitionDataServiceTest.throwsOnANonNumericLeagueId`

**Suggested fix:** Reuse the tolerant approach from `FixtureAssembler.mapCompetition` and skip unmappable rows.

---

## 9. Builder-made `Fixture` cannot accept a one-liner — Low (latent)

**Where:** `domain/Fixture.java:15` (`@Builder`) and `:47`

```java
private Set<OneLiner> oneLiners = new HashSet<>();   // no @Builder.Default
```

**What happens:** Lombok's `@Builder` ignores field initialisers unless marked `@Builder.Default`, so
`Fixture.builder().build()` — which is how `FixtureAssembler` creates every fixture — starts with a `null` set.
Reading is safe because `getOneLiners()` guards for null, but `addOneLiner` throws `NullPointerException`.

Latent rather than live: `OneLinersService` only calls `addOneLiner` on fixtures loaded from MongoDB, where Spring
Data uses the no-arg constructor and the initialiser does run.

**Confirmed by test:** `FixtureTest.builderLeavesTheOneLinerSetNullSoAddingThrows`

**Suggested fix:** Add `@Builder.Default` to the field. Also worth checking `goals` and `statistics`, which have
the same shape but are always set explicitly by the assembler today.

---

## 10. Expired tokens throw instead of validating to `false` — Low

**Where:** `security/JwtUtil.java`, `isTokenValidated`

**What happens:** `isTokenValidated` reads like a predicate, but jjwt raises `ExpiredJwtException` during parsing,
so an expired token throws rather than returning `false`. `JwtAuthFilter` wraps the call in a catch-all and
answers 401, so today's behaviour is correct — but any new caller has to know to catch it.

**Confirmed by test:** `JwtUtilTest.throwsRatherThanReturningFalseForAnExpiredToken`

**Suggested fix:** Either catch `ExpiredJwtException` inside `isTokenValidated` and return `false`, or rename it
to something that does not read as a total predicate.

---

---

## 11. A player who leaves the tracked leagues is never deleted — Medium *(by inspection, not covered by a test)*

**Where:** `services/TeamDataService.java`, `savePlayers`, reached from `saveCompetitionTeams`

```java
BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, PlayerData.class);
players.forEach(player -> bulk.replaceOne(...upsert...));   // writes the current squad
bulk.execute();                                             // nothing removes anyone else
```

**What happens:** `playerData` only ever grows. The refresh writes whatever squad `get_teams` returns and never
reconciles that against the players already stored for the team, so a departed player keeps his `teamId` and his
final stats forever. `PlayerDataService.getNotablePlayers` selects on having played, which a stale document
still satisfies, so a player who left in the summer can be named in his old club's card and in the sentence the
model writes from it.

**Why it is only Medium:** a move *between* two tracked leagues corrects itself, because the new club's squad
rewrites the same `_id` with a new `teamId`. The damage is limited to players who leave the tracked leagues
entirely — a transfer abroad, a retirement, a release — and to a squad that shrinks between refreshes. It grows
with every transfer window rather than all at once.

**Not covered by a test.** `TeamDataServiceTest.SavingTeams` asserts what *is* written; nothing asserts what
should have been removed, because today nothing is.

**Suggested fix:** In `savePlayers`, collect the ids of the squad just written and add one
`bulk.remove(Query.query(where("teamId").is(teamId).and("_id").nin(writtenIds)))` to the same bulk. It rides
along with the write that is already there, stays atomic per team, and costs no extra round-trip. Careful with
the empty-squad case: a national team legitimately returns no players, and deleting on an empty response would
wipe a squad whenever the API has a bad day — so skip the removal, as the write is already skipped, rather than
treating "no players" as "everyone left".

## Security posture

These are deliberate design choices rather than accidental defects, so they are listed separately — but they are
worth a decision.

- **Passwords are stored and compared in plain text.** `services/UserService.java:48` looks users up with
  `findByEmailAndPassword(email, password)`, and `User.password` holds the raw value. A database leak is an
  immediate credential leak, and it rules out password reset done properly. Hashing with BCrypt would be a
  contained change: `UserService.addUser`, `UserService.login`, and a migration for existing rows.
- **The JWT signing key is a constant in source.** `security/JwtUtil.java:22` hard-codes the base64 secret, so it
  is in git history and identical across environments. Anyone with repository access can mint a valid token for
  any user. Move it to an environment variable like every other secret in `application.properties`.
- **There is no Spring Security.** Authorisation is a hand-maintained list of URI prefixes and HTTP methods in
  `JwtAuthFilter`. Bug #1 is the direct consequence: the rules are string comparisons with no compiler or
  framework checking that a new endpoint is covered. `JwtAuthFilterTest` now documents the full routing table, so
  any change to it is at least visible.
