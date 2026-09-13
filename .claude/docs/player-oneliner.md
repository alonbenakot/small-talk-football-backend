# Player one-liner — implementation plan

A "player small talk" feature: given a player, return one or two sentences a user can say that make them sound
current on that player — his form, his standing in the squad, his injury status, his place in the scoring charts
— **plus** the dry facts behind it so the frontend can render a card: photo, shirt number, position, age, club,
season stats, and the club's own context.

It is the third one-liner, after the fixture one (`GET /one-liners/{fixtureId}`) and the team one
(`GET /one-liners/teams/{teamId}`). The team feature was built with this one in mind: `PlayerData` already exists
as its own collection, populated twice a week by `TeamsJob`, precisely so a player can be looked up by id rather
than found by scanning team documents.

> **Reading order:** the build order is immediately below — start there, then read only the sections the phase
> you are on refers to. Sections 1–11 are reference material, not a sequence to work through. §12 holds the
> questions for the owner, with their answers.
>
> **Revised after review (2026-09-13). Decisions taken:** players are reached **by id only**, from a team's squad
> list — no name search; the cached sentence lives **on `PlayerData`** and `savePlayers` switches from
> `replaceOne` to a `$set` update so it survives the refresh (§5); **one voice, no perspective** (§3); bug #11
> is fixed in Phase 1 (§10); the Champions League stat-scope check was run live and the stats are **season-wide** — no
> per-competition storage needed (§4.2, §12.3); the "wrong coach"
> caveat inherited from the team plan was itself wrong and is withdrawn (§12.6). A second live smoke is owed once
> development is finished, because the ingestion write path changes.

---

## Start here — build order, grouped into sessions

Ten steps in four phases, each phase independently verifiable and sized for one session. Open a fresh session
per phase rather than carrying the whole feature in one context. The opening prompt is literally
*"Read `.claude/docs/player-oneliner.md` and implement phase N."*

**Every phase ships its own tests.** They are listed inline per step, and a phase is not finished until they pass
alongside the existing suite (green at **393 tests** as of the team feature). §11 is a reference index of the
same list organised by component, not a separate stage of work. Conventions live in `CLAUDE.md`: plain
JUnit/Mockito with no Spring context wherever possible, `@WebMvcTest` with `excludeFilters` for controllers,
`JsonFixtures.parse` for anything deserialised from apifootball. Run `./mvnw test` (~15s, no Docker or network).

### Phase 1 — Ingestion changes and player lookup (steps 1–4) — ✅ DONE (2026-09-13)

Nothing AI-shaped here. It changes the one write path this feature depends on, and answers the question the
feature title takes for granted: *how does a user select a player at all?* Today nothing exposes `playerData`
over HTTP.

1. ~~Settle the competition-scope question~~ — **done before Phase 1, live, 2026-09-13.** The stats are
   season-wide (§4.2); nothing to build. Kept as a numbered step so the later step numbers in this document stay
   valid.
2. **`savePlayers`: `replaceOne` → `$set` upsert, plus the bug #11 removal** (§5, §10). Both edit the same bulk
   write, so they ship together. The `$set` is what lets the cached sentence live on `PlayerData`; the removal
   is what stops a departed player being selectable.
   - **Tests:** `TeamDataServiceTest.SavingTeams` — the write is an upsert-`$set`, not a replace; a field that
     went from a value to null (a `leagueScorerRank` that dropped out of the charts) is cleared, not left
     stale; players stored for the team but absent from the payload are removed; an **empty** payload removes
     nobody; a payload **smaller than half** the stored squad removes nobody and logs (§10). Note bug #11 is currently "by inspection, not covered by a test" — the removal case is the first
     one, and `bugs.md` row 11 is marked fixed when it lands.
3. **`PlayerDataService.getPlayerById`** returning the document or throwing `NotFoundException` (mirroring what
   `TeamDataService.getTeamById` became in the team feature), plus the light `PlayerSummary` response shape and
   a `getSquadSummaries(teamId)` for the picker (§2.2).
   - **Tests:** `PlayerDataServiceTest` — a hit, a miss throwing `NotFoundException`, the summary carrying only
     the picker fields.
4. **`PlayerController`** with `GET /players/teams/{teamId}` (§2.2). `/players` is not in any `isJwtRequired*`
   branch of `JwtAuthFilter`, so it is public with **no filter change** — the same reasoning that kept the team
   route off `/teams`.
   - **Tests:** `PlayerControllerTest` (`@WebMvcTest`, `excludeFilters` for `JwtAuthFilter`) — the route, and an
     empty list for a team with no stored squad. One `JwtAuthFilterTest.Open` assertion that
     `GET /players/teams/{id}` is public, so a future filter edit cannot silently gate it.

**Done when:** the suite is green, a real `POST /teams` still completes in the same order of time as before (the
`$set` write must not regress the bulk-write performance of commit `939a5bf`), and `GET /players/teams/2611`
lists a squad against a real database.
✅ Suite green at 407 tests (+14). The two live checks are still owed — no credentials were in the Phase 1
shell; see the Phase 1 handoff notes at the end of this document.

### Phase 2 — The one-liner (steps 5–7) — ✅ DONE (2026-09-13)

The core. The traps here are the two the team feature already paid for once: a `Set` whose equality ignores its
text, and a builder-created collection with no `@Builder.Default`.

5. **The cache.** `PlayerOneLiner` value type and `Set<PlayerOneLiner> oneLiners` on `PlayerData` with
   `@Builder.Default` and `@JsonIgnore` (§5). Snapshot fields for staleness: appearances, goals, assists,
   injured, scorer rank. **The set must be excluded from the `$set` update in `savePlayers`** — that is the
   whole point of step 2 — and a test must pin that a refresh leaves it in place.
   - **Tests:** `PlayerOneLinerTest` — equality on language while ignoring `text`, mirroring
     `TeamOneLinerTest`. `PlayerDataTest` — add-vs-replace behaviour and the `@Builder.Default` trap, mirroring
     `TeamDataTest`. `TeamDataServiceTest.SavingTeams` — a stored one-liner survives a squad refresh.
6. **`PlayerAngleSelection`, `PlayerPromptContext`, `PlayerOneLinerPromptBuilder`, and the factory overload**
   (§6). Implement the seven `PromptBuilder` methods; never a new template string.
   - **Tests:** `PlayerAngleSelectionTest` — one case per angle in §6.3, in priority order: an injured regular
     beats a scoring rank, a top-five rank beats being the squad's leading contributor, an ever-present defender
     with no goals still yields an angle, a fringe player yields the honest low-minutes angle, and a player with
     no appearances yields the "hasn't featured" angle rather than nothing.
     `PlayerOneLinerPromptBuilderTest` — the data block carries the player, his club and the club's standing;
     a null club, a null standing, absent keeper stats and an empty contribution history all degrade to readable
     lines rather than NPEs; the constraints forbid reaching for training data.
7. **`PlayerOneLinersService` and `GET /one-liners/players/{playerId}`** (§2.1, §8), with `PlayerFacts` /
   `PlayerSmallTalk` as the response shapes.
   - **Tests:** `PlayerOneLinersServiceTest` (Mockito) — cache hit when nothing moved; regeneration after an
     appearance, after a goal, after an injury flag flips, and after a scorer-rank move; a null `generatedAt`
     always regenerates; `replaceOneLiner` actually overwrites; independent entries per language; the multi-line
     answer is collapsed onto one line (the defect the team feature found live, §5).
     `OneLinerControllerTest` — a new nested `PlayerRoute` class: happy path, required `lang`, a 404 for an
     unknown id, and the snapshot fields not leaking onto the wire. One `JwtAuthFilterTest` assertion that
     `GET /one-liners/players/{id}` is public.

**Done when:** the suite is green and the endpoint returns a cached sentence on the second call and a fresh one
after the underlying stats move.
✅ Suite green at 461 tests (+54). The live check of the endpoint is still owed, along with Phase 1's two — see
the Phase 2 handoff notes at the end of this document.

### Phase 3 — Recent contributions from our own fixtures (step 8)

Separable on purpose: the feature is shippable without it, and it touches fixture ingestion.

8. **Bind the scorer ids and join on them** (§4.4). `get_events` goalscorer entries carry `home_scorer_id` /
   `away_scorer_id` / `home_assist_id` / `away_assist_id`, the same identifier as `PlayerData.id`. Add them to
   `GoalscorerItem` (verbatim `@JsonProperty`), carry `scorerId` / `assistId` onto `Goal` in `FixtureAssembler`,
   and the player's recent contributions are a filter over his club's recent finished fixtures by id. No name
   matching. Fixtures ingested before this change have no ids on their goals and simply contribute nothing —
   the window is 30 days, so that resolves itself.
   - **Tests:** `MatchDtoTest` — the four id fields bind through `JsonFixtures.parse`. `FixtureAssemblerTest` —
     ids land on `Goal`, and a blank id lands as null. A contributions test — goals and assists in the club's
     recent fixtures are counted per fixture for the right player only; fixtures whose goals carry no ids
     contribute nothing. One builder test that the contribution lines reach the data block and that "no
     contributions on record" is phrased as absence of data, not as zero goals.

**Done when:** the suite is green and, against a real database after a `POST /fixtures`, a known scorer's
recent goals show up in his card while a defender's card shows none.

### Phase 4 — Live smoke and tuning (steps 9–10)

9. **Manual smoke** against a real database and a real OpenAI key, across the full range of player types: a
   league-leading striker, a first-choice goalkeeper, an ever-present centre-back, an injured regular, a fringe
   squad player, and a player with zero appearances. Read the actual output and tune `examples()`,
   `constraints()` and the angle thresholds from what comes back. **This is where the feature is made good** —
   the prompt text in §6 is a starting point, not a finished artefact.
10. **Record what the smoke found** in a handoff section at the end of this document, as each previous phase did,
   including any defect it turned up and the tests that now pin it.

**Done when:** all six player types produce a sentence you would actually say out loud, and none of them makes a
claim the data does not support.

### Later

- `get_players?player_id=` for nationality, birthdate and minutes played (§4.5) — deferred. With the `$set`
  write in place, a lazily fetched nationality now has a natural home on `PlayerData`, so this is cheaper than
  it was when first deferred.

---

## 1. What already exists (and what we reuse)

| Concern | Existing code | Reuse |
|---|---|---|
| The player collection | `domain/PlayerData`, `PlayerDataRepository`, `PlayerDataMapper` | As is — 5,492 documents live, refreshed Mon/Thu by `TeamsJob` |
| Squad reads and ranking | `PlayerDataService.getPlayersByTeam` / `getNotablePlayers` | Extend with `getPlayerById`, `getSquadSummaries`, `save` |
| Prompt assembly | `PromptBuilder` (default `buildPrompt()` templating role/task/style/structure/constraints/examples/data) | Implement a fourth builder, no new template strings |
| Prompt selection | `PromptBuilderFactory` — already has a fixture overload and a team overload | Add a player overload |
| Narrowing facts to what is worth saying | `PromptPlayerSelection` (picks 0–3 players out of ten for the team sentence) | The direct model for `PlayerAngleSelection` (§6.3) — same idea, one level down |
| LLM call | `AiService.generate(String)` | As is |
| Cached one-liner | `OneLiner` on `Fixture`, `TeamOneLiner` on `TeamData` — both key on everything but the text | Same pattern; see §5 for where the set lives |
| Club context | `TeamData` (name, crest, coach, `Map<Competition, Standing>`) | Joined via `PlayerData.teamId` |
| Recent form / next fixture | `FixtureService.getRecentFinishedForTeam` / `getNextFixtureForTeam` | As is — already built for the team feature, already verified live |
| Phrasing helpers | `PromptPhrasing.phraseStanding` / `phraseRecentForm` | As is; `phraseStanding` takes a `Competition` explicitly, so it works without a fixture |
| Response envelope | `SmallTalkResponse<T>`, `ControllerAdvice`, `Messages` | As is |

The team feature paid down most of the groundwork. What is genuinely new here is: a way to *find* a player over
HTTP, a per-player cache, a per-player staleness rule, and the angle selection that decides what a sentence about
one footballer should even be about.

---

## 2. Endpoints

### 2.1 The one-liner

```
GET /one-liners/players/{playerId}?lang=BRITISH
```

On the existing `OneLinerController`. Two path segments, so no collision with `GET /one-liners/{fixtureId}`, and
it sits under `/one-liners`, which `JwtAuthFilter` does not gate — **no security change is needed**, exactly as
with the team route.

```java
@GetMapping("/players/{playerId}")
@ResponseStatus(HttpStatus.OK)
public SmallTalkResponse<PlayerSmallTalk> getPlayerOneLiner(@PathVariable String playerId,
                                                            @RequestParam Language lang) throws SmallTalkException
```

`lang` is required, matching both existing one-liner routes. There is no `perspective` parameter — decided, §3.

Response — `SmallTalkResponse<PlayerSmallTalk>`:

```jsonc
{
  "data": {
    "oneLiner": {
      "language": "BRITISH",
      "text": "Haaland's eight in ten and top of the charts — City are just feeding him.",
      "generatedAt": "2026-09-10T10:02:11Z"
    },
    "facts": {
      "id": "659972248", "name": "Erling Haaland",
      "image": "https://.../68451_e-haaland.jpg",
      "number": "9", "position": "Forwards", "age": "26", "captain": true, "injured": false,
      "team": { "id": "80", "name": "Manchester City", "crest": "https://.../80_manchester-city.jpg",
                "coach": "Enzo Maresca" },
      "competition": "PREMIER_LEAGUE",
      "season": { "matchesPlayed": 10, "goals": 8, "assists": 0, "shotsTotal": 33,
                  "keyPasses": 4, "passes": 86, "passesAccurate": 54,
                  "tackles": 1, "interceptions": null, "clearances": 3,
                  "duelsTotal": 29, "duelsWon": 17,
                  "yellowCards": 0, "redCards": 0, "rating": "7.30",
                  "saves": null, "insideBoxSaves": null, "goalsConceded": null },
      "leagueScorerRank": 1,
      "squadContext": { "leadingScorer": true, "leadingContributor": true,
                        "everPresent": true, "appearanceShare": 1.0, "squadSize": 24 },
      "teamStanding": { "position": 1, "playedMatches": 10, "points": 25, "overall": {...} },
      "recentContributions": [ { "fixtureId": "...", "date": "...", "opponent": "Chelsea",
                                 "goals": 2, "assists": 0 } ],   // phase 3
      "nextFixture": { "fixtureId": "...", "opponent": "Arsenal", "home": false, "kickOff": "..." }
    }
  },
  "systemMessage": {...},
  "statusCode": 200
}
```

`season` is a flat echo of the stored stats so the frontend can render whatever it likes; nulls are meaningful
and must not be coerced to zero (a blank in the feed means "not recorded", not "none" — §4.1). `squadContext` is
computed, not stored (§4.3), and it is what turns a row of numbers into something worth saying.

Do **not** return the `PlayerData` document directly. The cache is embedded on it (§5), so
the cached sentences would leak into the body — the same reason `TeamFacts` exists rather than returning
`TeamData`.

### 2.2 Finding a player

Decided: the app presents a team's squad and the user picks from it, so the only discovery route is one
read-only endpoint on a new `PlayerController` (`@RequestMapping("players")`), public because `/players` matches
no `isJwtRequired*` branch:

```
GET /players/teams/{teamId}      → List<PlayerSummary>, the stored squad for that team
```

`PlayerSummary` is a light shape — `id`, `name`, `image`, `number`, `position`, `injured`, `matchesPlayed` — not
the 25-field document. The picker does not need the stats and the card fetches them anyway; `injured` and
`matchesPlayed` are included only so the picker can grey out or sort the players nobody will want to ask about.
Order the list by position bucket then shirt number, so it reads like a squad list rather than a database dump.

No name search. It was considered and dropped: the flow never types a name, and an unindexed regex over 5,500
documents was the only thing that would have needed an index later.

---

## 3. Perspective — decided: none

**The owner's instinct was that a perspective is less needed here, and it is right — here is why.** `Perspective` on the team one-liner works because the
speaker's relationship to the subject is genuinely different in each mode: `FAN` and `RIVAL_FAN` are defined by a
badge, and a club badge is exactly what a supporter has an allegiance to. A player is a different kind of
subject. A neutral, a fan and a rival looking at "eight goals in ten games" all agree on what they are looking
at; what varies is only whether they are pleased about it, which is a thin difference and produces three
sentences that read as the same sentence with the adjectives swapped. Worse, `RIVAL_FAN` aimed at an individual
is the mode most likely to slide from banter into something personal about a named real person — which is a
content risk the team feature never had, because a club is not a person.

So: **v1 ships one voice.** The cache key is language only, the route takes no `perspective`, and
`PlayerOneLinerPromptBuilder` has a single `role()` / `style()` / `examples()`.

**If variety is wanted later**, the axis that actually varies for a player is not allegiance but *stance on the
evidence*, and two values cover it:

| | role | when it earns its place |
|---|---|---|
| `HYPE` | "You rate this player and you're telling a friend why." | a good run, a rank, a standout contribution |
| `SCEPTIC` | "You are unconvinced by this player and you're saying so." | thin minutes, a dry spell, a big name having a quiet season |

That is a real difference — the two would reach for *different facts*, not just different adjectives — whereas
`FAN` / `RIVAL_FAN` would reach for the same ones. It is also cheap to add later: the cached set would gain a
field, old entries would compare unequal to new ones, and they would simply regenerate. **There is no
forward-compatibility cost to leaving it out now**, which is the main reason not to build it speculatively.

Decided (§12.2): one voice in v1.

---

## 4. Where the dry facts come from

### 4.1 The player himself — already in Mongo, free

`PlayerData` holds everything `get_teams` gives: name, image, number, position (`Goalkeepers` / `Defenders` /
`Midfielders` / `Forwards`), age, captaincy, appearances, goals, assists, shots, key passes, passes and accurate
passes, tackles, interceptions, clearances, duels, keeper stats (saves, inside-box saves, goals conceded), cards,
injury flag, rating, and `leagueScorerRank` backfilled from `get_topscorers`.

Two properties of that data the whole feature has to respect, both learned the hard way in the team feature:

- **Every numeric field is a nullable `Integer`, and null means "not recorded", not zero.** apifootball sends
  `""` for an unused squad member across every stat, and the mapper stores that as null. Rendering it as `0` in
  the card, or phrasing it as "no goals" in the prompt, states something the feed never said. Phrase absence as
  absence.
- **`passesAccurate` is a count of completed passes, not a percentage.** It is only meaningful as a ratio to
  `passes`. Use it as a ratio or not at all.

### 4.2 Which competition are these stats from? — **resolved: season-wide**

**Checked live on 2026-09-13.** `get_teams&league_id=152` (Premier League) and `get_teams&league_id=3`
(Champions League) share five clubs — Manchester City, Liverpool, Manchester United, Arsenal, Aston Villa. Every
one of the 152 player objects those clubs have in common was **byte-identical** across the two responses:
identical squad lists, identical `player_match_played`, `player_goals`, `player_passes`, `player_rating`,
`player_injured`, everything. apifootball does not scope player statistics to the requested league.

Consequences:

- The stored `PlayerData` numbers are correct for every club regardless of which competition's call wrote them
  last. **No per-competition stats map, no mapper change, no change to the team one-liner's ranking.**
- The concern this section originally raised — that `CHAMPIONS_LEAGUE` being last in `Competition.values()`
  would leave every European club with its European numbers — was worth checking and turned out to be unfounded.
  The team one-liner was never affected.
- Because the two calls return the same squad list, the per-competition ordering caution in §10 (run the bug #11
  removal only from the domestic call) is belt-and-braces rather than necessary. Keep the guard anyway: it is one
  line and it means a future change to the API cannot make the two calls delete each other's players.
- What "season" means here is not stated by the API. The Phase 4 smoke should look at whether the totals move
  after a midweek European round; if a club's `player_match_played` climbs with a Champions League game, the
  numbers are all-competitions, and the prompt should say "this season" rather than "in the league".

### 4.3 Squad context — computed, not stored

The single most valuable thing we can say about a player is not his stat line but *where he sits in his own
team*. "Eight goals" means nothing on its own; "their leading scorer by a distance" means something. It costs one
`findByTeamId` — the same call the card already makes — and no API call at all:

- `leadingScorer` — the highest `goals` in the squad, and by how much.
- `leadingContributor` — highest `goals * 2 + assists`, the same measure `PromptPlayerSelection` already uses.
- `everPresent` / `appearanceShare` — his appearances against the squad's maximum. This is what lets a
  goalless centre-back have a sentence at all.
- `firstChoiceKeeper` — for a `Goalkeepers` entry, whether he is the most-used keeper in the squad.
  `PlayerDataService.firstChoiceKeeper` already computes exactly this; make it reusable rather than duplicating
  it.

Keep this a small computed record (`SquadContext`), built in `PlayerDataService`, so both the card and the prompt
read the same numbers.

### 4.4 Recent contributions — our own `Fixture` collection (Phase 3)

`FixtureService.getRecentFinishedForTeam(teamId, n)` is already built and already verified against a real
database. Each `Fixture` carries a `List<Goal>` with `goalBy`, `assistBy`, `minute`, `penalty` and `teamName`.

**The join is by player id.** `get_events` goalscorer entries carry `home_scorer_id`, `away_scorer_id`,
`home_assist_id` and `away_assist_id` (see `docs/football-api-responses.md`, `get_events`), and they are the
same identifier as `player_id` in `get_teams` — i.e. `PlayerData.id`. Today `GoalscorerItem` does not bind
them and `Goal` carries only the free-text names, so Phase 3 adds the four fields to the DTO, two nullable
`scorerId` / `assistId` fields to `Goal`, and maps them in `FixtureAssembler`. After that, a player's recent
contributions are:

```
recentForm.stream()                        // the club's last five finished fixtures, already fetched
    .map(fixture -> count goals where scorerId == player.id, assists where assistId == player.id)
    .filter(nonZero)
```

An earlier draft of this section planned a surname-and-initial name matcher because the ids had not been
noticed. Do not build it — a blank id (the feed sends `""` when it does not know) maps to null and the goal is
simply not attributed, which is the right outcome.

Phrase the empty case as *"no goals or assists on record in the last five"* — with ids the claim is now
supported, unlike the name-based version, but keep the wording modest because a fixture ingested before the
ids were bound contributes nothing.

### 4.5 `get_players` — deferred, with the reason

`action=get_players&player_id=` is the only source of `player_birthdate`, `player_country` and `player_minutes`.
Nationality in particular would improve the sentence ("the Norwegian is..."), and it is a single cheap call.

Two things make it a poor fit for v1:

1. **Nothing for it exists yet.** An earlier revision of the team plan claimed `getPlayerById` /
   `getPlayersByName` and a `PlayerDto` had been added to `FootballApiService`; that was wrong and has been
   corrected there. `FootballApiService` has no `get_players` call, so this is a small job, but a job.
2. **It is a live call per player on the request path**, or a lazy backfill that has to be excluded from the
   `$set` write the same way `oneLiners` is (§5). Neither is hard, but neither is free, and nationality is a
   nice-to-have for a sentence that already has plenty to say.

With the `$set` write in place the backfill has an obvious home — a `country` / `birthdate` pair on `PlayerData`,
excluded from the refresh alongside `oneLiners` — so this is the first thing to add once v1 is live and the
sentences are read.

---

## 5. Caching and freshness

Same principle as the team one-liner: **no TTL.** A sentence is good until the facts under it move.

A cached player one-liner is **stale** when any of these differ from the snapshot taken at generation time:

1. `matchesPlayed` — he has played since;
2. `goals` or `assists` — he has contributed since;
3. `injured` — the flag has flipped, in either direction;
4. `leagueScorerRank` — his place in the charts has moved.

All four are read off the current `PlayerData` document, so the comparison is one small object against another
and needs no new configuration — and therefore nothing to add to the shadowing
`src/test/resources/application.properties`.

In practice this means a sentence is regenerated at most a couple of times a week, since `TeamsJob` is the only
thing that moves those numbers. That is the right cadence: it is the same freshness the team card has.

```java
@Getter @Builder @AllArgsConstructor
public class PlayerOneLiner {
    Language language;
    String text;
    Instant generatedAt;
    @JsonIgnore Integer matchesPlayedAtGeneration;
    @JsonIgnore Integer goalsAtGeneration;
    @JsonIgnore Integer assistsAtGeneration;
    @JsonIgnore Boolean injuredAtGeneration;
    @JsonIgnore Integer scorerRankAtGeneration;
    // equals/hashCode on language only — text is deliberately excluded
}
```

Three details that are not optional, each one a defect the team feature already hit:

- **Regeneration uses `replaceOneLiner`, never `addOneLiner`.** `add` on a `Set` whose equality ignores `text` is
  a silent no-op when an entry already exists, so the stale sentence would survive.
- **`@Builder.Default` on the set.** Lombok's `@Builder` ignores field initialisers without it, and the set is
  then null and `addOneLiner` NPEs. That is bug #9 verbatim — still open on `Fixture`, avoided on `TeamData`.
- **The model returns two lines.** Real answers came back split on a hard newline with trailing spaces. Collapse
  whitespace runs before storing, exactly as `TeamOneLinersService.singleLine` does. Reuse that rather than
  writing a third copy; it belongs somewhere shared.

**Where the set lives — decided: on `PlayerData`, and `savePlayers` changes to make that safe.** The set sits
on the document exactly as `TeamData.oneLiners` does, with `@Builder.Default` and `@JsonIgnore`. What makes it
survive the twice-weekly refresh is changing the bulk write from `replaceOne` (which discards every field the
mapper did not produce) to an upsert with `$set` of only the mapped fields:

```java
BulkOperations bulk = mongoTemplate.bulkOps(BulkMode.UNORDERED, PlayerData.class);
for (PlayerData player : players) {
    Document mapped = new Document();
    mongoTemplate.getConverter().write(player, mapped);          // the same mapping save() would use
    Update update = Update.fromDocument(mapped, "_id", "oneLiners");   // never touch the cache
    bulk.upsert(Query.query(Criteria.where("_id").is(player.getId())), update);
}
bulk.execute();
```

Two details that decide whether this is correct:

1. **Nulls are omitted by the converter, so `$set` cannot clear them.** A `leagueScorerRank` that was 4 last
   week and is absent from this week's `get_topscorers` is `null` on the mapped object, absent from the
   `Document`, and therefore *left at 4* by the update. `replaceOne` cleared it for free; `$set` does not. So
   after `fromDocument`, every nullable field the mapper owns that is missing from the document gets an explicit
   `update.unset(field)` (or `set(field, null)`). The cleanest way is a fixed list of the mapper's nullable field
   names next to the mapper, so nothing is forgotten when a field is added. The step-2 test "a value that became
   null is cleared" pins this.
2. **It stays one bulk round-trip per team**, so the performance of commit `939a5bf` is kept. `$set` of ~25
   fields per player versus a replace of the same fields is not a meaningful difference on the wire. The Phase 1
   "done when" checks the live `POST /teams` time to be sure.

The bug #11 removal (§10) rides along in the same bulk, before `execute()`.

A **second live smoke** is owed once development finishes — this changes the ingestion write path that the team
feature verified live, and the verification has to be repeated: `POST /teams` completes, a stored one-liner
survives it, and a rank that dropped out of the charts is cleared.

---

## 6. Prompt design

### 6.1 The builder

`system/utils/prompts/PlayerOneLinerPromptBuilder implements PromptBuilder` — implement the seven methods, never
a new template string (per `CLAUDE.md`).

- **role** — "You follow football closely and you're making conversation about {player} of {club}."
- **task** — "Generate a casual comment that shows you know how this player is doing right now."
- **style** — `language.getDescription()` + "observational, casual friendly banter."
- **structure** — "1-2 sentences, under 20 words each, no line breaks, no emojis." (Matches the other three.)
- **constraints** — firmer than any existing builder. See §6.2.
- **examples** — 4–6, chosen to demonstrate the *range of angles* in §6.3, not just the flattering one. Include a
  fringe player and an injured one, so the model has a template for the unglamorous cases that are the majority
  of the collection.
- **data** — the player, his squad context, his club and its standing, the next fixture, and (Phase 3) his recent
  contributions.

### 6.2 Constraints — the failure mode here is worse than for a team

The model knows a great deal about famous footballers and much of it is out of date: former clubs, transfer
rumours, nationality, honours, injuries from two seasons ago. For a club, stale knowledge produces a slightly
wrong sentence. For a named individual, it produces a confidently wrong claim *about a real person*, which is the
one failure this feature genuinely has to prevent.

So the constraints block must say, at minimum:

```
Use only the data provided.
Do not mention this player's nationality, age, former clubs, transfer value, honours or career history
unless it appears in the data below.
No predictions, no invented transfers, injuries, quotes or statistics.
Do not speculate about his future, his attitude, his fitness or his relationship with the club.
If the data is thin, say something modest and true rather than reaching for something you remember.
```

That last line is doing real work: it gives the model a licence to be dull, which is the correct behaviour when
there is nothing to say, and it is the cheapest defence against invention.

### 6.3 `PlayerAngleSelection` — deciding what the sentence is about

The direct analogue of `PromptPlayerSelection`, one level down: that class decides *which players* a team
sentence should name; this one decides *what about one player* is worth saying. Same shape — a package-private
final class in `system/utils/prompts`, a static `select`, tunable constants at the top with a comment saying they
are a starting point.

Take the **first** angle that applies, and pass it to the data block as the lead fact:

| # | Angle | Condition | The line it produces |
|---|---|---|---|
| 1 | `INJURED` | `injured` and appearances at or above the squad median | "out injured, and they'll miss him — he'd started nine of ten" |
| 2 | `LEAGUE_SCORER` | `leagueScorerRank != null && <= 5` | "third in the scoring charts already" |
| 3 | `LEADING_CONTRIBUTOR` | clearly ahead of the squad on `goals * 2 + assists` — the existing `STANDOUT_MULTIPLE` / `STANDOUT_RATE_PER_APPEARANCE` thresholds | "carrying their front line, eight goals and four assists" |
| 4 | `KEEPER` | first-choice `Goalkeepers` with recorded saves | "twenty-one saves in nine, they're conceding plenty in front of him" |
| 5 | `EVER_PRESENT` | `appearanceShare` at or near 1.0 with no attacking angle | "hasn't missed a minute at centre-back for a side sitting second" |
| 6 | `FRINGE` | appearances well below the squad median but above zero | "barely featured — three appearances all season" |
| 7 | `UNUSED` | no appearances at all | "yet to play a competitive minute this season" |

**Angles 6 and 7 are the ones that make this feature honest.** The majority of a 5,500-player collection is not
notable, and a user is free to select any of them. A sentence that pretends a fringe player is interesting is
worse than one that says plainly that he is not playing. Do not reject these players with an error — the user
asked about him, he deserves an answer.

Angle 1 outranks everything for the same reason it does in the team feature: an injured regular is the most
conversation-worthy thing a squad holds and the fact a casual fan is least likely to know.

The club context (position, points, form, next fixture) goes into the data block **for every angle**, not just
the thin ones. It is what lets even angle 7 produce something worth saying — "yet to play a minute, mind you
they're top of the league and unchanged".

### 6.4 The factory

```java
public PromptBuilder create(PlayerPromptContext context, Language language)
// PlayerPromptContext: PlayerData player, SquadContext squadContext, TeamData team,
//                      Competition competition, Fixture nextFixture,
//                      List<MatchContribution> recentContributions   (empty until phase 3)
```

A third `create` overload on `PromptBuilderFactory`. Everything it needs is gathered by the caller, so like the
team overload it fetches nothing.

> **Watch out:** overloading `create` a second time will make Mockito's `create(any(), any())` ambiguous in
> existing stubs. The team feature hit this and fixed it by typing the first matcher —
> `any(TeamPromptContext.class)`, `any(Fixture.class)`. Any new or existing stub of this factory has to do the
> same, and `OneLinersServiceTest` / `TeamOneLinersServiceTest` may need updating when the overload lands.

---

## 7. Which competition is the sentence about?

Less fraught than for a team. A player has exactly one club, and the club's domestic league is the competition
his stats belong to — the same resolution `TeamOneLinersService.resolveCompetition` already implements: the
standing that is neither Champions League nor World Cup, most-played first.

Reuse that logic rather than copying it; it is currently private on `TeamOneLinersService` and should move
somewhere both services can call. Two differences in how the result is used:

- **No `competition` request parameter.** The player card shows his club's primary league; there is no
  meaningful "his Champions League one-liner" — the stored stats are one season-wide set (§4.2).
- **A national-team-only club is not a rejection here.** The team one-liner rejects a World-Cup-only team with a
  400, because league position is its entire subject. A player sentence can survive without a table: fall back to
  the player's own numbers and phrase the club context as unavailable. In practice this barely arises — national
  team entries carry no `players` array, so those players are not in the collection at all — but it should
  degrade rather than throw.

---

## 8. Service flow

New `PlayerOneLinersService`, alongside `OneLinersService` and `TeamOneLinersService`. A third service rather
than a method on an existing one, for the same reason the second one exists: the caching rule is its own.

```
getPlayerSmallTalk(playerId, lang)
  ├─ PlayerData player      = playerDataService.getPlayerById(playerId)          // → NotFoundException (404)
  ├─ List<PlayerData> squad = playerDataService.getPlayersByTeam(player.getTeamId())
  ├─ SquadContext squadCtx  = SquadContext.of(player, squad)                     // §4.3
  ├─ TeamData team          = teamDataService.findTeamById(player.getTeamId())   // null-tolerant, see below
  ├─ Competition target     = resolveCompetition(team)                           // §7, null-tolerant
  ├─ List<Fixture> form     = fixtureService.getRecentFinishedForTeam(teamId, 5) // club form for the data block;
  │                                                                              // phase 3 scans the same list for his goals
  ├─ Fixture next           = fixtureService.getNextFixtureForTeam(teamId).orElse(null)
  ├─ List<MatchContribution> recent = contributions(player, form)               // phase 3, by id; empty before that
  ├─ PlayerOneLiner cached  = player.findOneLiner(lang)                          // the set on PlayerData, §5
  │     ├─ fresh (§5)? → use it
  │     └─ stale?      → aiService.generate(factory.create(context, lang).buildPrompt()),
  │                      collapse to one line, player.replaceOneLiner(new), playerDataService.save(player)
  └─ return new PlayerSmallTalk(oneLiner, PlayerFacts.from(player, squadCtx, team, target, form, next, recent))
```

`playerDataService.save(player)` is a plain `repository.save` — a full document replace, which is fine because
the whole document was just loaded, one-liners included. The only writer that could race it is `TeamsJob` at
04:00 on Monday and Thursday; losing one cached sentence to that is harmless (it regenerates), so no locking.

One asymmetry worth stating: **`getTeamById` throws `NotFoundException`, and here that is the wrong behaviour.**
A player whose `teamId` no longer resolves — bug #11's departed player, or a club dropped from the tracked
leagues — should still get a sentence about himself with the club context omitted, not a 404 that says "no team
found" in response to a request about a player. Add a null-returning `findTeamById` (or an `Optional`-returning
one) alongside the throwing `getTeamById` rather than changing the existing method, which the team one-liner
depends on throwing.

`PlayerFacts` and `PlayerSmallTalk` live in `models/` as plain response shapes, serialised by the `@Primary`
`LOWER_CAMEL_CASE` mapper.

---

## 9. Files touched

**New**
```
controllers/PlayerController.java                        GET /players/teams/{teamId}
services/PlayerOneLinersService.java
models/PlayerOneLiner.java
models/PlayerFacts.java
models/PlayerSmallTalk.java
models/PlayerSummary.java                                the light picker shape
models/SquadContext.java                                 computed, §4.3
models/MatchContribution.java                            phase 3
system/utils/prompts/PlayerOneLinerPromptBuilder.java
system/utils/prompts/PlayerPromptContext.java
system/utils/prompts/PlayerAngleSelection.java
```

**Modified**
```
domain/PlayerData.java                       + Set<PlayerOneLiner> oneLiners (@Builder.Default, @JsonIgnore)
services/TeamDataService.java                savePlayers: replaceOne → $set upsert + null clearing + bug #11 removal + Competition param; + findTeamById (§8)
controllers/OneLinerController.java          + GET /one-liners/players/{playerId}
services/PlayerDataService.java              + getPlayerById, getSquadSummaries, save, reusable firstChoiceKeeper
repositories/PlayerDataRepository.java       + countByTeamId (the half-size guard, §10)
services/TeamOneLinersService.java           resolveCompetition made shareable (§7)
system/utils/prompts/PromptBuilderFactory.java   + player overload (watch the Mockito ambiguity, §6.4)
system/messages/Messages.java                + NO_PLAYER_FOUND
bugs.md                                      mark #11 fixed
```

```
models/dto/GoalscorerItem.java               + the four *_scorer_id / *_assist_id fields (phase 3)
models/Goal.java                             + scorerId, assistId (phase 3)
system/utils/mappers/FixtureAssembler.java   map the ids onto Goal (phase 3)
```

`FixtureService` needs nothing — `getRecentFinishedForTeam` / `getNextFixtureForTeam` suffice. No change to
`JwtAuthFilter` — both new routes are outside every `isJwtRequired*` branch — and no new properties, so
`src/test/resources/application.properties` is untouched.

---

## 10. Bug #11 — fixed in Phase 1, and how

`playerData` only ever grows: `savePlayers` writes the current squad and never reconciles it against what is
already stored, so a player who leaves the tracked leagues keeps his `teamId` and his final stats forever. Today
that is confined to a stale name inside a team card. **Once players are selectable by id, a departed player is a
first-class subject** — the picker lists him under his old club, and the feature confidently writes a sentence
about a player who has not been there since the summer. Decided: fix it as part of Phase 1 step 2.

**The fix, in full.** In `savePlayers`, after the per-player upserts are queued and before `execute()`:

```java
List<String> writtenIds = players.stream().map(PlayerData::getId).toList();
bulk.remove(Query.query(Criteria.where("teamId").is(teamDto.getTeamKey())
                                .and("_id").nin(writtenIds)));
```

`savePlayers(teamDto, scorerRankByPlayerId)` does not currently know which competition it is running for; the
domestic-only guard below needs it, so add a `Competition` parameter — the caller's loop already has it in hand.

That is: *"delete every stored player of this team who is not in the squad we just wrote."* It rides in the same
bulk as the writes, so it costs no extra round-trip, and because it is scoped to `teamId` it can only ever
remove players of the team being refreshed.

What it handles and why:

- **A player who left the tracked leagues** (abroad, retired, released) is absent from his old club's payload
  and gets removed. This is the case the bug is about.
- **A player who moved between two tracked clubs** already corrected itself — the new club's write rewrites the
  same `_id` with the new `teamId`. With the removal in place there is an ordering subtlety: if the *old* club is
  refreshed after the new one, its removal query matches on `teamId = old club`, which the player no longer has,
  so he is safe. If the old club is refreshed first, he is removed and then re-inserted by the new club's
  upsert. Either order ends correctly.
- **An empty payload removes nobody.** The method already returns early when `players` is empty — a national
  team legitimately has no squad, and an API bad day must not wipe a real one. The early return has to stay
  *above* the removal, and the step-2 test pins that.
- **A club refreshed by two competitions' calls.** Verified live (§4.2): both calls return the identical squad
  list, so the later call's removal has nothing to delete. Guard it anyway — run the removal only from the
  domestic-league call (skip it for `CHAMPIONS_LEAGUE` and `WORLD_CUP`) — so a future change to the API cannot
  make the two calls delete each other's players. One line.

**The downside — and the guard for it.** The removal trusts the payload. Today, a *missing* player in a
`get_teams` response means "not at this club any more"; after the fix it also means "delete him". So a
**partial** response — apifootball returning 18 of a 25-man squad on a bad day — deletes seven real players
until the next refresh three or four days later. The existing empty-squad early return catches the total
failure, not the partial one. While they are gone: the picker does not list them, the one-liner route 404s for
them, and their cached sentences (which live on the document) are lost and regenerated on return — a cost in
OpenAI calls, not correctness. The team card's notable-players list degrades to whoever is left.

Guard it with a proportion check rather than an absolute one: **skip the removal, and log a warning, when the
incoming squad is smaller than half the stored one for that team** (`countByTeamId` before the bulk — one cheap
count per team, 254 per run). A genuine squad rarely halves between Thursday and Monday; a truncated response
often does. The write still goes ahead, so nothing is lost by skipping — the stale players simply survive until
a full response arrives. Pin it with a step-2 test: a payload of 10 against 25 stored removes nobody. If the
Phase 4 smoke shows the API never truncates in practice, the threshold can stay as cheap insurance.

Two smaller consequences worth knowing, neither needing action:

- A player loaned out and back mid-season is deleted and later re-inserted with a fresh document — his old
  cached one-liner does not come back, which is correct, since his numbers restarted.
- A frontend holding a player id across a refresh can get a 404 it did not get before. That is the honest
  answer for a departed player, and the picker re-lists from the squad on the next visit.

What it does not handle, deliberately: a whole club dropping out of the tracked leagues (relegation from a
tracked league to an untracked one). Its players keep their documents because no refresh ever names that
`teamId` again. That is a `TeamData`-level reconciliation and out of scope here; note it in `bugs.md` as the
remaining Low-severity residue when marking #11 fixed.

---

## 11. Tests — reference index

**This is not a phase of work.** Every test below is assigned to the step that creates the code it covers, in the
build order at the top. This section exists so you can look up what covers a given component and so nothing is
dropped if a phase is split differently.

- *(phase 1, step 2)* `TeamDataServiceTest.SavingTeams` — the write is a `$set` upsert; a value that became null is
  cleared; absent players are removed; an empty payload removes nobody; a half-size payload removes nobody.
- *(phase 1, step 3)* `PlayerDataServiceTest` — `getPlayerById` hit and miss (`NotFoundException`); the squad
  summary carries only the picker fields.
- *(phase 1, step 4)* `PlayerControllerTest` (`@WebMvcTest`, `excludeFilters` for `JwtAuthFilter`) — the squad route,
  an empty squad. `JwtAuthFilterTest` — `GET /players/teams/{id}` is public.
- *(phase 2, step 5)* `PlayerOneLinerTest` — equality on language while ignoring `text`, mirroring
  `TeamOneLinerTest`. `PlayerDataTest` — add-vs-replace and the `@Builder.Default` trap, mirroring
  `TeamDataTest`. `TeamDataServiceTest.SavingTeams` — a stored one-liner survives a refresh.
- *(phase 2, step 6)* `PlayerAngleSelectionTest` — one case per angle in §6.3 plus the priority order between
  them; specifically that a fringe player and an unused player each still yield an angle.
  `PlayerOneLinerPromptBuilderTest` — the data block carries the player, the squad context, the club and its
  standing; a null club, a null standing, absent keeper stats and an empty contribution list all degrade to
  readable lines; the constraints forbid nationality, former clubs and career history.
- *(phase 2, step 7)* `PlayerOneLinersServiceTest` (Mockito) — cache hit when nothing moved; regeneration on each
  of the four snapshot fields independently; null `generatedAt` always regenerates; `replaceOneLiner` overwrites;
  independent entries per language; multi-line output collapsed; a player whose team no longer resolves still
  gets a sentence (§8).
  `OneLinerControllerTest` — a new `PlayerRoute` nested class: happy path, required `lang`, 404, and the
  snapshot fields not on the wire. `JwtAuthFilterTest` — `GET /one-liners/players/{id}` is public.
- *(phase 3, step 8)* `MatchDtoTest` — the four goal id fields bind. `FixtureAssemblerTest` — ids land on `Goal`,
  blank → null. A contributions test — counted per fixture for the right player only; id-less goals contribute
  nothing. A builder test that the contribution lines reach the data block and the empty case reads as absence.

Nothing here needs a Spring context except the two controller slices. As `CLAUDE.md` notes, there are still no
MongoDB integration tests in this project, so the `$set` upsert and the `bulk.remove` in `savePlayers` are only
exercised through a mocked `BulkOperations`. A wrong `Update` shape would surface only against a real database —
Phase 1's "done when" and the Phase 4 smoke both cover it.

---

## 12. Questions, notes and things to clarify

Answered 2026-09-13 unless marked open. Kept here rather than deleted so the reasoning survives.

### 12.1 Where the cached sentence lives — **decided: B, on `PlayerData`**

`savePlayers` does a full `replaceOne` per player, which would wipe anything on the document that did not come
from `get_teams`. Decision: keep the set on `PlayerData` for symmetry with `TeamData`, and change the write to a
`$set` upsert that excludes `oneLiners`. The mechanics, including the null-clearing trap this introduces, are in
§5. The same change is what makes a future `get_players` enrichment (§4.5)
possible, which is a good part of why B is the cleaner choice. A second live smoke after development is owed
because of it.

### 12.2 Perspective — **decided: one voice, no perspective**

§3 stands. `FAN` / `RIVAL_FAN` / `NEUTRAL` does not transfer to a player, and `RIVAL_FAN` pointed at a named
individual carries a content risk the team feature never had. If variety is wanted later, the `HYPE` /
`SCEPTIC` stance axis in §3 is the one that would actually change which facts get used, and it can be added
with no forward-compatibility cost.

### 12.3 The Champions League stat-scope check — **done: season-wide, nothing to build**

Run live on 2026-09-13 with the owner's key. All 152 player objects shared between the Premier League and
Champions League responses were byte-identical. Details and consequences in §4.2. The key was used from the
shell for the two calls and the dumps were deleted afterwards; nothing was written to the repository.

### 12.4 Bug #11 — **decided: fix it in Phase 1**

The fix is explained in full in §10, together with its one real downside — a truncated API response would
delete real players for a few days — and the half-size guard that covers it. It ships with step 2 because both
edit the same bulk write.

### 12.5 The team plan overstated what was built — **corrected**

`team-oneliner.md` §4.5 and §10 claimed `getPlayerById` / `getPlayersByName` and a `PlayerDto` had been added to
`FootballApiService` for this feature. They had not — none of the three exists — and this plan does not need
them (§4.5 defers `get_players`). Both places in the team plan now say so.

### 12.6 The "wrong coach" caveat — **withdrawn; the API was right**

The team plan's Phase 4 notes said apifootball returned a wrong coach for Liverpool (Andoni Iraola). It did not:
Iraola was appointed Liverpool head coach on 4 June 2026, succeeding Arne Slot — the model reviewing the smoke
output was working from stale training data, which is exactly the failure mode §6.2's constraints exist to stop,
just on the other side of the prompt. Corrected in the team plan. The general point survives in a weaker form:
when a sentence looks wrong during Phase 4, check the raw API response and a current source *before* treating it
as a prompt fault — and do not assume the reviewer's memory is more current than the feed.

### 12.7 The picker — **decided: team squad only, GET by id**

`GET /players/teams/{teamId}` is the only discovery route (§2.2). The name search is dropped.

### 12.8 Open — anything the picker should carry that §2.2's `PlayerSummary` does not?

It currently has `id`, `name`, `image`, `number`, `position`, `injured`, `matchesPlayed`. If the frontend wants
to show, say, goals next to each name in the list, that is a one-field addition — but it would be the first
place a stat appears outside the card. Cheaper to leave it out until asked for.

---

## State at handoff (2026-09-13)

- **Nothing has been implemented.** This document and the two corrections to `team-oneliner.md` are the only
  changes on `feature/player-oneliner`, and they are uncommitted. Commit them first (or alongside Phase 1).
- The suite is green at 393 tests; `./mvnw test` needs no Docker or network.
- The one live check this plan needed (§4.2) has been done; no key is needed until the Phase 1 "done when"
  (`POST /teams` timing against a real database) and the Phase 4 smoke. Have `API_FOOTBALL_KEY`, `MONGODB_URI`
  and `OPENAI_API_KEY` in the shell environment for those — never in the conversation or the repository.
- Each phase ends by appending a **"Phase N handoff notes"** section here, in the style of `team-oneliner.md`:
  what changed, the tests added, anything owed to the next phase, and where the next phase starts. The next
  session reads that section before anything else.
- Start Phase 1 at step 2 (`savePlayers`); step 1 is already closed.
- Every apifootball response shape the app uses, with live samples and the traps in each, is in
  `docs/football-api-responses.md`. Read it instead of making exploratory calls.

---

## Phase 1 handoff notes (for the Phase 2 session)

Phase 1 is complete on `feature/player-oneliner`; the suite is green at **407 tests** (+14). Nothing here is
AI-shaped; what changed is the ingestion write path and the player lookup.

### Step 2 — `savePlayers` (`TeamDataService`)

- The per-player write is now `bulk.upsert(Query on _id, Update)` where the `Update` is a `$set` of every key
  the mapping converter produces for the `PlayerData` (via `mongoTemplate.getConverter().write`), except `_id`.
  **`Update.fromDocument` was tried first and is wrong for this** — it expects a document already in operator
  form and puts plain keys at the top level of the update, so the fields are set one by one instead.
- **Null clearing** is automatic, not a hand-maintained list: every property of the `PlayerData` persistent
  entity (from `getConverter().getMappingContext().getRequiredPersistentEntity`) that is missing from the
  converted document gets `update.unset(field)`. A new field on `PlayerData` is therefore cleared correctly
  with no further change — **unless it is one the refresh must leave alone**, in which case it goes in
  `FIELDS_NOT_REFRESHED`, the one place both the `$set` and the `$unset` loops consult.
- **For Phase 2: add `"oneLiners"` to `FIELDS_NOT_REFRESHED`** when the set lands on `PlayerData`. That is the
  whole mechanism by which the cache survives the refresh; without it the `$unset` loop would clear the set on
  every run. The step-5 test "a stored one-liner survives a squad refresh" pins exactly this — assert that
  neither `$set` nor `$unset` in the captured `Update` contains `oneLiners`. Today the constant holds only
  `_id`.
- `savePlayers` gained a `Competition` parameter (the caller's loop already had it) for the domestic-only
  removal guard.
- The half-size guard uses `mongoTemplate.count(Query on teamId, PlayerData.class)` rather than a new
  `countByTeamId` on `PlayerDataRepository` — `TeamDataService` already holds the template and did not need a
  second repository, so the constructor (and every test that builds it) is unchanged. §9's "files touched" list
  is therefore one entry shorter than planned.
- Bug #11 is marked fixed in `bugs.md`, with the relegation residue noted there as planned.

### Step 3 — `PlayerDataService`

- `getPlayerById` throws `NotFoundException` with the new `Messages.NO_PLAYER_FOUND`.
- `getSquadSummaries(teamId)` returns `List<PlayerSummary>` ordered Goalkeepers → Defenders → Midfielders →
  Forwards → anything else, then by numeric shirt number with blank/non-numeric numbers last.
- `PlayerSummary` is a **Java record** in `models/`, with a static `from(PlayerData)`. It serialises through
  the `@Primary` mapper exactly as the Lombok `@Data` shapes do; a record was the smaller option for a type
  with no behaviour. Nothing else needed `firstChoiceKeeper` yet, so it is still private — make it reusable in
  Phase 2 when `SquadContext` needs it (§4.3).

### Step 4 — `PlayerController`

- `GET /players/teams/{teamId}` → `SmallTalkResponse<List<PlayerSummary>>`. No `JwtAuthFilter` change; the
  route is pinned as public in `JwtAuthFilterTest.Open`.

### Tests added (14)

`TeamDataServiceTest.SavingTeams` was rewritten around the new write shape (the `replaceOne` stubs are gone;
`expectABulkWrite(storedSquadSize)` stubs a **real `MappingMongoConverter`** on the mocked template so the
captured `Update` can be inspected): `$set` keyed on id, null clearing, scorer-rank backfill via the update,
absent players removed, empty payload removes nobody, half-size payload removes nobody, Champions League
refresh removes nobody. `PlayerDataServiceTest` gained `Lookup` and `SquadSummaries`. `PlayerControllerTest`
is new. `JwtAuthFilterTest.Open` has the new route.

### Also in this phase — an unrelated repair

`FixtureServiceTest.java` on `main` did not compile: the merge commit `16b7e06` had moved a 19-line block into
the middle of another test (visible in `git diff f8f7f7d 16b7e06 -- <file>`). It was restored from `f8f7f7d`,
the last commit before the merge. The "green at 393" figure in this plan predates that merge; the test tree
could not have been run on `main` as merged.

### Owed to the next session

- **The two live "done when" checks** — a real `POST /teams` completing in the same order of time as before,
  and `GET /players/teams/2611` listing a squad — were not run: `MONGODB_URI` / `API_FOOTBALL_KEY` were not in
  the shell. Run them at the start of Phase 2 (or the end), from a shell that has them. What to look at after
  `POST /teams`: a player whose `leagueScorerRank` was set last week and who is now unranked should have **no**
  `leagueScorerRank` field, and a `db.playerData.countDocuments()` should not exceed the sum of current squads.
- Phase 2 starts at step 5 (§5): `PlayerOneLiner`, the set on `PlayerData` with `@Builder.Default` +
  `@JsonIgnore`, and the `FIELDS_NOT_REFRESHED` entry above.
- The Phase 1 changes are **not committed** at the time of writing this section.

---

## Phase 2 handoff notes (for the Phase 3 session)

Phase 2 is complete on `feature/player-oneliner`; the suite is green at **461 tests** (+54).
`GET /one-liners/players/{playerId}?lang=` exists, is public, caches on `PlayerData`, and regenerates when any
of the four snapshot fields moves.

### Step 5 — the cache

- `models/PlayerOneLiner` keys on `language` only; the five snapshot fields are `@JsonIgnore`. `PlayerData`
  gained `oneLiners` (`@Builder.Default`, `@JsonIgnore`) with `findOneLiner(lang)` / `addOneLiner` /
  `replaceOneLiner`, mirroring `TeamData`.
- `"oneLiners"` is in `TeamDataService.FIELDS_NOT_REFRESHED`, and `TeamDataServiceTest.SavingTeams`
  pins that neither `$set` nor `$unset` touches it.
- **Test-harness trap found here:** the real `MappingMongoConverter` the `SavingTeams` tests build on the mocked
  template used a bare `MongoMappingContext`, which has no JSR-310 simple types and tried to map the
  `Instant` inside a nested one-liner as an entity (`InaccessibleObjectException` on `java.time.Instant`).
  The helper now registers `MongoCustomConversions` the way Boot does. Anything else that adds a
  `java.time` field to `PlayerData` gets this for free now.

### Step 6 — angle selection and the prompt

- `models/SquadContext` is a record with a static `of(player, squad)`. It carries `medianAppearances` as well
  as the §4.3 fields, because both the `INJURED` and `FRINGE` angles compare against the median and the angle
  selection only sees the player and the context. `firstChoiceKeeper` moved here from `PlayerDataService` as
  a public static, and `getNotablePlayers` now calls it — that is the "make it reusable" from Phase 1.
- `PlayerAngleSelection` has **eight** angles, not the seven in §6.3: `REGULAR` sits between `EVER_PRESENT`
  and `FRINGE`, because the table had a hole — a player on 6 of 10 appearances with no goals matched
  nothing. It is the "plays regularly, nothing stands out, lean on the club" case. `UNUSED` is checked
  before `EVER_PRESENT` so a squad where nobody has played does not make an unused player "ever present".
  Thresholds are at the top of the class and in `SquadContext`, marked as starting points for Phase 4.
- `PlayerPromptContext` has **no `recentContributions` field yet** — `MatchContribution` does not exist, and
  a field of a type that does not exist cannot be declared. Phase 3 adds both the type and the field together,
  and a `%s` block in `PlayerOneLinerPromptBuilder.data()` for the contribution lines.
- `PromptPhrasing.phraseNextFixture(fixture, teamId)` was lifted out of `TeamOneLinerPromptBuilder` so both
  builders share it; the team builder's behaviour is unchanged.
- The data block phrases absent numbers as `not recorded` and the constraints tell the model that means
  unknown, not zero. The examples cover the full range of angles, including the fringe and unused cases.

### Step 7 — service, shapes, route

- `PlayerOneLinersService.getPlayerSmallTalk(playerId, lang)` follows §8 exactly. `TeamDataService.findTeamById`
  returns `Optional<TeamData>`; the throwing `getTeamById` is untouched.
- `resolveCompetition` was **not** moved to a shared service: the domestic-league-most-played rule is now
  `TeamData.primaryCompetition()` (`Optional<Competition>`), and `TeamOneLinersService` handles only the
  "requested competition" branch and the World-Cup-only rejection on top of it. The player service uses the
  `Optional` directly and leaves the competition null rather than throwing (§7).
- `singleLine` is a public static on `AiService`; both one-liner services call it. It is a static rather than
  a wrapping `generateOneLine(...)` so the existing tests that stub `aiService.generate` still exercise it.
- `PlayerFacts` and `PlayerSmallTalk` are records in `models/`. `PlayerFacts.Season` echoes the stats flat
  with nulls preserved; `PlayerFacts.Club` is the four-field club stub; the next fixture reuses
  `TeamFacts.NextFixture`, whose helper now takes a team id instead of a `TeamData` so it works without a club.
  There is **no `recentContributions` field on `PlayerFacts`** — Phase 3 adds it alongside the context field.
- `OneLinerController` has the new route; `OneLinerControllerTest.PlayerRoute` and `JwtAuthFilterTest.Open`
  cover it. No filter change.

### Tests added (54)

`PlayerOneLinerTest` (5), `PlayerDataTest` (6), `TeamDataServiceTest.leavesTheCachedOneLinersAloneOnARefresh`,
`PlayerAngleSelectionTest` (11), `PlayerOneLinerPromptBuilderTest` (10), `PlayerOneLinersServiceTest` (16),
`OneLinerControllerTest.PlayerRoute` (4), one `JwtAuthFilterTest.Open` row.

### Owed to the next session

- **Three live checks**, all needing `MONGODB_URI`, `API_FOOTBALL_KEY` and (for the third) `OPENAI_API_KEY` in
  the shell: Phase 1's two (`POST /teams` timing and `GET /players/teams/2611`), and this phase's
  "done when" — `GET /one-liners/players/{id}?lang=BRITISH` twice, the second call returning the same
  `generatedAt`, then once more after editing one of the four snapshot fields on the stored document.
  Also confirm that a real `POST /teams` after that leaves the `oneLiners` array on the document in place.
- Phase 3 starts at step 8 (§4.4): bind the four id fields on `GoalscorerItem`, carry `scorerId` /
  `assistId` onto `Goal`, then add `MatchContribution`, the `recentContributions` field on both
  `PlayerPromptContext` and `PlayerFacts`, the join in `PlayerOneLinersService` over the `recentForm` list it
  already fetches, and the contribution lines in the builder's data block.
- The Phase 1 and Phase 2 changes are **not committed** at the time of writing this section.
