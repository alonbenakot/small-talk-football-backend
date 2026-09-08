package com.smalltalk.SmallTalkFootball.testsupport;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smalltalk.SmallTalkFootball.models.dto.MatchDto;

/**
 * Builds apifootball.com match payloads as JSON and deserializes them into {@link MatchDto}.
 * <p>
 * MatchDto has no setters and no builder, so JSON is the only way to construct one - which is
 * the right thing for these tests anyway, since it exercises the real deserialization step
 * where the snake_case mapping actually happens.
 */
public final class MatchDtoJson {

    private final ObjectNode root;

    private MatchDtoJson(ObjectNode root) {
        this.root = root;
    }

    /**
     * A completed 3-1 home win, with no goal or statistic detail unless a test adds it.
     */
    public static MatchDtoJson finishedMatch() {
        return base()
                .field("match_status", "Finished")
                .field("match_hometeam_score", "3")
                .field("match_awayteam_score", "1");
    }

    /**
     * A match that has not been played: no status and no scores yet.
     */
    public static MatchDtoJson upcomingMatch() {
        return base()
                .field("match_status", "")
                .field("match_hometeam_score", "")
                .field("match_awayteam_score", "");
    }

    private static MatchDtoJson base() {
        ObjectNode node = JsonFixtures.apiClientMapper().createObjectNode();
        node.put("match_id", "9001");
        node.put("league_id", "152");
        node.put("match_date", "2026-03-01");
        node.put("match_time", "20:45");
        node.put("match_stadium", "Anfield");
        node.put("match_hometeam_id", "2621");
        node.put("match_awayteam_id", "2622");
        node.put("match_hometeam_name", "Liverpool");
        node.put("match_awayteam_name", "Everton");
        node.put("match_hometeam_system", "4-3-3");
        node.put("match_awayteam_system", "4-4-2");
        node.put("team_home_badge", "home-badge.png");
        node.put("team_away_badge", "away-badge.png");
        return new MatchDtoJson(node).withLineup("Arne Slot", "David Moyes");
    }

    public MatchDtoJson field(String jsonKey, String value) {
        if (value == null) {
            root.putNull(jsonKey);
        } else {
            root.put(jsonKey, value);
        }
        return this;
    }

    public MatchDtoJson withoutField(String jsonKey) {
        root.remove(jsonKey);
        return this;
    }

    public MatchDtoJson withLineup(String homeCoach, String awayCoach) {
        ObjectNode lineup = root.putObject("lineup");
        lineup.putObject("home").putArray("coach").addObject().put("lineup_player", homeCoach);
        lineup.putObject("away").putArray("coach").addObject().put("lineup_player", awayCoach);
        return this;
    }

    public MatchDtoJson withoutLineup() {
        root.remove("lineup");
        return this;
    }

    /**
     * Appends a goal. {@code score} is the running score as apifootball reports it, e.g. "2 - 1".
     */
    public MatchDtoJson goal(String time, String homeScorer, String awayScorer, String score) {
        ObjectNode goal = goalscorer().addObject();
        goal.put("time", time);
        goal.put("home_scorer", homeScorer == null ? "" : homeScorer);
        goal.put("away_scorer", awayScorer == null ? "" : awayScorer);
        goal.put("home_assist", "");
        goal.put("away_assist", "");
        goal.put("score", score);
        return this;
    }

    public MatchDtoJson homeGoal(String time, String scorer, String score) {
        return goal(time, scorer, "", score);
    }

    public MatchDtoJson awayGoal(String time, String scorer, String score) {
        return goal(time, "", scorer, score);
    }

    public MatchDtoJson emptyGoals() {
        root.putArray("goalscorer");
        return this;
    }

    public MatchDtoJson nullGoals() {
        root.putNull("goalscorer");
        return this;
    }

    public MatchDtoJson statistic(String type, String home, String away) {
        ObjectNode stat = statistics().addObject();
        stat.put("type", type);
        stat.put("home", home);
        stat.put("away", away);
        return this;
    }

    public MatchDtoJson emptyStatistics() {
        root.putArray("statistics");
        return this;
    }

    private ArrayNode goalscorer() {
        return root.has("goalscorer") && root.get("goalscorer").isArray()
                ? (ArrayNode) root.get("goalscorer")
                : root.putArray("goalscorer");
    }

    private ArrayNode statistics() {
        return root.has("statistics") && root.get("statistics").isArray()
                ? (ArrayNode) root.get("statistics")
                : root.putArray("statistics");
    }

    public String json() {
        return root.toString();
    }

    public MatchDto build() {
        return JsonFixtures.parse(json(), MatchDto.class);
    }
}
