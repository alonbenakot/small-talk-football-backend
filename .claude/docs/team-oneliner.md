# Team one-liner — implementation plan

A "team small talk" feature: given a team, return one or two sentences a user can say that make them sound
current on that team — league position, form, a notable player fact — **plus** the dry facts behind it so the
frontend can render a card: crest, coach, venue, standing, recent results, next match, notable players.

It mirrors the existing match one-liner but is keyed on a team rather than a fixture, and the sentence adapts to
who is speaking: a fan of the team, a fan of a rival, or a neutral.

> **Reading order:** the build order is immediately below — start there, then read only the sections the phase
> you're on refers to. Sections 1–11 are reference material, not a sequence to work through. The appendix holds a
> verified live `get_teams` response; trust it over the published apifootball documentation, which is wrong about
> that endpoint in both directions.
>
> Revised after review. Decisions taken: route lives on `OneLinerController`; bug #3 is fixed with
> `@JsonProperty`; three speaker perspectives; derived repository query; production runs a 30-day fixture window;
> no TTL — freshness is derived from the team's last match; WORLD_CUP-only teams are rejected; player data gets
> its own collection because the player one-liner is the next feature.

---

---

## Start here — build order, grouped into sessions

Ten steps in four phases. Each phase is independently verifiable and sized for one session — open a fresh session
per phase rather than carrying the whole feature in one context. The opening prompt is literally
*"Read `.claude/docs/team-oneliner.md` and implement phase N."*

**Every phase ships its own tests.** They are listed inline below, per step, and a phase is not finished until
they pass alongside the existing suite. Do not defer tests to a later phase — §11 is a reference index of the
same list, organised by component, not a separate stage of work. Test conventions live in `CLAUDE.md`: plain
JUnit/Mockito with no Spring context wherever possible, `@WebMvcTest` with `excludeFilters` for controllers, and
`JsonFixtures.parse` for anything deserialised from apifootball. Run `./mvnw test` (~15s, no Docker or network).

### Phase 1 — Groundwork (steps 1–2) — ✅ DONE (2026-09-09)

Small, self-contained, no new behaviour. First because everything after it depends on DTO binding actually
working.

1. Fix bug #3 from `bugs.md` with `@JsonProperty`, using the snake_case wire names verbatim (§7). Apply through
   `MatchDto`, `MatchLineup` and `LineUp`. Mark #3 fixed in `bugs.md`.
   - **Tests:** rewrite the four `FixtureAssemblerTest.UnboundFields` cases to assert correct binding instead of
     the pinned defect — they are *expected* to fail until you do, per the `CLAUDE.md` note on pinning tests. Add
     a `MatchDto` binding test through `JsonFixtures.parse`, so the `SNAKE_CASE`-plus-explicit-`@JsonProperty`
     interaction is genuinely exercised rather than assumed.
2. Extract `Language.getDescription()` and the `PromptPhrasing` helper (§8). Pure refactor.
   - **Tests:** none new. `PromptBuilderTest` already covers both call sites and must stay green untouched — if
     it needs editing, the refactor changed behaviour and went wrong.

**Done when:** `./mvnw test` is green, no test was deleted, and `FixtureAssembler` yields non-null team names and
coaches from a payload that carries them. ✅ All 297 tests green; see the Phase 1 handoff notes at the end of this document.

### Phase 2 — Data layer (steps 3–4) — ✅ DONE (2026-09-09)

No API exploration needed: a live `get_teams` response is in the appendix with the full field inventory and a
traps table. Write the DTOs from that, **not** from the published apifootball documentation, which is wrong about
this endpoint in both directions.

3. `TeamDataDto` gains `players` and the nested `venue` object; add `PlayerItem` and `VenueDto`; add the
   `PlayerData` document, its repository and mapper; write players in `saveCompetitionTeams` (§4.5, §4.6).
   - **Tests:** `PlayerDataMapperTest` — blank strings parse to null rather than throwing (a third of every squad
     is blank across all stats); non-numeric values tolerated; `player_injured` `"Yes"`/`"No"` becomes a boolean;
     an empty `players` array is normal for national teams, mirroring the existing empty-coaches case in
     `TeamDataUpdateMapperTest`. Extend `TeamDataUpdateMapperTest` for venue and founded. Add a `TeamDataDto`
     binding test via `JsonFixtures.parse` using a player object copied from the appendix.
4. `getTopScorers(Competition)` on `FootballApiService`, the `leagueScorerRank` backfill, and the notable-player
   ranking (§4.4, §6.4).
   - **Tests:** extend `FootballApiServiceTest` for `getTopScorers` against
     `MockRestServiceServer.bindTo(RestClient.Builder)`, with `ExpectedCount` if it iterates
     `Competition.values()`. Add a ranking test: exactly 10 returned; the first-choice goalkeeper is always among
     them, including when no keeper has appearances; zero-appearance outfielders excluded; the per-position cap
     holds; a high-volume midfielder places ahead of a fringe forward.

**Done when:** the suite is green and a real `POST /teams` against a live key populates `PlayerData` for every
tracked competition, with the ranking returning 10 players including the first-choice keeper.
✅ Suite green at 318 tests (+21). The live-key check is still owed — see the Phase 2 handoff notes at the end of
this document for what to verify.

### Phase 3 — The feature (steps 5–7) — ✅ DONE (2026-09-09)

The subtle phase. The traps here are ones tests catch only if you write them deliberately.

5. `Perspective` enum; `TeamOneLiner` on `TeamData` with `@Builder.Default`; `TeamOneLinerPromptBuilder`; the
   `PromptBuilderFactory` overload (§3, §5, §6).
   - **Tests:** `TeamOneLinerPromptBuilderTest` — one case per `Perspective` asserting the role and style text
     genuinely differ; the data block carries position, points, form, next fixture and notable players; a missing
     standing and an empty form list degrade to readable lines rather than NPEs; an empty notable-players list is
     handled. Extend `OneLinerTest`'s style for `TeamOneLiner`: equality keyed on language + competition +
     perspective while ignoring `text`.
6. The by-team fixture queries and their service methods (§4.2).
   - **Tests:** service-level with mocked repositories. Note the queries themselves have **no** integration
     coverage — there are no MongoDB tests in this project, and a mis-resolved nested path returns empty rather
     than failing, so verify them once against a real database before merging.
7. `TeamOneLinersService`, `TeamFacts`, `TeamSmallTalk`, the `GET /one-liners/teams/{teamId}` route, and turning
   `getTeamById`'s `IllegalStateException` into a `NotFoundException` (§2, §9).
   - **Tests:** `TeamOneLinersServiceTest` (Mockito) — cache hit when nothing changed; regeneration after a newer
     finished fixture; regeneration after a standings move with no new fixture; independent entries per
     perspective and per language; and specifically that `replaceOneLiner` overwrites, since `add` on a Set whose
     equality ignores `text` is a silent no-op (§5). Prompt-selection test: injured regular beats a higher
     scorer, league scorer rank beats raw goals, a merely-top-of-a-poor-squad scorer qualifies for nothing.
     Extend `OneLinerControllerTest` for the new route (`@WebMvcTest`, `excludeFilters` for `JwtAuthFilter`),
     covering a 400 for a WORLD_CUP-only team and a 404 for an unknown id. Add one `JwtAuthFilterTest` assertion
     that `GET /one-liners/teams/{id}` is public, so a future filter edit cannot silently gate it.

**Done when:** the suite is green and the endpoint returns a cached one-liner on the second call, a regenerated
one after a new finished fixture or a standings move, and a 400 for a WORLD_CUP-only team.
✅ Suite green at 389 tests (+71). The caching, regeneration and rejection behaviour is pinned in
`TeamOneLinersServiceTest`; the live-database check of the two derived queries is still owed, along with Phase 2's
live-key check. See the Phase 3 handoff notes at the end of this document.

### Phase 4 — Operations and tuning (steps 8–9) — ✅ DONE (2026-09-09)

8. ✅ `TeamsJob` on a twice-weekly cron for the squad refresh (§4.7 — planned daily, cut to Monday and Thursday
   after Phase 4; see the note below).
   - **Tests:** none beyond asserting the job delegates — the existing jobs carry no tests either, and a cron
     expression is not meaningfully testable here.
9. Manual smoke against a real database and a real OpenAI key. Read actual output for all three perspectives and
   tune `examples()` and `constraints()` from what comes back. **This is where the feature is made good** — the
   prompt text in §6 is a starting point, not a finished artefact. Budget real time for it.
   - **Tests:** none new, but any prompt change must leave `TeamOneLinerPromptBuilderTest` green. If a tuning
     change breaks it, decide deliberately whether the test or the prompt was wrong.

**Done when:** all three perspectives produce sentences you would actually say out loud.
✅ Run live against the real cluster, a real apifootball key and a real OpenAI key. All three perspectives
produce sentences worth saying, and the smoke closed every verification owed from Phases 2 and 3. It also turned
up two genuine defects — a venue side decided on team names, and multi-line model output — both fixed here with
regression tests. Suite green at **393 tests** (+4). See the Phase 4 handoff notes at the end of this document.

### Later

10. `get_news` by `team_id`, if the sentences need more than the table and the form (deferred; terms recorded in
    "Decisions taken"). Then the player one-liner, which `PlayerData` unblocks.


## 1. What already exists (and what we reuse)

| Concern | Existing code | Reuse |
|---|---|---|
| Prompt assembly | `PromptBuilder` (default `buildPrompt()` templating role/task/style/structure/constraints/examples/data) | Implement a third builder, no new template strings |
| Prompt selection | `PromptBuilderFactory.create(fixture, teamType, language)` | Add a team-flavoured overload |
| LLM call | `AiService.generate(String)` | As is |
| Cached one-liner | `OneLiner` (equals/hashCode on `teamType` + `language`, ignoring `text`), `Set<OneLiner>` on `Fixture` | Same pattern, new type keyed on language + competition + perspective |
| Team facts | `TeamData` (`name`, `coach`, `crest`, `Map<Competition, Standing>`), refreshed by `POST /teams` and `StandingsJob` | Primary data source |
| Recent form / next match | `Fixture` collection — a 30-day window in production | Sole source, no extra API call |
| External API | `FootballApiService` — every call through `ResponseHandler.process(...)`, never throws | Extend, preserve the contract |

`UpcomingFixtureOneLinerPromptBuilder` already phrases a standing and a recent-form list for a single team
(`phraseStanding`, `phraseRecentForm`). Those are the seed of the team builder and should be extracted rather
than copied a third time (see §8).

---

## 2. Endpoint

```
GET /one-liners/teams/{teamId}?lang=BRITISH&perspective=FAN[&competition=PREMIER_LEAGUE]
```

On the existing `OneLinerController`. No collision with `GET /one-liners/{fixtureId}` — different segment counts —
and `/one-liners` is not gated by `JwtAuthFilter`, so **no security change is needed**. That is the main reason
this route wins over `GET /teams/{teamId}/small-talk`: `isJwtRequiredTeams` currently gates *all* of `/teams` as
admin-only, and widening it would mean touching the hand-maintained routing table that `JwtAuthFilterTest` pins.

```java
@GetMapping("/teams/{teamId}")
@ResponseStatus(HttpStatus.OK)
public SmallTalkResponse<TeamSmallTalk> getTeamOneLiner(@PathVariable String teamId,
                                                        @RequestParam Language lang,
                                                        @RequestParam(defaultValue = "NEUTRAL") Perspective perspective,
                                                        @RequestParam(required = false) Competition competition)
```

Response — `SmallTalkResponse<TeamSmallTalk>`:

```jsonc
{
  "data": {
    "oneLiner": {
      "language": "BRITISH", "competition": "PREMIER_LEAGUE", "perspective": "FAN",
      "text": "Four on the bounce and top of the table — Saka's carrying them right now.",
      "generatedAt": "2026-09-09T10:02:11Z"
    },
    "facts": {
      "id": "2611", "name": "Arsenal", "crest": "https://.../arsenal.png",
      "coach": "Mikel Arteta", "founded": "1886",
      "venue": { "name": "Emirates Stadium", "city": "London", "capacity": "60704", "surface": "grass" },
      "primaryCompetition": "PREMIER_LEAGUE",
      "standings": {
        "PREMIER_LEAGUE": { "position": 1, "playedMatches": 5, "points": 13,
                            "overall": {"wins": 4, "draws": 1, "losses": 0}, "home": {...}, "away": {...} },
        "CHAMPIONS_LEAGUE": { ... }
      },
      "recentForm": [ { "competition": "PREMIER_LEAGUE", "date": "...", "opponent": "Chelsea",
                        "home": true, "score": "2-1", "result": "WIN" } ],
      "nextFixture": { "fixtureId": "...", "opponent": "Man City", "home": false, "kickOff": "..." },
      "notablePlayers": [ { "id": "...", "name": "Bukayo Saka", "number": "7", "position": "Midfielders",
                            "age": "24", "matchesPlayed": 5, "goals": 3, "assists": 2, "shotsTotal": 21,
                            "passesAccuracy": 84, "rating": "7.8", "injured": false,
                            "leagueScorerRank": 3 } ]   // 10 of them (§6.4)
    }
  },
  "systemMessage": {...},
  "statusCode": 200
}
```

`lang` is required, matching the match endpoint. `perspective` defaults to `NEUTRAL`. `competition` is optional —
see §6.3. `facts.standings` returns every competition we hold so the FE can render tabs without a second call,
while `primaryCompetition` tells it (and the prompt) which one the sentence is about.

---

## 3. Perspective — three speaker modes

New enum `enums/Perspective`:

```java
public enum Perspective { FAN, RIVAL_FAN, NEUTRAL }
```

The same facts, three voices. This is a prompt-level concern only — no extra data is fetched — and it maps
cleanly onto the builder's `role()` / `style()` / `examples()`:

| | role | style | example output |
|---|---|---|---|
| `FAN` | "You support {team} and you're talking about your club with friends." | partisan, optimistic, forgiving of bad results | *"Four on the bounce and top of the table — nobody's stopping us right now."* |
| `RIVAL_FAN` | "You support a rival club and you're winding up a {team} fan." | teasing, needling, seizes on any weakness in the data | *"Second is still second, and they've drawn three of five. Bottling it again."* |
| `NEUTRAL` | "You follow football closely and you're making conversation about {team}." | observational, even-handed, no allegiance | *"Arsenal are top on thirteen points, unbeaten but with three draws."* |

Notes:

- `RIVAL_FAN` needs the firmest `constraints()` — it is the mode most likely to invent a jibe about a transfer or
  a manager rumour that isn't in the data. Add an explicit "mock only what's in the data; if the data is all
  positive, be grudging rather than inventing a flaw" line, plus an example demonstrating exactly that case.
- We do **not** need to know *which* rival. Naming a specific rival club would require a rivalry map we don't
  have, and would let the model reach for stale training-data feuds. Keep it as an unnamed rival voice.
- `perspective` is part of the cache key (§5), so a team holds up to 3 × 3 × n one-liners (perspectives ×
  languages × competitions). That's a handful of short strings per team — no storage concern.

The existing match one-liner uses `TeamType` (HOME/AWAY) for its bias and is unaffected. Worth noting for later:
`Perspective` is the more expressive model, and the match endpoint could migrate to it eventually — out of scope
here.

---

## 4. Where the dry facts come from

### 4.1 Already in Mongo — free
`TeamData` gives name, coach, crest and the per-competition `Standing` (position, played, points, and
overall/home/away `WinLossDraw`). `StandingsJob` refreshes it three times a day.

### 4.2 Recent form and next fixture — our own `Fixture` collection
Confirmed: production runs `MAX_MATCH_DAYS=30`, not the 7 in the default (`application.properties:13` is
`${MAX_MATCH_DAYS:7}`; only the local/test default is 7). Thirty days back is 4–8 matches for a club playing
league plus cup — plenty for a form line. **No extra API call is needed for form**, and the `get_events`
`team_id` fallback from the first draft is dropped.

One caveat to keep in mind: `max.match.days` is also what `FixtureService.deleteOldFixtures` prunes against, so
the form depth is exactly the retention window. If that env var is ever lowered, this feature degrades quietly.
Worth a log line when a team returns fewer than two finished fixtures.

Repository query, in the derived-name style you prefer:

```java
List<Fixture> findByFinishedTrueAndHomeTeamIdOrFinishedTrueAndAwayTeamId(String homeTeamId, String awayTeamId, Sort sort);
```

Two things about that method name, both fine but non-obvious:

- Spring Data's `PartTree` splits on `Or` first and `And`s within each branch, so this parses as
  `(finished = true AND homeTeam.id = ?0) OR (finished = true AND awayTeam.id = ?1)` — the intended meaning.
  There is no parenthesis syntax, so repeating `FinishedTrue` in both branches is required, not redundant.
- `HomeTeamId` has no matching `homeTeamId` property on `Fixture`, so Spring Data falls back to the nested path
  `homeTeam.id`. That resolution is silent — a typo would produce an always-empty query rather than an error, so
  it needs the manual check noted in §10.

The service passes the same `teamId` as both arguments and sorts by `matchDateTime` descending, limit 5. The
"next fixture" is the mirror query with `FinishedFalse`, ascending, first result.

### 4.3 Squad and venue — already paid for, currently discarded
Verified against a live call (full response in the appendix): `get_teams` — which
`TeamDataService.saveCompetitionTeams` already calls once per competition — returns, alongside `coaches`:

- **`venue`: a nested object**, not the flat `venue_*` fields the documentation page implies —
  `{ venue_name, venue_address, venue_city, venue_capacity, venue_surface }`. The DTO needs a nested type.
- **`players`: an array of 42-field objects**, far richer than documented. Beyond the basics it carries
  `player_complete_name`, `player_is_captain`, `player_shots_total`, `player_key_passes`, `player_passes`,
  `player_passes_accuracy`, `player_tackles`, `player_interceptions`, `player_clearances`, `player_blocks`,
  `player_duels_total`, `player_duels_won`, `player_dribble_attempts`, `player_dribble_succ`,
  `player_fouls_committed`, `player_dispossesed` *(sic)*, `player_woordworks` *(sic)*, the four penalty counters,
  and — for keepers — `player_saves`, `player_inside_box_saves`, `player_goals_conceded`.
- **team level:** `team_key`, `team_name`, `team_country`, `team_founded`, `team_badge`.

`TeamDataDto` declares only `team_key`, `team_name`, `team_badge` and `coaches`, so every squad refresh throws
the rest away. Binding it costs one API call: zero.

**Four things the live response settles, all of which change the mapper:**

1. **`player_minutes` does not exist here.** The documentation lists it; the payload does not. Ranking works
   without it (§6.4).
2. **Unused players have `""`, not `"0"`, in every stat field.** Manchester City list 24 players and only 15 have
   any appearances at all; the other 9 are blank across the board. So the "has played" filter is
   `!matchesPlayed.isBlank()`, never `matchesPlayed == 0`, and every numeric parse must treat blank as absent
   rather than throwing.
3. **`player_passes_accuracy` is a count, not a percentage.** Ruben Dias: `player_passes: "478"`,
   `player_passes_accuracy: "454"`. It is accurate passes *completed*, so it must be used as a ratio against
   `player_passes` or not at all — reading it as a percentage would rank every high-volume passer at 400%+.
4. **`player_injured` is `"Yes"` / `"No"`**, a string, and it is populated (both values appear in the league).
   `player_country` and several others are frequently empty even for regulars, so no field should be assumed
   present.

### 4.4 Top scorers — cheap and genuinely "current"
`get_topscorers&league_id=` returns `player_place`, `player_name`, `player_key`, `team_name`, `team_key`, `goals`,
`assists`, `penalty_goals`. Filtering by `team_key` gives a fact that sounds like real knowledge — *"their striker
is third in the league scoring charts"* — for one call per competition, on the same cadence as `StandingsJob`.

Worth including in phase 1: it's the difference between a one-liner about the table and one about a person.
Store `leagueScorerRank`, `goals` and `assists` on the player record (§4.6).

### 4.5 There is no bulk player endpoint — squads come from `get_teams`

Worth stating plainly, because it is counter-intuitive: **apifootball has no way to fetch a squad by team.**
`get_players` takes `player_id` **or** `player_name` and nothing else — no `team_id` parameter. So the only route
to a squad is the `players` array embedded in the `get_teams` response we already call once per competition.

That means the ingestion path is:

```
saveCompetitionTeams()                       // existing, one get_teams call per Competition
  └─ for each TeamDataDto
       ├─ upsert TeamData      (existing, + venue and founded)
       └─ upsert PlayerData    (NEW — one document per entry in the dto's players array)
```

No new API call, no new job beyond the `TeamsJob` in §4.7. The saving of players lives inside
`TeamDataService.saveCompetitionTeams`, next to the team upsert that is already there.

> **Correction (2026-09-13):** an earlier revision of this section claimed `getPlayerById` / `getPlayersByName`
> and a `PlayerDto` had been added to `FootballApiService` for the player one-liner. **They were never built** —
> none of the three exists in the codebase. The player one-liner plan (`player-oneliner.md` §4.5) defers
> `get_players` and does not need them.

`get_players` is the only source of `player_birthdate`,
`player_country` and `player_minutes` — none of which `get_teams` returns. Minutes aren't needed for this
feature (§6.4 ranks without them), but a targeted per-player top-up is the route if the player one-liner wants
them, rather than a sweep of every squad.

### 4.6 Player data gets its own collection
Since the player one-liner is the next feature, embedding the squad inside `TeamData` would have to be undone
immediately — a player one-liner needs to look a player up by id, not scan every team document.

Plan: a new `@Document PlayerData` collection, written from the same `get_teams` response.

```java
@Document @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PlayerData {
    @Id private String id;            // player_id
    private String teamId;            // indexed — the by-team lookup this feature needs
    private String teamName;
    private String name;
    private String image;
    private String number;
    private String position;          // player_type: Goalkeepers / Defenders / Midfielders / Forwards
    private String age;
    private boolean captain;          // player_is_captain, "1" / "" 
    private Integer matchesPlayed;    // blank for unused players — Integer, not int
    private Integer goals;
    private Integer assists;
    private Integer shotsTotal;
    private Integer keyPasses;
    private Integer passes;
    private Integer passesAccurate;   // player_passes_accuracy — a COUNT, renamed to say so
    private Integer tackles;
    private Integer interceptions;
    private Integer clearances;
    private Integer duelsTotal;
    private Integer duelsWon;
    private Integer saves;            // keepers
    private Integer insideBoxSaves;   // keepers
    private Integer goalsConceded;    // keepers
    private Integer yellowCards;
    private Integer redCards;
    private boolean injured;
    private String rating;
    private Integer leagueScorerRank; // from get_topscorers, null when unranked
}
```

- `TeamData` keeps `venue` and `founded` but **not** the squad; the service joins via
  `PlayerDataRepository.findByTeamId(teamId)`.
- Written in `saveCompetitionTeams` with the same `MongoTemplate` upsert pattern already used for teams, so a
  refresh updates in place rather than churning ids.
- Note `spring.data.mongodb.auto-index-creation=false` in the test properties: an `@Indexed` on `teamId` is
  correct for production and inert in tests, which is exactly why that flag exists.
- Numeric fields arrive as strings from apifootball (every field in that API is a string). Parse them in the
  mapper, tolerantly — a blank `player_goals` must yield `0` or `null`, never a thrown `NumberFormatException`,
  because one bad row would otherwise abort a whole squad refresh (the shape of bug #8).

### 4.7 Squad freshness
`saveCompetitionTeams` runs only from the manual admin `POST /teams` — there is no job for it (`StandingsJob`
only refreshes standings). Squad stats (`player_goals`, `player_injured`, `player_rating`) change weekly, so
without a job the "notable players" data goes stale and the one-liners get subtly wrong.

Add `TeamsJob` alongside the other two in `services/jobs`, on a cron in `Asia/Jerusalem`, calling
`saveCompetitionTeams()`.

> **Superseded after Phase 4.** This was written as a daily job. It ships as **Monday and Thursday at 04:00**
> (`0 0 4 * * MON,THU`) — the rounds fall at the weekend and midweek, so those two runs pick up each of them, and
> it cuts the API spend by about 70% against daily. Injury data is then at most three or four days old, which is
> the freshness that actually matters: the one-liner names an injured regular ahead of every other fact.

### 4.8 Considered and deferred: `get_news`
`get_news` accepts `team_id` and returns `title`, `content`, `published_at`, `sources` — on paper the ideal source
for "important relevant facts". Deferred to phase 2 for two reasons: it is untrusted third-party text flowing
straight into an LLM prompt (headline-only, truncated, and explicitly framed as data would be the minimum
handling), and it changes the feature's character from "reads the table" to "reads the press". Flagged as an open
question rather than dropped — it is the single highest-value addition after this ships.

---

## 5. Caching and freshness (no TTL)

**What TTL meant:** "time to live" — cache the sentence for N hours, then regenerate regardless. It's the blunt
version. Your instinct is better and it's what the plan now does: compare against the team's last match.

Rule: a cached one-liner is **stale** when either
1. the team has a finished fixture whose `matchDateTime` is after `generatedAt` — the team has played since; or
2. the stored standings snapshot no longer matches the current one — position or points moved.

(2) matters because a team's league position changes when *other* teams play. Without it, a team in a two-week
international break keeps a one-liner saying "top of the table" after being overtaken. It costs one small
comparison and no new configuration — which is why there is no `team.oneliner.ttl` property, and therefore
nothing to add to the shadowing `src/test/resources/application.properties`.

```java
@Getter @Builder @AllArgsConstructor
public class TeamOneLiner {
    Language language;
    Competition competition;
    Perspective perspective;
    String text;
    Instant generatedAt;
    Integer positionAtGeneration;   // standings snapshot
    Integer pointsAtGeneration;
    // equals/hashCode on language + competition + perspective only — mirrors OneLiner ignoring `text`
}
```

`TeamData` gains `Set<TeamOneLiner> oneLiners` with the same null-safe getter / `addOneLiner` / `replaceOneLiner`
trio `Fixture` has. **Add `@Builder.Default`** — `TeamData` is `@Builder`, and without it the set is null and
`addOneLiner` NPEs, which is bug #9 verbatim.

Regeneration uses `replaceOneLiner`, not `addOneLiner`: `add` on a `Set` whose equality ignores `text` is a no-op
when an entry already exists, so a stale sentence would never be overwritten.

---

## 6. Prompt design

### 6.1 New builder
`system/utils/prompts/TeamOneLinerPromptBuilder implements PromptBuilder` — implement the seven methods, never a
new template string (per `CLAUDE.md`).

- **role** — per perspective, per the table in §3.
- **task** — "Generate a casual comment that shows you follow this team closely at the moment."
- **style** — `Language.getDescription()` + the perspective's tone.
- **structure** — "1-2 sentences, under 20 words each, no line breaks, no emojis." (matches the other two.)
- **constraints** — "Use only the data provided. No predictions, no invented transfers, injuries, quotes or
  statistics. Do not mention anything you know about this club that is not listed below." Stale training-data
  knowledge is the dominant failure mode here — the model has opinions about every big club that may be years
  out of date — so this needs to be firmer than in the fixture builders. Plus the `RIVAL_FAN` clause from §3.
- **examples** — 3–4 per perspective, in the house voice. Pick them to demonstrate the *edge*: a good run seen by
  a rival, a bad run seen by a fan.
- **data** — competition, position, points, W/D/L, home/away split; recent form list; next fixture with opponent
  and venue side; coach; and 2–3 notable players with their goals/assists/league scorer rank/injury status.

### 6.2 Factory
Add an overload to `PromptBuilderFactory` (it already injects `TeamDataService` and `FootballApiService`), taking
a small context object rather than seven positional arguments:

```java
public PromptBuilder create(TeamPromptContext context, Language language, Perspective perspective)
// TeamPromptContext: TeamData team, Competition competition, List<Fixture> recentForm,
//                    Fixture nextFixture, List<PlayerData> notablePlayers
```

Keeping one factory matches the `CLAUDE.md` pointer that prompt variants are added there.

### 6.3 Competition selection
`TeamData.standings` may hold several entries. Rule: use the request's `competition` when given and present;
otherwise the team's domestic league — the single entry that is neither `CHAMPIONS_LEAGUE` nor `WORLD_CUP`; if
still ambiguous, the one with the most `playedMatches`. Return the choice as `facts.primaryCompetition`.

### 6.4 Notable players — 10 for the card, 0–3 for the prompt

One list, not two. `facts.notablePlayers` returns **10 players**, and the prompt draws from those same 10. There
is no positional XI: we hold no lineup data outside a specific fixture, so a 1/4/4/2 list would have implied a
starting eleven we can't actually know. "Notable players" claims only what it can support.

**Ranking the 10.** `player_minutes` isn't in the `get_teams` response (§4.3), so regularity comes from
`player_match_played` and the supporting stats do the rest:

1. Drop anyone whose `matchesPlayed` is blank — an unused squad member is not notable. Note this is a blank
   string, not a zero (§4.3), and roughly a third of a listed squad is blank.
2. Score the rest on appearances relative to the squad's maximum, plus goal contribution
   (`goals * 2 + assists`), plus a volume term. For the volume term use `passes` (raw involvement) and
   `duelsWon`, **not** `passesAccuracy` as a percentage — it is a count of completed passes, so it only means
   anything as `passesAccuracy / passes` (§4.3). `shotsTotal` works as given. This term is what lets a
   defensive midfielder or a centre-back place at all; without it the list is forwards and nothing else.
3. **Reserve one slot for the first-choice goalkeeper** — the `Goalkeepers` entry with the most appearances,
   included unconditionally, ranked or not. A keeper scores near zero on every attacking signal and would never
   survive the ranking on merit, but "who's in goal" is basic knowledge a fan is expected to have, and the
   backup keeper appearing instead of the first choice would read as a mistake. If no keeper has appearances
   (early season, or a thin squad listing), take the first `Goalkeepers` entry rather than leaving the slot
   empty. The remaining 9 come from the ranking.

   Better than expected: the live response gives keepers **`player_saves`, `player_inside_box_saves` and
   `player_goals_conceded`**, so the reserved slot can carry a real fact — "21 saves in nine games" — rather than
   just a name. Store those three on `PlayerData` and let the prompt use them when the keeper qualifies.
4. Guard the rest loosely: at most 4 from any one `player_type` bucket. Enough to stop the list degenerating
   into nine attackers, without pretending to be a formation.
5. `injured == true` does not remove a player — an injured regular is *more* notable, not less. Flag it in the
   payload and let the FE render it.

**The prompt gets 0–3 of those 10, and only when justified.** A name with no story attached makes the sentence
worse, not better, so a player reaches the prompt only if:

1. **Injured regular** — `injured == true` with appearances at or above the squad median. The most
   conversation-worthy fact a squad holds, and the one a casual fan won't know.
2. **League scorer rank** — a non-null `leagueScorerRank` from `get_topscorers` (§4.4), best rank first.
3. **Standout contribution** — `goals * 2 + assists` clearly ahead of the squad, not merely top of it.
   Concretely: at least 1.5× the second-best player's score, or better than roughly one goal contribution every
   two appearances. A team whose leading scorer has three in twenty is not carrying anyone, and saying so would
   be exactly the empty stat that `constraints()` exists to suppress.

Take them in that order, stop at 3, and pass **zero** when nothing qualifies — the builder must handle an empty
list and fall back to a table-and-form sentence.

### 6.5 WORLD_CUP-only teams — rejected
Decided: if the resolved primary competition is `WORLD_CUP`, or the team holds no standings outside `WORLD_CUP`,
throw a `SmallTalkException` (400) with a message constant in `system/messages/Messages`. League position is
meaningless for a national side, and the alternative is a confidently wrong sentence. A club team that also has a
`WORLD_CUP` entry is unaffected — §6.3 skips it when choosing the primary competition.

---

## 7. Bug #3 — fixed with `@JsonProperty`

Agreed, and it is a prerequisite rather than a nicety: the new `TeamDataDto` fields land in the same hand-written
DTO where getter names already drift from field names, so the same trap is one typo away.

One detail that decides whether the fix works: the apifootball mapper is the `@Qualifier("apiClient")` one with
`SNAKE_CASE` naming, and **Jackson does not apply a naming strategy to an explicitly named property**. So the
annotation must carry the wire name verbatim, not the camelCase one:

```java
@JsonProperty("match_hometeam_name")   // correct
private String matchHometeamName;

@JsonProperty("matchHometeamName")     // wrong — would silently stay null, same as today
private String matchHometeamName;
```

Apply to `MatchDto.matchHometeamName`, `matchAwayteamName`, `lineup`, and through `MatchLineup.home`/`away` and
`LineUp.coach`, which have the same shape at every level.

Consequence worth expecting: `TeamDataService.enrichTeamsData` currently *backfills* names and coaches that never
deserialize. Once binding works it becomes a genuine fallback, and `FixtureAssembler.getCoach()` stops being dead
code. `FixtureAssemblerTest.UnboundFields` pins the broken behaviour deliberately — those four cases must be
rewritten to assert the correct behaviour, per the `CLAUDE.md` note about pinning tests. Update `bugs.md` to mark
#3 fixed.

---

## 8. Refactors this feature should carry (small, in scope)

1. **Language description** — `switch (language) { HEBREW -> "Hebrew"; ... }` is copy-pasted in both existing
   builders and a third copy is imminent. Move it onto the `Language` enum as `getDescription()`.
2. **Standing / recent-form phrasing** — extract `phraseStanding` and `phraseRecentForm` from
   `UpcomingFixtureOneLinerPromptBuilder` into a package-private `PromptPhrasing` helper and call from both.

Both are pure moves covered by `PromptBuilderTest`, so they're cheap and stop the duplication from tripling.

---

## 9. Service flow

New `TeamOneLinersService` — a separate service from `OneLinersService`, since the caching rule differs entirely.

```
getTeamSmallTalk(teamId, competition, lang, perspective)
  ├─ TeamData team   = teamDataService.getTeamById(teamId)      // → NotFoundException, see §10
  ├─ Competition target = resolveCompetition(team, competition) // rejects WORLD_CUP-only (§6.5)
  ├─ List<Fixture> form = fixtureService.getRecentFinishedForTeam(teamId, 5)
  ├─ Fixture next       = fixtureService.getNextFixtureForTeam(teamId)
  ├─ List<PlayerData> notable = playerDataService.getNotablePlayers(teamId, 5)   // §6.4
  ├─ TeamOneLiner cached = team.findOneLiner(lang, target, perspective)
  │     ├─ fresh (§5)? → use it
  │     └─ stale?      → aiService.generate(factory.create(context, lang, perspective).buildPrompt()),
  │                      team.replaceOneLiner(new), teamDataService.save(team)
  └─ return new TeamSmallTalk(oneLiner, TeamFacts.from(team, target, form, next, notable))
```

`TeamFacts` and `TeamSmallTalk` live in `models/` as plain response shapes, serialised by the `@Primary`
`LOWER_CAMEL_CASE` mapper. Do **not** return `TeamData` directly — the cached one-liner set would leak into the
response body.

---

## 10. Files touched

**New**
```
enums/Perspective.java
domain/PlayerData.java
repositories/PlayerDataRepository.java
services/PlayerDataService.java
services/TeamOneLinersService.java
services/jobs/TeamsJob.java
models/TeamOneLiner.java
models/TeamFacts.java
models/TeamSmallTalk.java
models/Venue.java
models/dto/PlayerItem.java                        (the players array inside get_teams)
models/dto/VenueDto.java                          (venue IS a nested object — see appendix)
system/utils/mappers/PlayerDataMapper.java
system/utils/prompts/TeamOneLinerPromptBuilder.java
system/utils/prompts/TeamPromptContext.java
system/utils/prompts/PromptPhrasing.java
```

**Modified**
```
controllers/OneLinerController.java              + GET /one-liners/teams/{teamId}
domain/TeamData.java                             + venue, founded, Set<TeamOneLiner> (with @Builder.Default)
models/dto/TeamDataDto.java                      + players, venue (nested), team_founded, team_country
models/dto/MatchDto.java, MatchLineup.java, LineUp.java   @JsonProperty fix for bug #3
system/utils/mappers/TeamDataUpdateMapper.java   + venue, founded
services/TeamDataService.java                    + save(TeamData), write PlayerData in saveCompetitionTeams
services/FootballApiService.java                 + getTopScorers(Competition)
repositories/FixtureRepository.java              + the derived by-team queries (§4.2)
services/FixtureService.java                     + getRecentFinishedForTeam / getNextFixtureForTeam
system/utils/prompts/PromptBuilderFactory.java   + team overload
system/utils/prompts/*OneLinerPromptBuilder.java + Language.getDescription() / PromptPhrasing
enums/Language.java                              + getDescription()
system/messages/Messages.java                    + unknown team, world-cup-only team
bugs.md                                          mark #3 fixed
```

No change to `JwtAuthFilter` and no new properties — so `src/test/resources/application.properties` is untouched
too.

---

## 11. Tests — reference index

**This is not a phase of work.** Every test below is assigned to the step that creates the code it covers, in the
build order at the top of this document; write it there. This section exists so you can look up what covers a
given component, and so nothing is dropped when a phase is split differently.

Conventions from `CLAUDE.md`: plain JUnit/Mockito wherever a Spring context isn't needed.

- *(phase 3, step 5)* `TeamOneLinerPromptBuilderTest` — one case per `Perspective` asserting the role/style text differs; assert the
  data block carries position, points, form, next fixture and notable players; assert a missing standing and an
  empty form list degrade to readable lines rather than NPEs.
- *(phase 3, step 7)* `TeamOneLinersServiceTest` (Mockito) — cache hit when nothing changed; regeneration after a newer finished
  fixture; regeneration after a standings move with no new fixture; independent cache entries per perspective and
  per language; `replaceOneLiner` actually overwrites (the `add`-is-a-no-op trap in §5).
- *(phase 2, step 3)* `PlayerDataMapperTest` — string→int parsing including blanks and non-numeric values; `player_injured`
  truthiness; empty `players` array (normal for national teams, mirroring the existing empty-coaches case).
- *(phase 2, step 4)* Notable-player ranking test — 10 returned; the first-choice goalkeeper is always among them, including when
  no keeper has appearances; zero-appearance outfielders excluded; the bucket cap holds; a high-passing
  midfielder places ahead of a fringe forward.
- *(phase 3, step 7)* Prompt-selection test — injured regular beats a higher scorer; league scorer rank beats raw goals; a
  merely-top-of-a-poor-squad scorer qualifies for nothing, and the builder handles the resulting empty list.
- *(phase 1 step 1 for `MatchDto`, phase 2 step 3 for `TeamDataDto`)* binding tests via `JsonFixtures.parse` (the production `apiClientObjectMapper`, so
  the `SNAKE_CASE` + explicit-`@JsonProperty` interaction from §7 is genuinely exercised).
- *(phase 1, step 1)* `FixtureAssemblerTest.UnboundFields` — rewrite the four pinning cases to assert correct binding.
- *(phase 2, step 4)* `FootballApiServiceTest` — `getTopScorers` against `MockRestServiceServer.bindTo(RestClient.Builder)`; use
  `ExpectedCount` if it iterates `Competition.values()`.
- *(phase 3, step 7)* `OneLinerControllerTest` — extend with the new route; `@WebMvcTest` with `excludeFilters` for `JwtAuthFilter`,
  as the existing controller tests do.
- *(phase 3, step 7)* `JwtAuthFilterTest` — **no change needed**, but add one assertion that `GET /one-liners/teams/{id}` is public,
  so a future filter edit can't silently gate it.

The derived queries in §4.2 have no integration coverage — there are no MongoDB tests in the project — and a bad
nested-path resolution returns empty rather than failing. Run them once against a real database before merging.

Separately: `TeamDataService.getTeamById` throws `IllegalStateException` for an unknown id, which
`ControllerAdvice` does not handle → bare 500. `teamId` is now user-supplied, making that reachable from outside
for the first time, so it must become a `NotFoundException` (404) as part of this work.

---

## Decisions taken (previously open)

1. **`RIVAL_FAN` against a mid-table side** — accept blander output. No fallback to `NEUTRAL`; the mode stays the
   mode, and a needling sentence about a team with nothing wrong with it is simply a mild one. This keeps the
   perspective honest rather than silently swapping voices behind the user's back, and it removes a threshold
   nobody would be able to tune.
2. **API budget** — hundreds of calls a day available. The twice-weekly `TeamsJob` (7 `get_teams`) plus `get_topscorers`
   (7) on the standings cadence is comfortably inside that alongside `FixturesJob` and `StandingsJob`.
3. **Player id stability** — assumed stable, consistent with fixture and team ids in this API. `PlayerData` is
   keyed on `player_id` with no composite fallback.
4. **Squad size** — one list of 10 notable players for the card, one slot of which is always the first-choice
   goalkeeper; 0–3 of those reach the prompt, and only when they qualify (§6.4). No positional XI, so the card
   makes no lineup claim it can't support.
5. **`player_minutes`** — confirmed absent from `get_teams`. Ranking uses `player_match_played` with
   `player_shots_total` and `player_passes_accuracy` as supporting signals, which also lets non-forwards place.
6. **`get_news`** — deferred, not rejected. Something to come back to once the feature is live and we can see
   whether the table-and-form sentences are carrying it on their own. When we do: headlines only, 3 most recent,
   last 7 days, framed to the model as untrusted third-party text.

## Still open

Nothing blocking. The one thing to watch during step 3 is that the `players` array in `get_teams` is richer than
the published documentation — bind it from a real dump, and if a field turns out to be a percentage string, an
empty string, or absent for some leagues, the mapper has to tolerate it rather than throw (§4.6).


---

## Appendix — live `get_teams` response (Premier League, fetched 2026-09-09)

Kept verbatim because the published documentation is wrong about this endpoint in both directions: it lists
`player_minutes`, which is absent, and omits roughly twenty fields that are present. Write the DTOs from this,
not from the docs page.

`GET https://apiv3.apifootball.com/?action=get_teams&league_id=152&APIkey=...` → a 710KB array, one object per
team. Top-level keys: `team_key`, `team_name`, `team_country`, `team_founded`, `team_badge`, `venue`, `players`,
`coaches`.

```jsonc
{
  "team_key": "80",
  "team_name": "Manchester City",
  "team_country": "England",
  "team_founded": "1880",
  "team_badge": "https://apiv3.apifootball.com/badges/80_manchester-city.jpg",
  "venue": {
    "venue_name": "Etihad Stadium",
    "venue_address": "Rowsley Street",
    "venue_city": "Manchester",
    "venue_capacity": "55097",
    "venue_surface": "grass"
  },
  "coaches": [ { "coach_name": "Enzo Maresca", "coach_country": "", "coach_age": "" } ],
  "players": [ /* 24 entries; 15 have appearances, 9 are blank across every stat */ ]
}
```

### An outfield player with appearances

```json
{
  "player_key": 659972248, "player_id": "659972248",
  "player_image": "https://apiv3.apifootball.com/badges/players/68451_e-haaland.jpg",
  "player_name": "Erling Haaland", "player_complete_name": "Erling Haaland",
  "player_number": "9", "player_country": "", "player_type": "Forwards",
  "player_age": "26", "player_birthdate": "2000-07-21", "player_is_captain": "1",
  "player_match_played": "10", "player_goals": "8", "player_assists": "0",
  "player_yellow_cards": "0", "player_red_cards": "0", "player_injured": "No",
  "player_substitute_out": "", "player_substitutes_on_bench": "",
  "player_shots_total": "33", "player_goals_conceded": "0",
  "player_fouls_committed": "5", "player_tackles": "1", "player_blocks": "1",
  "player_crosses_total": "", "player_interceptions": "", "player_clearances": "3",
  "player_dispossesed": "", "player_saves": "", "player_inside_box_saves": "",
  "player_duels_total": "29", "player_duels_won": "17",
  "player_dribble_attempts": "1", "player_dribble_succ": "1",
  "player_pen_comm": "", "player_pen_won": "", "player_pen_scored": "1", "player_pen_missed": "0",
  "player_passes": "86", "player_passes_accuracy": "54", "player_key_passes": "4",
  "player_woordworks": "", "player_rating": "7.30"
}
```

### A first-choice goalkeeper — note `player_saves` and `player_goals_conceded`

```json
{
  "player_key": 1425703506, "player_id": "1425703506",
  "player_name": "Gianluigi Donnarumma", "player_number": "1", "player_type": "Goalkeepers",
  "player_age": "27", "player_birthdate": "1999-02-25", "player_is_captain": "0",
  "player_match_played": "9", "player_goals": "", "player_assists": "0",
  "player_yellow_cards": "2", "player_red_cards": "0", "player_injured": "No",
  "player_goals_conceded": "12", "player_saves": "21", "player_inside_box_saves": "17",
  "player_clearances": "7", "player_tackles": "1", "player_fouls_committed": "1",
  "player_duels_total": "7", "player_duels_won": "5",
  "player_pen_scored": "0", "player_pen_missed": "0",
  "player_passes": "200", "player_passes_accuracy": "164", "player_key_passes": "1",
  "player_rating": "7.12"
}
```

### An unused squad player — every stat is `""`

```json
{
  "player_key": 3907863339, "player_id": "3907863339",
  "player_name": "Geronimo Rulli", "player_number": "28", "player_type": "Goalkeepers",
  "player_age": "34", "player_birthdate": "1992-05-20", "player_injured": "No",
  "player_match_played": "", "player_goals": "", "player_assists": "",
  "player_yellow_cards": "", "player_red_cards": "", "player_is_captain": "",
  "player_saves": "", "player_goals_conceded": "", "player_passes": "",
  "player_passes_accuracy": "", "player_rating": ""
}
```

### Full `player_*` key inventory (union across every Premier League team)

```
player_age                player_dribble_succ        player_key_passes         player_red_cards
player_assists            player_duels_total         player_match_played       player_saves
player_birthdate          player_duels_won           player_name               player_shots_total
player_blocks             player_fouls_committed     player_number             player_substitute_out
player_clearances         player_goals               player_passes             player_substitutes_on_bench
player_complete_name      player_goals_conceded      player_passes_accuracy    player_tackles
player_country            player_id                  player_pen_comm           player_type
player_crosses_total      player_image               player_pen_missed         player_woordworks
player_dispossesed        player_injured             player_pen_scored         player_yellow_cards
player_dribble_attempts   player_inside_box_saves    player_pen_won
player_interceptions      player_is_captain          player_rating
player_key
```

Two of those are misspelled upstream — `player_dispossesed` and `player_woordworks` — so bind them verbatim and
do not "correct" them.

### Traps this response reveals

| Observation | Consequence |
|---|---|
| `player_minutes` absent despite being documented | Ranking uses `player_match_played` (§6.4) |
| Stats are `""` for unused players, not `"0"` | Filter on blank, and parse blank as `null`; `Integer`, never `int` |
| `player_passes_accuracy` is a count (454 of 478), not a % | Use as a ratio against `player_passes`, or not at all |
| `player_key` is a JSON **number**, `player_id` a string | Bind `player_key` as `Long` or ignore it; key `PlayerData` on `player_id` |
| `venue` is nested; `coaches` is an array | Two DTO types, not flat fields |
| `player_country` blank even for internationals | Never assume a field is populated |
| `player_injured` is `"Yes"` / `"No"` | Parse to boolean explicitly |
| Numeric-looking values are all strings | Every parse goes through a tolerant helper (§4.6) |


---

## Phase 1 handoff notes (for the Phase 2 session)

Phase 1 is complete (committed as `ce0ff5f` / `17bff41`; the "not yet committed" note originally here was
written before the commit). Summary of what changed and
what the next session should know.

### Step 1 — bug #3 fixed

`@JsonProperty` with the **verbatim snake_case wire name** was added to:

- `MatchDto.matchHometeamName` → `@JsonProperty("match_hometeam_name")`
- `MatchDto.matchAwayteamName` → `@JsonProperty("match_awayteam_name")`
- `MatchDto.lineup` → `@JsonProperty("lineup")`
- `MatchLineup.home` / `MatchLineup.away` → `@JsonProperty("home")` / `@JsonProperty("away")`
- `LineUp.coach` → `@JsonProperty("coach")`

`CoachItem.lineupPlayer` was left alone — its getter (`getLineupPlayer`) already matches the field name, so it
binds without help. The rule confirmed in practice: with the `SNAKE_CASE` strategy, an explicit `@JsonProperty`
value is **not** run through the strategy, so it must be the literal wire key. **When you add `players` and
`venue` to `TeamDataDto` in Phase 2 step 3, annotate every new field the same way** — that DTO has the same
getter/field drift and `TeamDataDto.getCoaches()` is already an example of it.

Consequence now live: `TeamDataService.enrichTeamsData` is a genuine fallback rather than load-bearing, and
`FixtureAssembler.getCoach()` is no longer dead code. No behavioural regression — enrichment still fills gaps and
still does not overwrite values the feed supplied.

Tests:
- `FixtureAssemblerTest.UnboundFields` was renamed to `TeamNameAndCoachBinding` and its four cases now assert
  correct binding (names bind, coach binds, winner is derived from the bound name, missing lineup still
  tolerated).
- New `com.smalltalk.SmallTalkFootball.models.dto.MatchDtoTest` exercises the deserialization directly through
  `JsonFixtures.parse` — team names, the full lineup subtree, and a regression check on already-working fields.
- One stale Javadoc sentence in `TeamDataServiceTest.Enrichment` was corrected (it claimed the assembler "cannot"
  read names).
- `bugs.md` row 3 and section 3 are marked fixed with a note on the approach.

### Step 2 — refactors

- `Language` enum gained a `description` field and `getDescription()` (`BRITISH` → "British English",
  `AMERICAN` → "American English", `HEBREW` → "Hebrew"). Both existing builders now call
  `language.getDescription()` and their private `getLanguageDescription()` switch methods are gone.
- New package-private `system/utils/prompts/PromptPhrasing` holds `phraseStanding(teamName, teamData, competition)`
  and `phraseRecentForm(List<Fixture>)`, extracted verbatim from `UpcomingFixtureOneLinerPromptBuilder`. Note the
  signature change: `phraseStanding` now takes `Competition` explicitly rather than reading it off a `fixture`
  field, so the Phase 3 `TeamOneLinerPromptBuilder` can call it without a fixture in hand.
- `UpcomingFixtureOneLinerPromptBuilder.phraseHeadToHead` was **left in place** — it is byte-identical to
  `phraseRecentForm` but semantically distinct, and the plan only asked for the two named helpers. If Phase 3
  wants it, point it at `PromptPhrasing.phraseRecentForm` and delete the private copy.
- `PromptBuilderTest` was not touched and stays green, confirming the refactor changed no output.

### Where Phase 2 starts

Step 3: `TeamDataDto` gains `players` (array) and a nested `venue` object; add `PlayerItem` and `VenueDto`; add
the `PlayerData` document + repository + mapper; write players in `saveCompetitionTeams`. Read §4.5, §4.6 and the
appendix (the live `get_teams` response and its traps table) — **not** the published apifootball docs. Apply
`@JsonProperty` to every new `TeamDataDto` field per the rule above.


---

## Phase 2 handoff notes (for the Phase 3 session)

Phase 2 is complete. `./mvnw test` is green at **318 tests** (was 297; +21). Nothing here has been committed to
git yet. Summary of what changed and what Phase 3 needs to know.

### Step 3 — DTOs, the `PlayerData` collection, and the squad write

**New wire DTOs** (`models/dto/`), all bound through the `@Qualifier("apiClient")` `SNAKE_CASE` mapper:

- `PlayerItem` — one entry of the `players` array in `get_teams`. `@Getter` lombok, and **every field carries a
  verbatim `@JsonProperty`** (`player_match_played`, `player_is_captain`, `player_passes_accuracy`, …) per the
  Phase 1 rule that the SNAKE_CASE strategy does not re-process an explicit `@JsonProperty` value. Only the fields
  this feature needs are bound; the misspelled upstream keys (`player_dispossesed`, `player_woordworks`) are
  deliberately omitted. `player_key` (a JSON number) is not bound — we key on `player_id`.
- `VenueDto` — the nested `venue` object (`venue_name`, `venue_address`, `venue_city`, `venue_capacity`,
  `venue_surface`), again with verbatim `@JsonProperty` on each field.
- `TopScorerItem` — one row of `get_topscorers` (`player_place`, `player_name`, `player_key`, `team_name`,
  `team_key`, `goals`, `assists`, `penalty_goals`). Plain `@Getter`; SNAKE_CASE handles all of these, no
  `@JsonProperty` needed.

**`TeamDataDto`** gained `teamFounded` (`@JsonProperty("team_founded")`), `teamCountry` (`"team_country"`),
`venue` (`VenueDto`), and `players` (`List<PlayerItem>`) — hand-written getters to match the file's existing
style, each field annotated verbatim.

**Domain:**

- `models/Venue` — new plain value type (`name`, `address`, `city`, `capacity`, `surface`; capacity kept as a
  String, it is display-only). `@Data @Builder @NoArgsConstructor @AllArgsConstructor`.
- `domain/TeamData` gained `String founded` and `Venue venue`. **It did *not* gain `Set<TeamOneLiner> oneLiners`
  yet — that is Phase 3 step 5**, and it needs `@Builder.Default` (bug #9) when you add it.
- `domain/PlayerData` — new `@Document`. `@Id` is `player_id`; `teamId` is `@Indexed` (correct for prod, inert
  under the test flag). Every numeric field is a nullable `Integer` because unused squad members send `""`, not
  `"0"`. Keeper stats (`saves`, `insideBoxSaves`, `goalsConceded`) are on it per §6.4 step 3. `passesAccurate` is
  named to flag that it is a *count* of completed passes, not a percentage.

**Mapper:** `system/utils/mappers/PlayerDataMapper implements Mapper<TeamDataDto, List<PlayerData>>`,
`@Qualifier("playerDataMapper")`. Takes the whole `TeamDataDto` so it can carry `teamKey`/`teamName` down onto
each player. Blank/non-numeric → `null` (never throws); `player_injured` `"Yes"`/`"No"` → boolean;
`player_is_captain` `"1"` → boolean; empty/absent `players` → empty list. `leagueScorerRank` is left null here.

**Repository:** `PlayerDataRepository extends MongoRepository<PlayerData, String>` with
`List<PlayerData> findByTeamId(String)`.

**`TeamDataService`:**

- Constructor gained a sixth arg, `@Qualifier("playerDataMapper") Mapper<TeamDataDto, List<PlayerData>>`. Any new
  test constructing the service directly must pass it (the existing `TeamDataServiceTest` was updated).
- `saveCompetitionTeams()` now, per competition: fetches `get_topscorers` once, builds a
  `player_id → leagueScorerRank` map, then for each team upserts the `TeamData` (as before, now also writing
  `founded` and `venue`) **and** `mongoTemplate.save(player)` for every mapped `PlayerData`, stamping the rank
  from the map. `save` (full replace by `_id`) is deliberate — the rank is re-derived on every run, so there is
  nothing to preserve across a refresh, unlike the `setOnInsert("standings", …)` on the team upsert.
- New `save(TeamData)` method (delegates to `repository.save`) — added now because Phase 3's caching needs it.
- `getTeamById` still throws `IllegalStateException` for an unknown id. **Phase 3 step 7 turns that into a
  `NotFoundException`** — it was left alone here because nothing user-facing reaches it until the new route lands.

### Step 4 — top scorers and the notable-player ranking

- `FootballApiService.getTopScorers(Competition)` — `action=get_topscorers&league_id=`, one request per call,
  same `ResponseHandler.process` contract (empty list on any failure). It does **not** iterate
  `Competition.values()` itself; `saveCompetitionTeams` calls it once per competition in its existing loop.
- The `leagueScorerRank` backfill matches `TopScorerItem.player_key` directly against
  `PlayerData` `_id` (= `get_teams` `player_id`). The traps table says these are the same identifier and only the
  JSON *type* differs, but **this equivalence has not been checked against a live key** — see the live-key
  checklist below. If it turns out they differ, fall back to matching on `(team_key, player_name)`.
- `services/PlayerDataService` — new `@Service`. `getNotablePlayers(String teamId)` returns **up to 10**
  `PlayerData`, ranked per §6.4: drop anyone with no appearances (`matchesPlayed` null or 0), reserve one slot
  for the first-choice `Goalkeepers` entry (most appearances, or just the first if none has played, or no slot at
  all if the squad lists no keeper), score the rest on
  `appearanceShare*3 + (goals*2 + assists) + (passes/100 + duelsWon/10 + shotsTotal/5)`, and cap any one
  `position` bucket at 4. `getPlayersByTeam(String)` is also there for Phase 3's card.
  - **Signature note:** §9's sketch says `getNotablePlayers(teamId, 5)`. That "5" is superseded — §6.4, the
    Phase 2 "done when", and the step-4 test all say the list is 10, so the method takes just the id and the
    count is the `NOTABLE_PLAYER_COUNT = 10` constant. If Phase 3 wants a smaller list for the *prompt* it should
    slice the result, not re-rank.
  - The scoring weights are a reasonable starting point, not tuned. Phase 4 step 9 tunes the *prompt*, not this
    ranking, but if the notable list looks wrong during the step-9 smoke, this is where to adjust.

### Tests added (21)

- `system/utils/mappers/PlayerDataMapperTest` (5) — full-player binding, blank→null, non-numeric tolerated,
  `player_injured` truthiness, empty squad.
- `services/PlayerDataServiceTest` (7) — exactly 10 returned; first-choice keeper always in; keeper slot held
  even when no keeper has played; zero-appearance outfielders dropped; bucket cap ≤ 4; a high-volume midfielder
  beats a fringe forward; empty squad → empty list.
- `models/dto/TeamDataDtoTest` (2) — `get_teams` binding through `JsonFixtures` (the production apiClient
  mapper): team-level fields + nested venue, and the players array.
- `system/utils/mappers/TeamDataUpdateMapperTest` (+2) — `founded` and the nested `venue`; null venue still sets
  the key.
- `services/FootballApiServiceTest` (+3) — `getTopScorers` query shape, field binding, empty-on-failure.
- `services/TeamDataServiceTest` (+2, new `SavingTeams` nested class) — every squad member is `save`d and the
  team is upserted; `leagueScorerRank` lands on the matching player and stays null on the rest.

### Owed: live-key verification (from the phase "done when")

Not blocking Phase 3, but do this before the feature merges — run `POST /teams` against a real `API_FOOTBALL_KEY`
and a real Mongo, then check:

1. `PlayerData` is populated for every tracked competition, with sane values (blanks landed as null, not 0).
2. `get_topscorers` `player_key` actually equals `get_teams` `player_id` — inspect a few `PlayerData` docs for a
   known top scorer (e.g. Haaland) and confirm `leagueScorerRank` is set. If it is always null, the match key is
   wrong (see step 4 note above).
3. `venue` and `founded` are populated on `TeamData`.
4. Log a warning-worthy case: a national team (WORLD_CUP) with an empty `players` array — confirm it does not
   error.

### Where Phase 3 starts

Step 5: `enums/Perspective` (`FAN`, `RIVAL_FAN`, `NEUTRAL`); `TeamOneLiner` on `TeamData` with `@Builder.Default`
on the `Set`; `TeamOneLinerPromptBuilder implements PromptBuilder`; the `PromptBuilderFactory` team overload.
Read §3, §5, §6, and §8 (the `PromptPhrasing` helper Phase 1 extracted — `phraseStanding` now takes a
`Competition` directly, so it works without a `Fixture`). `PlayerDataService.getNotablePlayers` is ready for the
data block.


---

## Phase 3 handoff notes (for the Phase 4 session)

Phase 3 is complete. `./mvnw test` is green at **389 tests** (was 318; +71). Phases 1 and 2 are committed
(`17bff41`, `90d55a0` on `feature/team-oneliner`); Phase 3's changes are in the working tree, uncommitted.
Summary of what changed and what Phase 4 needs to know.

### Step 5 — perspective, the cache, the builder, the factory

- `enums/Perspective` — `FAN`, `RIVAL_FAN`, `NEUTRAL`, exactly as §3.
- `models/TeamOneLiner` — `@Getter @Builder @AllArgsConstructor`, equality on language + competition +
  perspective only. `positionAtGeneration` / `pointsAtGeneration` carry `@JsonIgnore`: they are a caching detail,
  and §2's documented response shape does not include them. `OneLinerControllerTest` pins that they stay off the
  wire.
- `domain/TeamData` gained `Set<TeamOneLiner> oneLiners` **with `@Builder.Default`** (bug #9's shape, avoided
  here) plus the null-safe `getOneLiners`, `addOneLiner`, `replaceOneLiner` trio and a `findOneLiner(language,
  competition, perspective)` lookup. The set is `@JsonIgnore`d as a second guard against it leaking through any
  endpoint that returns `TeamData` directly. Note **bug #9 itself is still open** — `Fixture.oneLiners` still has
  no `@Builder.Default`; only the new field is safe.
- `system/utils/prompts/TeamPromptContext` — a `record` (team, competition, recentForm, nextFixture,
  notablePlayers).
- `system/utils/prompts/TeamOneLinerPromptBuilder` — the seven `PromptBuilder` methods, no new template string.
  Role, style and examples switch on the perspective; the data block is identical across all three, because a
  perspective is a voice and not a different set of facts. It reuses `PromptPhrasing.phraseStanding` and
  `phraseRecentForm` from Phase 1.
- `PromptBuilderFactory.create(TeamPromptContext, Language, Perspective)` — an overload, per §6.2. **Watch out:**
  overloading `create` made `create(any(), any(), any())` ambiguous in Mockito stubs, which broke the existing
  `OneLinersServiceTest`. The fix was to type the first matcher (`any(Fixture.class)` /
  `any(TeamPromptContext.class)`). Any new stub of this factory has to do the same.

### Step 6 — the by-team fixture queries

- `FixtureRepository` gained `findByFinishedTrueAndHomeTeamIdOrFinishedTrueAndAwayTeamId(...)` and the
  `FinishedFalse` mirror, both taking a `Sort`.
- `FixtureService.getRecentFinishedForTeam(teamId, limit)` sorts descending and trims to the limit, and logs a
  warning when fewer than two finished fixtures come back (the `MAX_MATCH_DAYS` degradation §4.2 warns about).
  `getNextFixtureForTeam(teamId)` returns an `Optional<Fixture>`, ascending, first result.
- **Still owed, as §4.2 and §11 both say:** these two derived names have no integration coverage. A
  mis-resolved nested path (`homeTeam.id`) returns an empty list rather than failing, so
  `FixtureServiceTest.ByTeamLookups` can only pin the service's half — same id on both sides of the `Or` and the
  sort direction. Run them once against a real database during the step-9 smoke.

### Step 7 — the service, the response shapes, the route

- `models/TeamFacts` — a response type with a static `from(team, competition, recentForm, nextFixture,
  notablePlayers)`, plus nested `FormResult` / `NextFixture` / `Result`. Home-or-away is decided by comparing
  **team ids**, not names, since the id is what the query selected on. `models/TeamSmallTalk` pairs it with the
  one-liner. `TeamData` is deliberately never returned directly (§9).
- `services/TeamOneLinersService` — the flow in §9. Freshness is the two-part rule from §5 (played since, or the
  standings snapshot moved); a cached entry with a null `generatedAt` is always treated as stale.
  `resolveCompetition` implements §6.3 and throws `SmallTalkException(Messages.TEAM_HAS_NO_LEAGUE_STANDING)` for
  a team with no standing outside the World Cup (§6.5). Regeneration uses `replaceOneLiner` then
  `teamDataService.save(team)`.
- `system/utils/prompts/PromptPlayerSelection` — package-private, narrows the ten notable players to the 0–3 that
  earn a mention (injured regular → league scorer rank → clear standout, in that order). The builder calls it, so
  the card gets all ten and the sentence gets the shortlist. "Nobody qualifies" is a normal outcome and the data
  block then tells the model to talk about the table and the form instead. **The thresholds
  (`STANDOUT_MULTIPLE = 1.5`, `STANDOUT_RATE_PER_APPEARANCE = 0.5`) are a starting point** — if step 9 shows the
  sentences naming nobody too often, or naming somebody unconvincing, this is the file to tune.
- `controllers/OneLinerController` gained `GET /one-liners/teams/{teamId}` with `lang` required, `perspective`
  defaulting to `NEUTRAL` and `competition` optional. **No `JwtAuthFilter` change was needed** and none should be
  made: the route is public because it lives under `/one-liners`, and `JwtAuthFilterTest.Open` now pins that.
- `TeamDataService.getTeamById` now throws `NotFoundException` (404) instead of `IllegalStateException`. That is
  a checked exception, which rippled: `PromptBuilderFactory.create(Fixture, …)` now declares
  `throws SmallTalkException`, and `OneLinersService.getOneLiner` had to stop using `orElseGet` (a supplier
  cannot throw a checked exception) — it now uses an explicit `Optional` and a ternary. Behaviour is unchanged.
- `Messages` gained `NO_TEAM_FOUND` (a `%s` format string, takes the id) and `TEAM_HAS_NO_LEAGUE_STANDING`.

### Tests added (71)

- `system/utils/prompts/TeamOneLinerPromptBuilderTest` (21) — a nested class per concern: the three voices really
  differ, only `RIVAL_FAN` carries the "mock only what is in the data" clause, every perspective forbids reaching
  for training data, a null perspective falls back to `NEUTRAL`; the data block carries position, points, the
  home/away split, form, the next fixture with its venue side, the coach and the notable player; and the thin
  cases (no standing, empty form, no next fixture, empty *and* null notable lists, a team with no name).
- `system/utils/prompts/PromptPlayerSelectionTest` (9) — an injured regular beats a higher scorer, an injured
  fringe player is not notable, a league rank beats raw goals, better ranks come first, a merely-top-of-a-poor-
  squad scorer qualifies for nothing, a clear standout does, never more than three, never the same player twice.
- `services/TeamOneLinersServiceTest` (17) — cache hit when nothing changed, regeneration after a new finished
  fixture, after a position move and after a points move, a null timestamp always regenerates, `replaceOneLiner`
  actually overwrites (the §5 trap), the snapshot is stored, independent entries per perspective and language;
  plus the whole of §6.3/§6.5 competition resolution and the NotFoundException propagation.
- `models/TeamOneLinerTest` (6) and `domain/TeamDataTest` (6) — the equality-ignores-text contract and the
  `@Builder.Default` / add-vs-replace behaviour it forces.
- `controllers/OneLinerControllerTest` (+8, new `TeamRoute` nested class) — the new route's happy path, the
  snapshot not leaking, the `NEUTRAL` default, the competition passthrough, required `lang`, an unknown
  perspective, a 400 for a WORLD_CUP-only team and a 404 for an unknown id.
- `services/FixtureServiceTest` (+4, new `ByTeamLookups` nested class) — the arguments and sort of both derived
  queries, the limit, and an absent next fixture.
- `security/JwtAuthFilterTest` (+1 case) — `GET /one-liners/teams/2611` is public.
- `services/TeamDataServiceTest` — the `Lookup` rejection case was updated from `IllegalStateException` to
  `NotFoundException`; `testsupport/TestFixtures` gained `teamDataWithStanding(...)` and `standing(...)`.

### Where Phase 4 starts

Step 8: `services/jobs/TeamsJob` on an `Asia/Jerusalem` cron calling `teamDataService.saveCompetitionTeams()`
(§4.7), alongside the existing `FixturesJob` and `StandingsJob`. No tests beyond asserting it delegates.

Step 9 is the one that makes the feature good, and it is where three owed verifications should be done in the
same sitting against a real key and a real Mongo:

1. **Phase 2's live-key checklist** (above) — `PlayerData` populated, `leagueScorerRank` actually landing, venue
   and founded present, a national team's empty `players` array not erroring.
2. **The two derived queries from step 6** — call `GET /one-liners/teams/{id}` and confirm `recentForm` and
   `nextFixture` are non-empty for a team that has obviously played. An empty list here means the nested path
   `homeTeam.id` did not resolve, not that the team has no fixtures.
3. **The prompt itself.** Read real output for all three perspectives and tune `examples()` and `constraints()`
   in `TeamOneLinerPromptBuilder`, and the thresholds in `PromptPlayerSelection` if the wrong players are being
   named. `TeamOneLinerPromptBuilderTest` asserts on specific strings from `role()`, `style()` and the data
   block's labels — if a tuning change breaks it, decide deliberately which of the two was wrong, as the phase
   description says.


---

## Phase 4 handoff notes (feature complete)

Phase 4 is complete and the whole feature has been exercised end to end against live data. `./mvnw test` is green
at **393 tests** (was 389; +4). Step 8 added the job; step 9 was run against the real cluster and produced two
code fixes, one tuning change, and the verification of everything Phases 2 and 3 left owed.

### Step 8 — `TeamsJob`

- `services/jobs/TeamsJob.java` — `@Component`, `@RequiredArgsConstructor`, one `@Scheduled(cron = "0 0 4 * * MON,THU",
  zone = "Asia/Jerusalem")` method calling `teamDataService.saveCompetitionTeams()` and logging on completion,
  matching `FixturesJob`'s shape.
- **Why Monday and Thursday at 04:00.** The cadence is the owner's call, taken after Phase 4: `POST /teams` had
  always been a twice-a-year transfer-window operation, and a daily job was a real change in what it costs. Monday
  picks up the weekend round and Thursday the midweek one, which is where the squad numbers actually move, and it
  spends about 70% fewer calls than daily. Weekly was rejected as too stale — injury news is the most
  conversation-worthy fact the card holds. **Why 04:00:** it is the only hour that collides with nothing else: `FixturesJob` runs at 07:00, 20:00, 21:00,
  23:30 and 00:30, and `StandingsJob` at 00:30, 19:00 and 21:00. A squad refresh overlapping a fixture write
  would have both jobs touching `TeamData`. Keep that separation if the cron ever moves.
- `services/jobs/TeamsJobTest` (1) — the delegation assertion, plus `verifyNoMoreInteractions`. It is the first
  test under `services/jobs`; the other two jobs still have none.
- **`saveCompetitionTeams` takes about eleven minutes.** The live `POST /teams` returned 201 after **655
  seconds**. The cause is `TeamDataService.savePlayers`, which issues one `mongoTemplate.save` per player — some
  5,500 round-trips to Atlas. It is survivable for an overnight job but it is the obvious candidate for a
  bulk write, and it would be intolerable if the squad refresh ever needed to run on demand.

### Step 9 — what the live smoke found

Seeded in the documented order (`POST /fixtures?matchDays=30&matchDaysIntoFuture=14` → `POST /teams` →
`PATCH /teams/standings`, 283 fixtures of which 167 finished, 254 teams, 5,500 players), then read real output.

**Two defects, both fixed here:**

1. **The prompt named the team as its own opponent.** `TeamOneLinerPromptBuilder.phraseNextFixture` decided home
   or away by comparing the team's *name* against the fixture's home-team *name*. Those names come from different
   apifootball endpoints and disagree — `TeamData.name` is "Manchester United" while the fixture carries
   "Manchester Utd". The comparison failed, the venue side inverted, and the data block told the model United
   were "away at Manchester Utd" for a home tie against Sabah Baku. The model duly wrote about an "oddly listed"
   fixture, which is how it was caught. It now compares ids, exactly as `TeamFacts.isHome` already did — the
   Phase 3 notes flagged ids as the right key for this and the builder simply did not follow it. Pinned by
   `TeamOneLinerPromptBuilderTest.DataBlock.decidesTheVenueSideOnIdsNotNames`.
2. **The model returns two lines.** Real answers came back split on a hard newline with trailing spaces, which
   the single-line card cannot render. `TeamOneLinersService.singleLine` now strips and collapses whitespace runs
   before the text is stored — cheaper and more reliable than a prompt constraint. Pinned by
   `TeamOneLinersServiceTest.Caching.collapsesAMultiLineAnswerOntoOneLine`.

**One tuning change.** `PromptPlayerSelection` qualified *any* non-null `leagueScorerRank`, and `get_topscorers`
ranks the entire league — live data had a one-goal defender at 49th, and one-goal forwards at 10th and 16th.
That produced limp lines like "Kramaric already on the league scoring charts". A new
`MAX_LEAGUE_SCORER_RANK = 5` keeps the fact only while it still sounds like knowledge. This broke
`PromptPlayerSelectionTest.betterLeagueRanksComeFirst`, which had pinned rank 10 as qualifying; per the phase
instruction the test was judged wrong and updated, and
`aPlaceOutsideTheTopFiveQualifiesForNothing` now pins the cap. `STANDOUT_MULTIPLE` and
`STANDOUT_RATE_PER_APPEARANCE` were left alone — they were picking sensible players.

**Verifications closed:**

- *Phase 2's live-key checklist.* `playerData` populated (5,492 documents), `venue` and `founded` present on
  202 of 254 teams (the other 52 are national sides, as expected), and no error on a national team's empty
  `players` array. `leagueScorerRank` lands correctly: `player_key` in `get_topscorers` really is the same
  identifier as `player_id` in `get_teams` — a spot check traced a stored rank of 49 back to the exact
  `player_key` in the Bundesliga response, confirming the join rather than a coincidence.
- *Phase 3's two derived queries.* `recentForm` and `nextFixture` both come back populated, so the nested paths
  in `findByFinishedTrueAndHomeTeamIdOrFinishedTrueAndAwayTeamId` and its `FinishedFalse` mirror do resolve
  against a real database. That was the one thing the mocked tests could not prove.
- *The cache.* A second identical request returned a byte-identical sentence, and the stored document carries
  three independent entries (one per perspective) each with `generatedAt`, `positionAtGeneration` and
  `pointsAtGeneration`. The forced-regeneration half was covered in `TeamOneLinersServiceTest` rather than live,
  because moving a stored snapshot by hand needs a database write.
- *The rejections.* `GET /one-liners/teams/22` (France, WORLD_CUP only) returns 400 with
  `TEAM_HAS_NO_LEAGUE_STANDING`, and an unknown id returns 404 with `NO_TEAM_FOUND` — both live, both in the
  `SmallTalkResponse` envelope.

**A caveat about the reviewer, not the data.** The first write-up of this smoke claimed apifootball returned a
wrong coach for Liverpool (Andoni Iraola). It did not — Iraola was appointed Liverpool head coach on 4 June 2026,
succeeding Arne Slot; the model reviewing the output was working from stale training data. The lesson is the
mirror image of §6's constraints: when a sentence looks wrong, check the raw API response and a current source
before calling it either a data fault or a prompt fault. *(Corrected 2026-09-13.)*

### Sample of the finished output

> **FAN** — "Carrick's got us playing some decent stuff again, that 5-2 over Ipswich and Sesko's finish were
> class. Sitting 11th's nothing this early, get past Sabah Baku at home and the whole mood lifts."
>
> **RIVAL_FAN** — "Sixth with two draws already, and Ekitike injured before Atleti at Anfield — this Iraola era's
> hitting turbulence early."
>
> **NEUTRAL** — "Hoffenheim have somehow ended up 16th with no points, despite pushing both Köln and Dortmund in
> those 3-2 defeats. Big one against Stuttgart next, and they'll be hoping Kramarić keeps that early scoring
> touch going."

### What is next

Step 10 (`get_news` by `team_id`) and then the player one-liner, which `PlayerData` already unblocks. Two things
this phase deliberately did not touch: **bug #9** (`Fixture.oneLiners` still has no `@Builder.Default`), and the
per-player write loop in `TeamDataService.savePlayers` described above. Neither blocks the feature.


---

## Feature summary — how the whole thing flows

Written at the end of Phase 4, describing the feature as built rather than as planned. Two paths matter: the
scheduled ingestion that fills the collections, and the single request that turns them into a sentence.

### Ingestion — three jobs, three collections

| Job | Cron (`Asia/Jerusalem`) | Calls | Writes |
|---|---|---|---|
| `FixturesJob` | 07:00, 20:00, 21:00, 23:30, 00:30 | `get_events` per `Competition` | `fixture` |
| `TeamsJob` | Mon + Thu 04:00 (added in Phase 4) | `get_teams` + `get_topscorers` per `Competition` | `teamData`, `playerData` |
| `StandingsJob` | 00:30, 19:00, 21:00 | `get_standings` per `Competition` | `teamData.standings` |

Each is also exposed as an admin endpoint (`POST /fixtures`, `POST /teams`, `PATCH /teams/standings`) for manual
runs. Every external call goes through `ResponseHandler.process`, which never throws — a competition the plan
does not cover, or an HTML error page, yields an empty list and the run continues.

`TeamsJob` is the one this feature added. Per competition it first builds a `player_id → scoring-charts place`
map from `get_topscorers`, then upserts each team from `get_teams` (name, coach, crest, **venue**, **founded**)
and writes every squad member as a `PlayerData` document carrying that rank. `_id` is the apifootball
`player_id`, so a re-run updates in place.

### Request — `GET /one-liners/teams/{teamId}`

Public: it lives under `/one-liners`, which `JwtAuthFilter` does not gate, and `JwtAuthFilterTest.Open` pins that
so a future filter edit cannot silently close it. Parameters are `lang` (required), `perspective` (defaults to
`NEUTRAL`) and `competition` (optional).

1. **Resolve the team.** `TeamDataService.getTeamById` — a miss is a `NotFoundException`, 404 `NO_TEAM_FOUND`.
2. **Resolve the competition** (§6.3): the requested one if given, otherwise the team's domestic league — the
   standing that is neither Champions League nor World Cup — and the most-played of those if there is still a
   choice. A team with no league standing at all (a national side) is rejected with 400
   `TEAM_HAS_NO_LEAGUE_STANDING` (§6.5).
3. **Gather the dry facts.** The last five finished fixtures and the next unfinished one, both from `fixture` via
   derived queries that match the team on either side of the tie; and the ten notable players from `playerData`,
   ranked by contribution with the first-choice goalkeeper always included and a cap per position group.
4. **Look for a cached sentence.** `TeamData.oneLiners` is keyed on language + competition + perspective — the
   text is deliberately outside `equals`, which is why regeneration must use `replaceOneLiner` and not `add`.
   There is no TTL: a cached entry is fresh while the team has not played since it was written *and* its stored
   `positionAtGeneration` / `pointsAtGeneration` still match the table. A null `generatedAt` is always stale.
5. **Otherwise generate.** `TeamPromptContext` (team, competition, form, next fixture, the ten players) goes to
   `PromptBuilderFactory`, which returns a `TeamOneLinerPromptBuilder` for the requested perspective. The builder
   fills the shared `PromptBuilder` template — role, task, style, structure, constraints, examples, data. Role,
   style and examples change per perspective; the data block does not, because a perspective is a voice and not a
   different set of facts. `PromptPlayerSelection` narrows the ten to the 0–3 worth naming (injured regular →
   top-five scoring-charts place → clear standout); naming nobody is a normal outcome and the block then tells
   the model to talk about the table and the form. The answer is collapsed onto one line and stored, and only
   then does the team document get saved.
6. **Respond.** `TeamSmallTalk` = the one-liner plus `TeamFacts` — crest, coach, venue, founded, standing, recent
   form, next fixture, all ten notable players — inside the usual `SmallTalkResponse` envelope. `TeamData` is
   never returned directly, and the caching snapshot fields are `@JsonIgnore`d off the wire.

### The invariant worth remembering

Two apifootball endpoints name the same team differently — `get_teams` says "Manchester United", `get_events`
says "Manchester Utd". **Anything that decides which side of a fixture a team is on must compare ids.** Both
`TeamFacts.isHome` and `TeamOneLinerPromptBuilder.phraseNextFixture` do; the latter only after Phase 4 caught it
telling the model a team was away at itself.

---

## The eleven-minute squad refresh — fixed

`TeamDataService.saveCompetitionTeams` took **655 seconds** in the Phase 4 smoke. Almost none of that was
apifootball: the whole run makes fourteen HTTP calls, a `get_teams` and a `get_topscorers` per competition. The
cost was the database — `savePlayers` issued one `mongoTemplate.save` per player, roughly 5,500 sequential
round-trips to a hosted MongoDB at about 120 ms each.

**What changed.** `savePlayers` now builds one unordered `BulkOperations` per team, adds a `replaceOne` with
upsert per player, and executes it once. That turns ~5,500 round-trips into ~250. A replace on `_id` is exactly
what `save` already did, so the write itself is unchanged — no mapper, document shape or endpoint moved, and the
change is contained to one private method.

**Measured, not predicted.** A live `POST /teams` against the same cluster returned 201 in **75 seconds**, down
from 655 — a little under nine times faster. The data was verified afterwards: 5,510 players, 248 carrying a
`leagueScorerRank`, 254 teams with 202 venues, and the cached one-liners on an already-generated team still in
place, since the team `Update` does not touch `oneLiners`.

Where the remaining 75 seconds go: about 11 in apifootball (fourteen calls at roughly 0.8 s each) and the rest
still in the database. The job now makes about 508 round-trips — one `upsert` per team plus one bulk `execute`
per team — at the same ~125 ms each. **The next win, if it is ever wanted, is to widen the scope from per-team to
per-competition:** one `BulkOperations` per competition carrying both the team upserts and every squad in it
would leave roughly fourteen round-trips and put the run in the fifteen-second range, dominated by the API. It
was not done here because per-team batching is where the two-orders-of-magnitude gain was, and per-competition
batching makes a single bad row fail a whole league's write rather than one club's.

Two things to know about it:

- **An empty squad must skip the bulk entirely.** National teams come back from `get_teams` with no players at
  all, and `execute()` rejects a bulk holding no operations. `writesNothingForATeamWithNoSquad` pins that.
- **Failure reporting differs.** An unordered bulk collects failures into a `BulkOperationException` at
  `execute()` rather than throwing on the offending document, so a bad row now fails that team's batch instead of
  that one player. For a nightly refresh that is an acceptable trade; it is worth remembering if this is ever
  called on demand.

`TeamDataServiceTest.SavingTeams` was rewired to verify against a mocked `BulkOperations` instead of counting
`save` calls. The scorer-rank backfill assertion still captures the written players, just through `replaceOne`.

Rejected alternatives: parallelising the saves keeps the same 5,500 round-trips and only spends connections to
hide them; diffing against stored players to skip unchanged rows adds a read per player to avoid a write.

**Left undone deliberately: nothing ever deletes a player.** The refresh writes the squad it received and never
reconciles it against what is already stored, so a player who leaves the tracked leagues keeps his `teamId` and
stats forever and can still be named on his old club's card. That is recorded as **bug #11 in `bugs.md`**, with
the suggested fix (a `remove` with an `_id` `$nin` the squad just written, added to the same bulk) and the trap
that goes with it — an empty `players` array is a normal response for a national team, so treating it as "everyone
left" would wipe a squad whenever the API has a bad day.
