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
> questions for the owner; two of them (§12.1 and §12.2) should be answered before Phase 1 starts, because they
> decide where the cache lives and whether the stored stats are the right ones.
>
> **The single biggest risk in this feature is not the code — it is that the stored per-player stats may be
> Champions League stats rather than domestic ones.** See §4.2. Verify it early; it is a two-minute check with a
> live key and it changes the shape of Phase 1.

---

## Start here — build order, grouped into sessions

Nine steps in four phases, each phase independently verifiable and sized for one session. Open a fresh session
per phase rather than carrying the whole feature in one context. The opening prompt is literally
*"Read `.claude/docs/player-oneliner.md` and implement phase N."*

**Every phase ships its own tests.** They are listed inline per step, and a phase is not finished until they pass
alongside the existing suite (green at **393 tests** as of the team feature). §11 is a reference index of the
same list organised by component, not a separate stage of work. Conventions live in `CLAUDE.md`: plain
JUnit/Mockito with no Spring context wherever possible, `@WebMvcTest` with `excludeFilters` for controllers,
`JsonFixtures.parse` for anything deserialised from apifootball. Run `./mvnw test` (~15s, no Docker or network).

### Phase 1 — Discovery and player lookup (steps 1–3)

Nothing AI-shaped here. It answers the question the feature title takes for granted: *how does a user select a
player at all?* Today nothing exposes `playerData` over HTTP.

1. **Settle the competition-scope question (§4.2).** One live `get_teams` call for `league_id=152` and one for
   `league_id=3`, diffing a shared club's player stats. If they differ, apply the mitigation in §4.2 — skip the
   player write for `CHAMPIONS_LEAGUE` and `WORLD_CUP` in `TeamDataService.savePlayers` — before anything is
   built on top of the numbers.
   - **Tests:** if the mitigation lands, one `TeamDataServiceTest.SavingTeams` case asserting no player write
     happens for those two competitions while the team upsert still does.
2. **`PlayerDataService.getPlayerById`** returning the document or throwing `NotFoundException` (mirroring what
   `TeamDataService.getTeamById` became in the team feature), plus **`searchPlayers(name)`** and the light
   `PlayerSummary` response shape (§2.2).
   - **Tests:** `PlayerDataServiceTest` — a hit, a miss throwing `NotFoundException`, a case-insensitive partial
     name match, an empty query rejected rather than returning the whole collection.
3. **`PlayerController`** with `GET /players/teams/{teamId}` and `GET /players?name=` (§2.2). `/players` is not
   in any `isJwtRequired*` branch of `JwtAuthFilter`, so it is public with **no filter change** — the same
   reasoning that kept the team route off `/teams`.
   - **Tests:** `PlayerControllerTest` (`@WebMvcTest`, `excludeFilters` for `JwtAuthFilter`) — both routes, a 404
     for an unknown id, a 400 for a blank `name`. One `JwtAuthFilterTest.Open` assertion that `GET /players?...`
     is public, so a future filter edit cannot silently gate it.

**Done when:** the suite is green, `GET /players/teams/2611` lists a squad against a real database, and the
competition-scope question in §4.2 has a recorded answer.

### Phase 2 — The one-liner (steps 4–6)

The core. The traps here are the two the team feature already paid for once: a `Set` whose equality ignores its
text, and a builder-created collection with no `@Builder.Default`.

4. **The cache.** `PlayerOneLiner` value type, and the document it lives on (§5 — this is where §12.1's answer
   applies). Snapshot fields for staleness: appearances, goals, assists, injured, scorer rank.
   - **Tests:** `PlayerOneLinerTest` — equality on language (plus perspective only if §12.2 says so) while
     ignoring `text`, mirroring `TeamOneLinerTest`. Whichever container holds the set: add-vs-replace behaviour
     and a `@Builder.Default`-shaped test, mirroring `TeamDataTest`.
5. **`PlayerAngleSelection`, `PlayerPromptContext`, `PlayerOneLinerPromptBuilder`, and the factory overload**
   (§6). Implement the seven `PromptBuilder` methods; never a new template string.
   - **Tests:** `PlayerAngleSelectionTest` — one case per angle in §6.3, in priority order: an injured regular
     beats a scoring rank, a top-five rank beats being the squad's leading contributor, an ever-present defender
     with no goals still yields an angle, a fringe player yields the honest low-minutes angle, and a player with
     no appearances yields the "hasn't featured" angle rather than nothing.
     `PlayerOneLinerPromptBuilderTest` — the data block carries the player, his club and the club's standing;
     a null club, a null standing, absent keeper stats and an empty contribution history all degrade to readable
     lines rather than NPEs; the constraints forbid reaching for training data.
6. **`PlayerOneLinersService` and `GET /one-liners/players/{playerId}`** (§2.1, §9), with `PlayerFacts` /
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

### Phase 3 — Recent contributions from our own fixtures (step 7)

Separable on purpose: it is the one part that can fail on data quality rather than on code, and the feature is
shippable without it.

7. **`PlayerMatchContributions`** — scan the player's club's recent finished fixtures for goals and assists
   credited to him, and add them to the data block (§4.4). The join is **by name, not by id** — `Fixture.goals`
   carries `goalBy` / `assistBy` as free text — which is the same class of trap as the venue-side bug the team
   feature hit live, so the matcher must be conservative and a miss must read as "no data" rather than as "he
   has not scored".
   - **Tests:** a matcher test — an exact name matches; `"E. Haaland"` matches `"Erling Haaland"`; two players
     sharing a surname in the same squad match **neither** (ambiguity is a miss, not a guess); an unrelated name
     does not match; an empty goals list is fine. Plus one builder test that the contribution lines reach the
     data block and that an empty result phrases as absence of data rather than a zero.

**Done when:** the suite is green and, against a real database, a known scorer's recent goals show up in his card
while a defender's card shows no invented ones.

### Phase 4 — Live smoke and tuning (steps 8–9)

8. **Manual smoke** against a real database and a real OpenAI key, across the full range of player types: a
   league-leading striker, a first-choice goalkeeper, an ever-present centre-back, an injured regular, a fringe
   squad player, and a player with zero appearances. Read the actual output and tune `examples()`,
   `constraints()` and the angle thresholds from what comes back. **This is where the feature is made good** —
   the prompt text in §6 is a starting point, not a finished artefact.
9. **Record what the smoke found** in a handoff section at the end of this document, as each previous phase did,
   including any defect it turned up and the tests that now pin it.

**Done when:** all six player types produce a sentence you would actually say out loud, and none of them makes a
claim the data does not support.

### Later

- `get_players?player_id=` for nationality, birthdate and minutes played (§4.5) — deferred, with the reason.
- Bug #11 (`playerData` never loses a departed player) becomes materially worse once players are directly
  selectable. See §10.

---

## 1. What already exists (and what we reuse)

| Concern | Existing code | Reuse |
|---|---|---|
| The player collection | `domain/PlayerData`, `PlayerDataRepository`, `PlayerDataMapper` | As is — 5,492 documents live, refreshed Mon/Thu by `TeamsJob` |
| Squad reads and ranking | `PlayerDataService.getPlayersByTeam` / `getNotablePlayers` | Extend with `getPlayerById` and a name search |
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

`lang` is required, matching both existing one-liner routes. On perspective, see §3 — the recommendation is that
there is no `perspective` parameter here.

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

Do **not** return the `PlayerData` document directly. If §12.1 resolves in favour of embedding the cache on it,
the cached sentences would leak into the body — the same reason `TeamFacts` exists rather than returning
`TeamData`.

### 2.2 Finding a player

The feature says "a user selects a player", and today there is no way to. Two read-only routes on a new
`PlayerController` (`@RequestMapping("players")`), both public because `/players` matches no `isJwtRequired*`
branch:

```
GET /players/teams/{teamId}      → List<PlayerSummary>, the squad, for a team → player drill-down
GET /players?name=hal            → List<PlayerSummary>, a case-insensitive partial-name search
```

`PlayerSummary` is a light shape — `id`, `name`, `image`, `number`, `position`, `teamId`, `teamName` — not the
25-field document. The picker does not need the stats and the card fetches them anyway.

Search is a derived `findByNameContainingIgnoreCase`, which Spring Data translates into a case-insensitive
regex. Across ~5,500 documents that is an unindexed collection scan; acceptable at this size, and an `@Indexed`
on `name` can be added later — note it would be inert under tests, which set
`spring.data.mongodb.auto-index-creation=false`. Reject a blank or one-character `name` with a
`SmallTalkException` (400) rather than returning the whole collection, and cap the result at a sane limit.

---

## 3. Perspective — the recommendation is not to have one

**You are right, and here is the sharper version of why.** `Perspective` on the team one-liner works because the
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

Recorded as a question in §12.2 in case you want the two-value axis in v1 after all.

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

### 4.2 ⚠ Which competition are these stats from?

**This must be checked before Phase 1 builds anything on the numbers, and it may already be affecting the team
one-liner.**

`TeamDataService.saveCompetitionTeams` loops `Competition.values()` and calls `get_teams&league_id=` once per
competition, writing every returned player with `bulk.replaceOne(..., upsert)` keyed on `player_id`. A club in
the Champions League is returned by **two** of those calls — its domestic league and `league_id=3` — and
`CHAMPIONS_LEAGUE` is **last** in the enum's declaration order. So if apifootball scopes the player statistics to
the requested league, the stored numbers for every Champions League club are its *Champions League* numbers: a
handful of matches, a goal or two, and a `leagueScorerRank` from a different competition's charts.

The team one-liner would already be quietly wrong for those clubs, which is a good reason to check regardless of
this feature.

**The check** (two minutes with a live key):

```
GET https://apiv3.apifootball.com/?action=get_teams&league_id=152&APIkey=...   # Premier League
GET https://apiv3.apifootball.com/?action=get_teams&league_id=3&APIkey=...     # Champions League
```

Find the same club in both and diff one well-known player's `player_match_played` and `player_goals`. Same
numbers ⇒ the stats are season-wide and there is nothing to do. Different numbers ⇒ they are league-scoped.

**If they are league-scoped**, the minimal mitigation is to skip the player write for `CHAMPIONS_LEAGUE` and
`WORLD_CUP` in `savePlayers` — a domestic-league squad is the one users mean, national-team entries carry no
players anyway, and every tracked club appears in exactly one domestic league. The team upsert itself must keep
running for those competitions, since standings are keyed per competition and are correct as they are. Record the
answer in this document either way; do not leave it as folklore.

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

**The catch, and it is the whole reason this is its own phase:** `Goal.goalBy` is a **free-text name** from
`get_events`, while `PlayerData.name` comes from `get_teams`. There are no ids on either side of that join. The
team feature was bitten by precisely this shape of assumption — it compared team *names* across the two endpoints
and got "Manchester United" vs "Manchester Utd", which inverted a fixture's venue and made the prompt name the
team as its own opponent.

So the matcher must be conservative and must fail closed:

- exact case-insensitive match on the full name;
- otherwise surname plus a matching first initial (`"E. Haaland"` ⇄ `"Erling Haaland"`);
- **if two players in the same squad would both match, match neither.** An ambiguous match is a miss, never a
  guess.
- a miss phrases as *"no goal or assist data available for his recent matches"*, never as *"no goals in his last
  five"*. Those are different claims and only one of them is supported.

If live data shows the matching is unreliable, this step can be dropped without touching anything else — which is
why it is late in the build order rather than woven into Phase 2.

### 4.5 `get_players` — deferred, with the reason

`action=get_players&player_id=` is the only source of `player_birthdate`, `player_country` and `player_minutes`.
Nationality in particular would improve the sentence ("the Norwegian is..."), and it is a single cheap call.

Two things make it a poor fit for v1:

1. **The team plan claims `getPlayerById` / `getPlayersByName` and a `PlayerDto` were already added to
   `FootballApiService` "for the player one-liner". They were not** — none of them exists in the codebase
   (`.claude/docs/team-oneliner.md` §4.5 and §10 are wrong about this). Anyone reading that section and assuming
   the groundwork is done will be surprised. Building it is a small job, but it is a job, not a given.
2. **Anything fetched lazily and written back onto `PlayerData` is wiped twice a week.** `savePlayers` does a
   full `replaceOne` per player, so a nationality backfilled at request time survives until Monday at 04:00. That
   is the same interaction §5 has to solve for the cached sentence, and solving it twice for a nice-to-have is
   not worth it in v1.

Revisit once §12.1 has settled where per-player derived data lives; at that point the enrichment has an obvious
home and the call is worth making.

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
    // equals/hashCode on language only (plus a stance, if §12.2 adds one) — text is deliberately excluded
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

**Where the set lives is an open decision — §12.1.** The recommendation is a small separate document keyed on the
player id, because `savePlayers` replaces the whole `PlayerData` document twice a week and would wipe an embedded
set. The alternative — changing that bulk write from `replaceOne` to an `updateOne` with `$set` so extra fields
survive — is tidier and symmetric with `TeamData`, but it edits a hot, already-perf-tuned ingestion path
(commit `939a5bf`, which took the run from eleven minutes down) for the benefit of a cache. Additive beats
symmetric here, but it is your call.

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
  meaningful "his Champions League one-liner" while the stored stats are single-valued (§4.2).
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
  ├─ PlayerData player   = playerDataService.getPlayerById(playerId)      // → NotFoundException (404)
  ├─ List<PlayerData> squad = playerDataService.getPlayersByTeam(player.getTeamId())
  ├─ SquadContext context   = SquadContext.of(player, squad)              // §4.3
  ├─ TeamData team          = teamDataService.findTeamById(player.getTeamId())   // null-tolerant, see below
  ├─ Competition target     = resolveCompetition(team)                    // §7, null-tolerant
  ├─ Fixture next           = fixtureService.getNextFixtureForTeam(teamId).orElse(null)
  ├─ List<MatchContribution> recent = contributions(player, teamId)       // phase 3; empty list before that
  ├─ PlayerOneLiner cached  = cache.find(playerId, lang)
  │     ├─ fresh (§5)? → use it
  │     └─ stale?      → aiService.generate(factory.create(context, lang).buildPrompt()),
  │                      collapse to one line, replace, save
  └─ return new PlayerSmallTalk(oneLiner, PlayerFacts.from(player, context, team, target, next, recent))
```

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
controllers/PlayerController.java                        GET /players/teams/{teamId}, GET /players?name=
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
system/utils/PlayerNameMatcher.java                      phase 3, §4.4
domain/PlayerOneLinerCache.java + its repository         only if §12.1 picks the separate-collection option
```

**Modified**
```
controllers/OneLinerController.java          + GET /one-liners/players/{playerId}
services/PlayerDataService.java              + getPlayerById, searchPlayers, reusable firstChoiceKeeper
repositories/PlayerDataRepository.java       + findByNameContainingIgnoreCase
services/TeamDataService.java                + findTeamById (null-tolerant, §8); savePlayers skip (§4.2, if needed)
services/FixtureService.java                 nothing — getRecentFinishedForTeam / getNextFixtureForTeam suffice
system/utils/prompts/PromptBuilderFactory.java  + player overload (watch the Mockito ambiguity, §6.4)
system/messages/Messages.java                + NO_PLAYER_FOUND, INVALID_PLAYER_SEARCH
domain/PlayerData.java                       + the one-liner set, only if §12.1 picks the embedded option
```

No change to `JwtAuthFilter` — both new route groups are outside every `isJwtRequired*` branch — and no new
properties, so `src/test/resources/application.properties` is untouched.

---

## 10. Bug #11 gets worse with this feature

`playerData` only ever grows: `savePlayers` writes the current squad and never reconciles it against what is
already stored, so a player who leaves the tracked leagues keeps his `teamId` and his final stats forever.

Today that is Medium severity because the damage is confined to a stale name appearing in a team card. **Once
players are directly selectable and searchable, a departed player becomes a first-class subject** — he will show
up in `GET /players?name=`, he will resolve by id, and the feature will confidently produce a sentence about a
player who has not been at that club since the summer, complete with his old club's league position.

This plan does not fix it — the fix belongs with the ingestion path, and `bugs.md` already records a good one
(one `bulk.remove` riding along with the write, skipping the empty-squad case so a bad API day cannot wipe a
squad). But it is worth doing **before** this feature ships, not after, and it is a small change. Raised in §12.4.

---

## 11. Tests — reference index

**This is not a phase of work.** Every test below is assigned to the step that creates the code it covers, in the
build order at the top. This section exists so you can look up what covers a given component and so nothing is
dropped if a phase is split differently.

- *(phase 1, step 1)* `TeamDataServiceTest.SavingTeams` — no player write for `CHAMPIONS_LEAGUE` / `WORLD_CUP`
  while the team upsert still happens (only if §4.2's check says the stats are league-scoped).
- *(phase 1, step 2)* `PlayerDataServiceTest` — `getPlayerById` hit and miss (`NotFoundException`); a
  case-insensitive partial name search; a blank query rejected.
- *(phase 1, step 3)* `PlayerControllerTest` (`@WebMvcTest`, `excludeFilters` for `JwtAuthFilter`) — both routes,
  404 for an unknown id, 400 for a blank search. `JwtAuthFilterTest` — `GET /players?name=x` is public.
- *(phase 2, step 4)* `PlayerOneLinerTest` — equality on language while ignoring `text`, mirroring
  `TeamOneLinerTest`. Container test for add-vs-replace and the `@Builder.Default` trap, mirroring
  `TeamDataTest`.
- *(phase 2, step 5)* `PlayerAngleSelectionTest` — one case per angle in §6.3 plus the priority order between
  them; specifically that a fringe player and an unused player each still yield an angle.
  `PlayerOneLinerPromptBuilderTest` — the data block carries the player, the squad context, the club and its
  standing; a null club, a null standing, absent keeper stats and an empty contribution list all degrade to
  readable lines; the constraints forbid nationality, former clubs and career history.
- *(phase 2, step 6)* `PlayerOneLinersServiceTest` (Mockito) — cache hit when nothing moved; regeneration on each
  of the four snapshot fields independently; null `generatedAt` always regenerates; `replaceOneLiner` overwrites;
  independent entries per language; multi-line output collapsed; a player whose team no longer resolves still
  gets a sentence (§8).
  `OneLinerControllerTest` — a new `PlayerRoute` nested class: happy path, required `lang`, 404, and the
  snapshot fields not on the wire. `JwtAuthFilterTest` — `GET /one-liners/players/{id}` is public.
- *(phase 3, step 7)* `PlayerNameMatcherTest` — exact match; initial-plus-surname match; two squad-mates sharing
  a surname match neither; an unrelated name misses; an empty goals list is fine. Plus a builder test that a miss
  phrases as missing data rather than as zero.

Nothing here needs a Spring context except the two controller slices. As `CLAUDE.md` notes, there are still no
MongoDB integration tests in this project, so `findByNameContainingIgnoreCase` has the same caveat the team
feature's derived queries had: a mis-resolved query returns empty rather than failing. Check it once against a
real database — Phase 1's "done when" covers it.

---

## 12. Questions, notes and things to clarify

### 12.1 Where should the cached sentence live? *(decide before Phase 2)*

`savePlayers` does a `bulk.replaceOne(..., upsert)` per player, so **anything stored on `PlayerData` that did not
come from `get_teams` is wiped every Monday and Thursday at 04:00** — including a cached one-liner. Two options:

- **A — a separate small collection** (`PlayerOneLinerCache`, `@Id` = player id, holding the set). Purely
  additive, zero blast radius on the ingestion path, and the twice-weekly refresh cannot touch it. Costs one
  extra read per request and breaks the symmetry with `TeamData`, which embeds its one-liners.
- **B — change `savePlayers` from `replaceOne` to `updateOne` with `$set`** of the mapped fields, so extra fields
  on the document survive a refresh. Then `PlayerData` carries its one-liners exactly as `TeamData` does, and the
  §4.5 nationality enrichment gets a home too. But it edits the hot bulk-write path that commit `939a5bf` tuned
  down from eleven minutes, for the benefit of a cache.

**My recommendation is A**, on the grounds that additive beats symmetric when the thing being edited is
ingestion. If you would rather have the symmetry — and B is the option that makes §4.5 easy later — say so and
the plan changes in one place.

### 12.2 Perspective — do you want the two-value stance axis in v1?

§3 argues for no perspective at all in v1, and I think you are right that `FAN` / `RIVAL_FAN` / `NEUTRAL` does
not transfer to a player: the three would reach for the same facts and differ only in adjectives, and
`RIVAL_FAN` pointed at a named individual is the one mode with a real content risk attached.

The axis that *would* vary the facts is `HYPE` / `SCEPTIC` (§3). It is a genuine difference and it is cheap to
add later with no forward-compatibility cost. Ship one voice unless you want the two now.

### 12.3 The Champions League stat-scope question *(§4.2)*

I could not check this without making a live call with your key, so it is written up as Phase 1 step 1. It is the
one thing in this plan that could invalidate the numbers the whole feature rests on — **and it may already be
affecting the team one-liner**, since every Champions League club's stored player stats would be its European
ones. If you have a feel for whether apifootball scopes `get_teams` stats by `league_id`, that answer saves the
check.

### 12.4 Bug #11 — fix it before this ships?

Departed players are already in `playerData` and never leave (§10). Today they can only turn up inside a team
card; once this feature ships they are directly searchable and selectable by id, and the app will write a
confident sentence about a player who left in the summer. `bugs.md` already carries a good one-line fix. My
suggestion is to do it as part of Phase 1 rather than leaving it to a later cleanup — it is small, and this
feature is what makes it visible.

### 12.5 Note — the team plan overstates what was built

`.claude/docs/team-oneliner.md` §4.5 and §10 both say `getPlayerById` / `getPlayersByName` and a `PlayerDto`
"have been added to `FootballApiService`... for the player one-liner". **They have not been** — none of the three
exists in the codebase. Nothing depends on them in this plan (§4.5 defers `get_players` entirely), but anyone
starting from that document will expect groundwork that is not there. Worth a correction to that file at some
point — I have not touched it this session.

### 12.6 Note — a data caveat that will resurface

The team feature's live smoke found that apifootball returns wrong coaches for some clubs (Liverpool's was listed
as Andoni Iraola). The same class of problem will show up here, with more force: a wrong shirt number, a wrong
position or a stale injury flag on a named player reads as a mistake by the app rather than by the feed. There is
no fix at our end and no amount of prompt tuning will help — but during Phase 4 it is worth checking a handful of
players you know well, so that the distinction between "our bug" and "their data" is established before anyone
debugs it as a prompt fault.

### 12.7 Open — is a player search over 5,500 documents the right picker?

`GET /players?name=` is an unindexed regex scan. It is fine at this size and easy to index later, but it assumes
the frontend flow is *type a name*. If the flow is really *pick a league → pick a team → pick a player*, then
`GET /players/teams/{teamId}` alone is enough and the search route can be dropped from Phase 1. Both are in the
plan because I do not know which the app does — tell me and one of them comes out.
