# apifootball.com — response reference

Every call the app makes to `https://apiv3.apifootball.com/`, with a **live sample of the JSON it returns**,
what the app binds from it, and the traps each one carries. Samples were fetched on 2026-09-13 and trimmed to
one element per array; field order and values are verbatim. Trust this file over the published apifootball
documentation, which is wrong about several of these endpoints (see the traps).

All calls are `GET`, take `APIkey=<key>` plus `action=<name>`, and are made only from
`services/FootballApiService.java` through `ResponseHandler.process(...)`, which never throws. Every value in
every response is a **string** unless noted — including numbers, booleans (`"Yes"`/`"No"`, `"1"`/`""`), and
ids. The one exception is `player_key`, which is a JSON number where it appears.

Deserialisation uses the `@Qualifier("apiClient")` `ObjectMapper` (`SNAKE_CASE`). A field whose Java name does
not round-trip through that strategy — anything with a capital in the wire name (`overall_league_W`), or a
getter that drifts from the field — needs an explicit `@JsonProperty("<wire name verbatim>")`; the strategy is
**not** applied to an explicit annotation.

| Action | Used by | DTO | Since |
|---|---|---|---|
| [`get_leagues`](#get_leagues) | `getCompetitionData()` | `CompetitionDto` | original |
| [`get_events`](#get_events) | `getMatches(from, to)` — one call per `Competition` | `MatchDto` | original |
| [`get_teams`](#get_teams) | `getTeamDataList(competition)` | `TeamDataDto` → `PlayerItem`, `VenueDto`, `CoachesItem` | original; `players`/`venue` bound for the team one-liner |
| [`get_topscorers`](#get_topscorers) | `getTopScorers(competition)` | `TopScorerItem` | team one-liner |
| [`get_standings`](#get_standings) | `getCompetitionStandings(competition)` | `StandingsDtoItem` | original |
| [`get_H2H`](#get_h2h) | `getHeadToHeadData(firstTeamId, secondTeamId)` | `HeadToHeadResponse` → `SummaryMatchDto` | original |
| [`get_players`](#get_players) | **not called yet** — deferred by the player one-liner plan | — | planned |
| [Error shape](#error-shape) | every call | handled by `ResponseHandler` | — |

---

## `get_leagues`

```
?action=get_leagues
```

Returns every league on the plan (55 at the time of the sample). The app keeps only the ids in the
`Competition` enum.

```json
{
  "country_id": "7",
  "country_name": "Sweden",
  "league_id": "307",
  "league_name": "Allsvenskan",
  "league_season": "2026",
  "league_logo": "https://apiv3.apifootball.com/badges/logo_leagues/307_allsvenskan.png",
  "country_logo": "https://apiv3.apifootball.com/badges/logo_country/7_sweden.png"
}
```

Bound: all seven fields. Nothing tricky.

---

## `get_events`

```
?action=get_events&from=YYYY-MM-DD&to=YYYY-MM-DD&league_id=<id>&timezone=UTC
```

One object per match in the window. The sample is a finished Premier League match with each nested array cut
to its first element.

```json
{
  "match_id": "812671",
  "country_id": "44",
  "country_name": "England",
  "league_id": "152",
  "league_name": "Premier League",
  "match_date": "2026-09-05",
  "match_status": "Finished",
  "match_time": "11:30",
  "match_hometeam_id": "3100",
  "match_hometeam_name": "Newcastle",
  "match_hometeam_score": "2",
  "match_awayteam_name": "Bournemouth",
  "match_awayteam_id": "3071",
  "match_awayteam_score": "2",
  "match_hometeam_halftime_score": "1",
  "match_awayteam_halftime_score": "2",
  "match_hometeam_extra_score": "",
  "match_awayteam_extra_score": "",
  "match_hometeam_penalty_score": "",
  "match_awayteam_penalty_score": "",
  "match_hometeam_ft_score": "2",
  "match_awayteam_ft_score": "2",
  "match_hometeam_system": "4-2-3-1",
  "match_awayteam_system": "4-2-3-1",
  "match_live": "0",
  "match_round": "3",
  "match_stadium": "",
  "match_referee": "R. Jones",
  "team_home_badge": "https://apiv3.apifootball.com/badges/3100_newcastle-united.jpg",
  "team_away_badge": "https://apiv3.apifootball.com/badges/3071_afc-bournemouth.jpg",
  "league_logo": "https://apiv3.apifootball.com/badges/logo_leagues/152_premier-league.png",
  "country_logo": "https://apiv3.apifootball.com/badges/logo_country/44_england.png",
  "league_year": "2026/2027",
  "fk_stage_key": "6",
  "stage_name": "Current",
  "goalscorer": [
    {
      "time": "9",
      "home_scorer": "",
      "home_scorer_id": "",
      "home_assist": "",
      "home_assist_id": "",
      "score": "0 - 1",
      "away_scorer": "M. Tavernier",
      "away_scorer_id": "3819203541",
      "away_assist": "",
      "away_assist_id": "",
      "info": "",
      "score_info_time": "1st Half"
    }
  ],
  "substitutions": {
    "home": [
      { "time": "90", "substitution": "L. Hall | V. Livramento", "substitution_player_id": "2321822486 | 3034772252" }
    ],
    "away": [
      { "time": "90+6", "substitution": "A. Scott | R. Christie", "substitution_player_id": "1272590441 | 422722142" }
    ]
  },
  "cards": [
    {
      "time": "28",
      "home_fault": "A. Elanga",
      "card": "yellow card",
      "away_fault": "",
      "info": "",
      "home_player_id": "2287492793",
      "away_player_id": "",
      "score_info_time": "1st Half"
    }
  ],
  "lineup": {
    "home": {
      "starting_lineups": [
        { "lineup_player": "Lukáš Horníček", "lineup_number": "21", "lineup_position": "1", "player_key": "128201375" }
      ],
      "substitutes": [
        { "lineup_player": "Nick Pope", "lineup_number": "1", "lineup_position": "0", "player_key": "3364715977" }
      ],
      "coach": [
        { "lineup_player": "Matthias Jaissle", "lineup_number": "", "lineup_position": "", "player_key": "2421709841" }
      ],
      "missing_players": []
    },
    "away": { "...same shape..." : "" }
  },
  "statistics": [
    { "type": "Corners", "home": "4", "away": "3" },
    { "type": "Throw In", "home": "18", "away": "16" },
    { "type": "Free Kick", "home": "19", "away": "20" }
  ],
  "statistics_1half": [
    { "type": "Corners", "home": "2", "away": "2" }
  ]
}
```

**Bound** (`MatchDto`): `match_id`, `league_id`, `match_date`, `match_time`, `match_status`, the four
team id/name/score fields, both `_system` fields, `match_stadium`, both badges, `goalscorer`, `substitutions`,
`lineup` (only `home`/`away` → `coach` → `lineup_player`), `statistics`. `match_hometeam_name`,
`match_awayteam_name` and `lineup` carry `@JsonProperty` (bug #3 fix).

**Traps**

- `match_status` is `"Finished"` or `"After Pen."` for a completed match (both treated as finished by
  `FixtureAssembler`), **the current minute as a string** (`"72"`) while live, and `""` before kick-off. Other
  values seen in the wild: `"Half Time"`, `"Postponed"`, `"Cancelled"`.
- `goalscorer[].score` is the running score, normally `"0 - 1"` but **sometimes bracketed** `"[3 - 2]"` — bug #2.
- **`goalscorer[]` carries player ids** — `home_scorer_id`, `away_scorer_id`, `home_assist_id`,
  `away_assist_id` — and they are the same identifier as `player_id` in `get_teams`. `GoalscorerItem` binds
  them and `FixtureAssembler` carries them onto `Goal.scorerId` / `Goal.assistId` (a blank `""` lands as null);
  the player one-liner's recent contributions join on them, never on the free-text names.
- `lineup[].player_key` is also that player id, as a string here (a number in `get_teams` / `get_topscorers`).
- `substitutions` is keyed `home` / `away`, but `SubstitutionsDto` declares `homeSubstitutions` /
  `awaySubstitutions`, which the `SNAKE_CASE` strategy maps to `home_substitutions` — so **it never binds**.
  Nothing reads it, so it is inert rather than a bug.
- Scores are `""` (not `"0"`) before a match starts; `_extra_score` and `_penalty_score` are `""` unless the
  match went that far.
- `match_stadium` is often `""` even for big clubs.

---

## `get_teams`

```
?action=get_teams&league_id=<id>
```

One object per team in the league. **~700 KB for a 20-team league** because every squad member is a 42-field
object. The sample keeps one player.

```json
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
  "players": [
    {
      "player_key": 659972248,
      "player_id": "659972248",
      "player_image": "https://apiv3.apifootball.com/badges/players/68451_e-haaland.jpg",
      "player_name": "Erling Haaland",
      "player_complete_name": "Erling Haaland",
      "player_number": "9",
      "player_country": "",
      "player_type": "Forwards",
      "player_age": "26",
      "player_match_played": "10",
      "player_goals": "8",
      "player_yellow_cards": "0",
      "player_red_cards": "0",
      "player_injured": "No",
      "player_substitute_out": "",
      "player_substitutes_on_bench": "",
      "player_assists": "0",
      "player_birthdate": "2000-07-21",
      "player_is_captain": "1",
      "player_shots_total": "33",
      "player_goals_conceded": "0",
      "player_fouls_committed": "5",
      "player_tackles": "1",
      "player_blocks": "1",
      "player_crosses_total": "",
      "player_interceptions": "",
      "player_clearances": "3",
      "player_dispossesed": "",
      "player_saves": "",
      "player_inside_box_saves": "",
      "player_duels_total": "29",
      "player_duels_won": "17",
      "player_dribble_attempts": "1",
      "player_dribble_succ": "1",
      "player_pen_comm": "",
      "player_pen_won": "",
      "player_pen_scored": "1",
      "player_pen_missed": "0",
      "player_passes": "86",
      "player_passes_accuracy": "54",
      "player_key_passes": "4",
      "player_woordworks": "",
      "player_rating": "7.30"
    }
  ],
  "coaches": [
    { "coach_name": "Enzo Maresca", "coach_country": "", "coach_age": "" }
  ]
}
```

**Bound** (`TeamDataDto`): `team_key`, `team_name`, `team_badge`, `team_founded`, `team_country`, `venue`
(all five fields), `coaches[].coach_name`, and `players[]` → `PlayerItem` (id, image, name, number, type, age,
captain, match_played, goals, assists, cards, injured, shots_total, key_passes, passes, passes_accuracy,
tackles, interceptions, clearances, duels_total, duels_won, saves, inside_box_saves, goals_conceded, rating).
Not bound: `player_key`, `player_complete_name`, `player_country`, `player_birthdate`, the penalty counters,
dribbles, fouls, blocks, crosses, `player_dispossesed`, `player_woordworks`.

**Traps**

- **Stats are season-wide, not league-scoped.** Verified 2026-09-13: the same club fetched via `league_id=152`
  and `league_id=3` returns byte-identical player objects. The value written last wins and it does not matter
  which competition wrote it.
- **Unused players have `""` in every stat, not `"0"`.** About a third of a listed squad. Parse blank as
  `null`, never throw, and never treat it as zero.
- **`player_passes_accuracy` is a count** of completed passes (Dias: 454 of 478), not a percentage.
- **`player_minutes` is absent** despite being documented. It exists only in `get_players`.
- `player_injured` is `"Yes"` / `"No"`; `player_is_captain` is `"1"` / `"0"` / `""`.
- `player_key` is a JSON **number**; `player_id` is the same value as a string. Key on `player_id`.
- `player_country` and `player_birthdate` are frequently `""` here even when `get_players` has them.
- `venue` is a nested object, not the flat `venue_*` fields the docs imply; `coaches` is an array (normally one
  entry, sometimes empty).
- National teams (`WORLD_CUP`) return `"players": []` and no `venue`. That is normal, not an error.
- Two keys are misspelled upstream — `player_dispossesed`, `player_woordworks`. Bind verbatim if ever needed.
- `player_age` in this endpoint (26) and in `get_players` (25, for the national-team entry) disagreed for the
  same player on the same day. Do not treat either as exact.

---

## `get_topscorers`

```
?action=get_topscorers&league_id=<id>
```

The league scoring charts, **the whole league** (50 rows for the Premier League), ordered by `player_place`.

```json
[
  {
    "player_place": "2",
    "player_name": "Bukayo Saka",
    "player_key": 2764979995,
    "team_name": "Arsenal",
    "team_key": "141",
    "goals": "3",
    "assists": "",
    "penalty_goals": "1",
    "fk_stage_key": "21477",
    "stage_name": " - 2nd Stage"
  },
  {
    "player_place": "3",
    "player_name": "Pedro Joao",
    "player_key": 114577412,
    "team_name": "Chelsea",
    "team_key": "88",
    "goals": "3",
    "assists": "3",
    "penalty_goals": "0",
    "fk_stage_key": "21477",
    "stage_name": " - 2nd Stage"
  }
]
```

**Bound** (`TopScorerItem`): `player_place`, `player_name`, `player_key`, `team_name`, `team_key`, `goals`,
`assists`, `penalty_goals`. Used only to backfill `PlayerData.leagueScorerRank`.

**Traps**

- `player_key` is a JSON number and **equals `player_id` in `get_teams`** — verified live, the join is on this.
- `player_place` ties are shared (two players at `"2"`, none at `"1"` in the sample above). A "rank" of 2 can
  mean joint-top.
- A place is not a distinction: the list runs to 50, so one-goal players sit at 30–50. The team one-liner only
  quotes a place ≤ 5.
- `assists` is `""` when zero.
- `stage_name` / `fk_stage_key` are cup-stage noise for a league and should be ignored.

---

## `get_standings`

```
?action=get_standings&league_id=<id>
```

One object per team, ordered by `overall_league_position`.

```json
{
  "country_name": "England",
  "league_id": "152",
  "league_name": "Premier League",
  "team_id": "141",
  "team_name": "Arsenal",
  "overall_promotion": "Promotion - Champions League (League phase)",
  "overall_league_position": "1",
  "overall_league_payed": "4",
  "overall_league_W": "4",
  "overall_league_D": "0",
  "overall_league_L": "0",
  "overall_league_GF": "8",
  "overall_league_GA": "1",
  "overall_league_PTS": "12",
  "home_league_position": "1",
  "home_promotion": "",
  "home_league_payed": "2",
  "home_league_W": "2",
  "home_league_D": "0",
  "home_league_L": "0",
  "home_league_GF": "5",
  "home_league_GA": "1",
  "home_league_PTS": "6",
  "away_league_position": "1",
  "away_promotion": "",
  "away_league_payed": "2",
  "away_league_W": "2",
  "away_league_D": "0",
  "away_league_L": "0",
  "away_league_GF": "3",
  "away_league_GA": "0",
  "away_league_PTS": "6",
  "league_round": "",
  "team_badge": "https://apiv3.apifootball.com/badges/141_arsenal-fc.jpg",
  "fk_stage_key": "6",
  "stage_name": "Current"
}
```

**Bound** (`StandingsDtoItem`): everything. The `_W`, `_D`, `_L` fields carry `@JsonProperty` because the
capital letter does not survive `SNAKE_CASE`.

**Traps**

- `payed` (sic) is "played". `PTS` and `GF`/`GA` bind only because their Java names are `overallLeaguePTS`
  etc. — do not "fix" the casing.
- For the Champions League league phase the structure is the same but `overall_promotion` describes the
  knockout bracket rather than European qualification.
- `league_round` is `""` here; do not rely on it for "matchday".

---

## `get_H2H`

```
?action=get_H2H&firstTeamId=<id>&secondTeamId=<id>
```

The only endpoint returning an **object**, not an array. Three lists of the same summary-match shape. The sample
is `firstTeamId=80` (Manchester City) vs `secondTeamId=2611`; note `2611` is *not* Arsenal (Arsenal is `141`) —
it resolved to a Czech fourth-tier side, which is why `firstTeam_VS_secondTeam` is empty and the second team's
results are what they are. Team ids are global across every league on the plan.

```json
{
  "firstTeam_VS_secondTeam": [],
  "firstTeam_lastResults": [
    {
      "match_id": "853175",
      "country_id": "",
      "country_name": "",
      "league_id": "3",
      "league_name": "UEFA Champions League",
      "match_date": "2026-09-08",
      "match_status": "Finished",
      "match_time": "21:00",
      "match_hometeam_id": "81",
      "match_hometeam_name": "FC Porto",
      "match_hometeam_score": "0",
      "match_awayteam_id": "80",
      "match_awayteam_name": "Manchester City",
      "match_awayteam_score": "2",
      "match_hometeam_halftime_score": "0",
      "match_awayteam_halftime_score": "0",
      "match_live": "0",
      "team_home_badge": "https://apiv3.apifootball.com/badges/81_porto.jpg",
      "team_away_badge": "https://apiv3.apifootball.com/badges/80_manchester-city.jpg",
      "league_logo": "https://apiv3.apifootball.com/badges/logo_leagues/3_uefa-champions-league.png",
      "country_logo": "https://apiv3.apifootball.com/badges/logo_country/160_europe.png"
    }
  ],
  "secondTeam_lastResults": [
    { "...same shape..." : "" }
  ]
}
```

**Bound** (`HeadToHeadResponse` → `SummaryMatchDto`): all three lists, every field. The three list names carry
`@JsonProperty` because of their mixed case.

**Traps**

- The lists span **every competition** the team played, including ones the app does not track (`league_id`
  values outside `Competition`). Filter or label accordingly.
- `firstTeam_VS_secondTeam` is legitimately empty for teams that have never met.
- `country_id` / `country_name` are `""` for European fixtures.
- `HeadToHeadLastFixtures` and `SecondTeamLastResultsItem` in `models/dto/` are unused duplicates of
  `SummaryMatchDto`.

---

## `get_players`

```
?action=get_players&player_id=<id>        (or &player_name=<name>)
```

**Not called by the app yet.** Documented here because the player one-liner plan
(`.claude/docs/player-oneliner.md` §4.5) defers it and the next session will want the shape without another
live call. It is the only source of `player_minutes`, and the only reliable source of `player_country` and
`player_birthdate`.

**Returns one entry per team the player is registered with** — for Haaland, two: Norway first, then Manchester
City. Each entry carries that team's stats. The sample is the national-team entry; the club entry that follows
it has `player_match_played: "10"`, `player_goals: "8"`, `player_minutes: "755"`, matching `get_teams`.

```json
{
  "player_key": 659972248,
  "player_id": "659972248",
  "player_image": "https://apiv3.apifootball.com/badges/players/68451_e-haaland.jpg",
  "player_name": "Erling Haaland",
  "player_complete_name": "Erling Haaland",
  "player_number": "9",
  "player_country": "Norway",
  "player_type": "Forwards",
  "player_age": "25",
  "player_birthdate": "2000-07-21",
  "player_match_played": "4",
  "player_goals": "7",
  "player_yellow_cards": "0",
  "player_red_cards": "0",
  "player_minutes": "360",
  "player_injured": "No",
  "player_substitute_out": "",
  "player_substitutes_on_bench": "",
  "player_assists": "0",
  "player_is_captain": "0",
  "player_shots_total": "15",
  "player_goals_conceded": "0",
  "player_fouls_committed": "5",
  "player_tackles": "",
  "player_blocks": "",
  "player_crosses_total": "",
  "player_interceptions": "1",
  "player_clearances": "7",
  "player_dispossesed": "7",
  "player_saves": "",
  "player_inside_box_saves": "",
  "player_duels_total": "37",
  "player_duels_won": "18",
  "player_dribble_attempts": "4",
  "player_dribble_succ": "1",
  "player_pen_comm": "",
  "player_pen_won": "",
  "player_pen_scored": "0",
  "player_pen_missed": "0",
  "player_passes": "43",
  "player_passes_accuracy": "30",
  "player_key_passes": "6",
  "player_woordworks": "",
  "player_rating": "8.30",
  "team_name": "Norway",
  "team_key": "692"
}
```

**Traps**

- **It is a list, and the first entry may be the national team.** Pick the entry whose `team_key` matches the
  stored `PlayerData.teamId`; never take `[0]`.
- Same field set as a `get_teams` player plus `player_minutes`, `team_name`, `team_key`. Same blank-string
  conventions.
- `player_name` search returns every player with a matching name across the plan, so by-id is the only safe
  form.

---

## Error shape

Any failure — bad key, a league not on the plan, no matches in the window — comes back as **HTTP 200 with an
object** instead of the expected array:

```json
{"error":404,"message":"No event found (please check your plan)!"}
```

Rate limiting and some upstream failures instead return an **HTML page**. `ResponseHandler.process` treats
both the same way: log, return `Optional.empty()`, and let the caller carry on with an empty list. Preserve that
contract for any new call.
