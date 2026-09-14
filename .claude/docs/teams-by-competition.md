# Teams by competition — implementation plan — ✅ DONE (2026-09-14)

The one backend piece the frontend's Teams page needs and does not have: a public way to **list the teams of a
competition**. The intended app flow (owner, 2026-09-13) is

```
Teams page (teams grouped by competition)
  → team page: the team one-liner (shown by default or on a tap) and the squad beneath it
    → player card: the player one-liner, generated on a tap
```

Everything from the team page down already exists (`GET /one-liners/teams/{id}`, `GET /players/teams/{id}`,
`GET /one-liners/players/{id}` — see `.claude/docs/frontend-handoff-oneliners.md`). The first screen has no
endpoint: `TeamController` holds only admin `POST` / `PATCH` / `DELETE`, and `JwtAuthFilter.isJwtRequiredTeams`
gates **every** method under `/teams`. The frontend handoff previously told the FE to harvest team ids from
fixtures, which is a workaround, not a Teams page.

Opening prompt for the implementing session: *"Read `.claude/docs/teams-by-competition.md` and implement it."*
It is one phase; it should fit comfortably in one session.

---

## 1. What already exists (and what we reuse)

| Concern | Existing code | Reuse |
|---|---|---|
| Which competitions exist | `Competition` enum (the whitelist), `GET /competitions` → `CompetitionData` (league id, name, logo, country, season) | As is; add one derived field (§3.3) |
| Which competition a team plays in | `TeamData.standings` — a `Map<Competition, Standing>` whose **keys are the competitions the team has a table in**, filled by `StandingsJob` / `PATCH /teams/standings` | The query key (§3.1). No new field on `TeamData` |
| A team's place in that table | `Standing.position`, `Standing.points` | Sort key and two display fields |
| Light response shapes | `PlayerSummary` (record with a static `from`) | Same pattern: `TeamSummary` |
| Route gating | `JwtAuthFilter` hard-codes URI/method rules | Narrow the `/teams` rule to non-`GET` (§3.4) |

There is no `competitions` field on `TeamData` and none is needed: a team is "in" a competition exactly when
it has a standing for it, and that is also the only case in which the Teams page can lead anywhere useful —
the team one-liner rejects a club with no standing.

---

## 2. Endpoint

```
GET /teams?competition=PREMIER_LEAGUE      → SmallTalkResponse<List<TeamSummary>>
```

`competition` is **required** (a `Competition` enum name). The list is the competition's table order:
sorted by `position` ascending. Public — no JWT.

```jsonc
{
  "data": [
    { "id": "141", "name": "Arsenal FC",       "crest": "https://.../141_arsenal-fc.jpg",       "position": 1, "points": 12 },
    { "id": "80",  "name": "Manchester City",  "crest": "https://.../80_manchester-city.jpg",   "position": 2, "points": 12 },
    ...
  ],
  "systemMessage": { "messageText": null, "error": false }, "jwt": null, "statusCode": 200
}
```

- A competition with no standings loaded yet returns `200` and `[]`, not an error — the same convention as
  `GET /players/teams/{id}` for an unknown team.
- An unknown `competition` value is Spring's default `400` (enum conversion failure), the same non-envelope
  body a missing `lang` produces on the one-liner routes. The FE only ever sends enum names from
  `GET /competitions`, so this is a programming error, not a user-facing case.
- `WORLD_CUP` works mechanically (national teams have a `WORLD_CUP` standing) but leads nowhere: the team
  one-liner rejects national teams with `TEAM_HAS_NO_LEAGUE_STANDING`. The FE should not offer it as a tab;
  the endpoint does not special-case it.

Why `GET /teams?competition=` rather than `/teams/competitions/{competition}`: it is the plain REST shape for
"the teams collection, filtered", it reads naturally next to the existing admin `POST /teams`, and the filter
change it needs (§3.4) is the one we would want anyway — `GET` on a public read collection should never have
been admin-only.

---

## 3. Steps

### 3.1 `TeamDataRepository.findByCompetition`

A single `@Query` on the map key, sorted by that competition's position:

```java
@Query(value = "{ 'standings.?0': { $exists: true } }", sort = "{ 'standings.?0.position': 1 }")
List<TeamData> findByCompetition(Competition competition);
```

`?0` binds the enum as its `name()` — the same string the `EnumMap` is stored under, because the mapping
converter writes enum map keys by name. This is the one thing to verify against a real database when the
phase is done (§5), since there are still no MongoDB integration tests.

### 3.2 `TeamSummary` and `TeamDataService.getTeamsByCompetition`

`models/TeamSummary` — a record `(String id, String name, String crest, int position, int points)` with a
static `from(TeamData team, Competition competition)` that reads the two numbers from
`team.getStandings().get(competition)`. The service method is one line over the repository call.

### 3.3 `CompetitionData.getCompetition()` — the enum name on the competitions list

`GET /competitions` returns `leagueId: 152`, not `PREMIER_LEAGUE`, so the FE could not build the tab bar
without a hard-coded id→enum table. Add a derived, Jackson-only getter on `CompetitionData`:

```java
public Competition getCompetition() { return Competition.fromCode(leagueId); }
```

Spring Data maps fields, not getters, so nothing is persisted; Jackson serialises it as `"competition":
"PREMIER_LEAGUE"`. Every stored `CompetitionData` came from `Competition.values()`, so `fromCode` cannot
throw here.

### 3.4 `TeamController.getTeams` and the filter

```java
@GetMapping
@ResponseStatus(HttpStatus.OK)
public SmallTalkResponse<List<TeamSummary>> getTeams(@RequestParam Competition competition)
```

and in `JwtAuthFilter`:

```java
private static boolean isJwtRequiredTeams(String uri, String method) {
    return uri.startsWith("/teams") && !GET.equals(method);
}
```

`isAdminOnlyRequest` has its own `/teams` rule — check it the same way so that a `GET` is neither
JWT-required nor admin-only. `OPTIONS` is already passed through before either check.

### 3.5 Tests

- `TeamDataServiceTest.Lookup` — `getTeamsByCompetition` maps position and points from the requested
  competition's standing (a two-table club must report its Premier League position when asked for
  `PREMIER_LEAGUE` and its Champions League position when asked for `CHAMPIONS_LEAGUE`); an empty repository
  result is an empty list.
- `TeamControllerTest` — **new** (`@WebMvcTest`, `excludeFilters` for `JwtAuthFilter`, like
  `PlayerControllerTest`): the route returns the summaries in the order the service gave them; a missing
  `competition` is a 400.
- `JwtAuthFilterTest` — move `GET,/teams` from `RequiresToken` and `RequiresAdmin` into `Open`; keep
  `POST,/teams`, `PATCH,/teams/standings` and `DELETE,/teams` where they are. This is the test that stops a
  future filter edit silently re-gating the Teams page.
- `CompetitionDataControllerTest` or a one-line serialisation test — `GET /competitions` carries
  `"competition": "PREMIER_LEAGUE"` next to `leagueId: 152`.

---

## 4. Files touched

**New**
```
models/TeamSummary.java
src/test/.../controllers/TeamControllerTest.java
```

**Modified**
```
repositories/TeamDataRepository.java        + findByCompetition (@Query on the map key)
services/TeamDataService.java               + getTeamsByCompetition
controllers/TeamController.java             + GET /teams?competition=
domain/CompetitionData.java                 + getCompetition() (Jackson-only)
security/JwtAuthFilter.java                 /teams gated for non-GET only (both the JWT and the admin rule)
src/test/.../security/JwtAuthFilterTest.java    GET /teams moves to Open
src/test/.../services/TeamDataServiceTest.java  Lookup cases
.claude/docs/frontend-handoff-oneliners.md  §1 and §2 already describe the endpoint as planned; flip the
                                            "not yet live" callout when this ships
```

No new properties, so `src/test/resources/application.properties` is untouched.

---

## 5. Done when

- The suite is green (475 before this plan).
- Against a real database with standings loaded: `GET /teams?competition=PREMIER_LEAGUE` returns the league
  in table order with no JWT, `GET /teams?competition=CHAMPIONS_LEAGUE` returns the clubs in that table with
  their Champions League positions, and `POST /teams` without a JWT is still a 401.
- `GET /competitions` shows `"competition": "PREMIER_LEAGUE"` on the Premier League entry.
- The callout in `.claude/docs/frontend-handoff-oneliners.md` §2 is flipped from "planned" to live, and the
  captured JSON sample there is replaced with a real one.

---

## 6. Questions and notes

- **Table order is the default and only order.** A Teams page that reads like the league table is more useful
  to this audience than alphabetical, and it costs nothing. If an alphabetical option is ever wanted it is a
  client-side sort.
- **Should the one-liner be fetched automatically on the team page?** Owner's call, either is supported.
  The first request per (team, language, perspective) costs 2–5 s; after that it is instant. Auto-fetch with a
  loading state is fine for a page the user navigated to deliberately; a tap is fine too. Recommend auto for
  the team (one call per page) and a tap for players (a squad has 25–30 of them).
- **`findByCompetition` ordering when positions tie** is undefined; ties are rare (points decide position in
  the feed) and harmless.
- **Standings must be loaded** for the endpoint to return anything. On the test database they were not until
  `PATCH /teams/standings` was run in the player one-liner's Phase 4; production runs `StandingsJob` three
  times a day.

---

## Handoff notes

Implemented on `feature/teams-by-competition`; suite green at **481 tests** (+6). Everything in §3 landed as
planned with one deviation:

- **The `sort` attribute of `@Query` does not substitute `?0`.** Against the real database the filter
  (`standings.?0 $exists`) bound correctly — 20 Premier League, 36 Champions League, 20 La Liga clubs, the
  right ones — but the list came back in storage order. `findByCompetition` is now unsorted and
  `TeamDataService.getTeamsByCompetition` sorts by `TeamSummary.position`; pinned by
  `TeamDataServiceTest.Lookup.ordersTheTeamsByTablePosition`, which hands the documents back out of order.
- `JwtAuthFilter` gained a `GET` constant; `isJwtRequiredTeams(uri, method)` is used by both the JWT rule and
  the admin rule, so the two cannot drift.
- Live, after the fix: `GET /teams?competition=PREMIER_LEAGUE` → positions 1…20 in order, `CHAMPIONS_LEAGUE`
  → 1…36; `POST /teams` without a JWT → 401; `GET /competitions` carries `"competition": "PREMIER_LEAGUE"`.
- The handoff (`frontend-handoff-oneliners.md` §1.1, §1.2) now shows the captured samples and no longer says
  "planned". One thing it flags for the FE: team names on this endpoint come from the standings feed and can
  differ from the fixture/one-liner names ("Arsenal FC" vs "Arsenal") — key on `id`.

Tests added (6): `TeamControllerTest` (2), `CompetitionDataControllerTest` (1), `TeamDataServiceTest.Lookup`
(3). `JwtAuthFilterTest` moved `GET /teams` from `RequiresToken` and `RequiresAdmin` into `Open`.

### Revised after review (2026-09-14): `GET /teams` returns everything, like `GET /fixtures`

The owner pointed out that the FE displays fixtures from one `GET /fixtures` call that returns
`{ competitions, fixtures }` and groups client-side, and wants the Teams page to work the same way. So the
`?competition=` filter (§2, §3.1) was replaced before merging:

- `GET /teams` takes **no parameter** and returns `TeamsResponse(List<Competition> competitions,
  List<TeamSummary> teams)`; `TeamSummary` gained a `competition` field. Every team becomes one row per
  standing it holds, sorted by competition (enum order, as `getFixtures` sorts) then position;
  `competitions` is the distinct list in that order.
- **`WORLD_CUP` rows are excluded** server-side — the one place this deviates from `getFixtures`. The team
  one-liner refuses national teams, so listing them would put a dead end on the Teams page.
- `TeamDataRepository.findByCompetition` is gone; `getTeams` is a `findAll` plus a flatMap over the standings
  map, ~130 rows. No query on the map key is needed any more, so the "sort placeholder" finding above is moot.
- Tests: `TeamDataServiceTest.Lookup` now covers the one-row-per-table shape, World Cup exclusion and a team
  with no standings; `TeamControllerTest` checks the `{competitions, teams}` shape. Suite at **479**.
- The handoff §1.2 describes the new shape.
