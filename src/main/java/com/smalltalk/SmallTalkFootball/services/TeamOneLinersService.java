package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.Perspective;
import com.smalltalk.SmallTalkFootball.models.Standing;
import com.smalltalk.SmallTalkFootball.models.TeamFacts;
import com.smalltalk.SmallTalkFootball.models.TeamOneLiner;
import com.smalltalk.SmallTalkFootball.models.TeamSmallTalk;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.PromptBuilder;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.PromptBuilderFactory;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.TeamPromptContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * The team one-liner: a sentence about a club plus the facts behind it. Separate from
 * {@link OneLinersService} because the caching rule is entirely different — a fixture's
 * one-liner is final once the match is over, whereas a team's is only good until the team
 * next plays or the table moves under it.
 *
 * <p>There is deliberately no TTL (plan §5). A cached sentence is stale when either the team
 * has played since it was written, or the standings snapshot taken at generation time no
 * longer matches the current one. The second rule is what stops "top of the table" surviving
 * an international break in which somebody else went top.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TeamOneLinersService {

    private static final int RECENT_FORM_LIMIT = 5;

    private final TeamDataService teamDataService;

    private final FixtureService fixtureService;

    private final PlayerDataService playerDataService;

    private final AiService aiService;

    private final PromptBuilderFactory promptBuilderFactory;

    public TeamSmallTalk getTeamSmallTalk(String teamId, Competition competition, Language lang,
                                          Perspective perspective) throws SmallTalkException {
        TeamData team = teamDataService.getTeamById(teamId);
        Competition target = resolveCompetition(team, competition);

        List<Fixture> recentForm = fixtureService.getRecentFinishedForTeam(teamId, RECENT_FORM_LIMIT);
        Fixture nextFixture = fixtureService.getNextFixtureForTeam(teamId).orElse(null);
        List<PlayerData> notablePlayers = playerDataService.getNotablePlayers(teamId);

        TeamOneLiner oneLiner = cachedOneLiner(team, target, lang, perspective, recentForm)
                .orElseGet(() -> generate(team, target, lang, perspective, recentForm, nextFixture, notablePlayers));

        return new TeamSmallTalk(oneLiner, TeamFacts.from(team, target, recentForm, nextFixture, notablePlayers));
    }

    private Optional<TeamOneLiner> cachedOneLiner(TeamData team, Competition competition, Language lang,
                                                  Perspective perspective, List<Fixture> recentForm) {
        return team.findOneLiner(lang, competition, perspective)
                .filter(oneLiner -> isFresh(oneLiner, team, competition, recentForm));
    }

    private boolean isFresh(TeamOneLiner oneLiner, TeamData team, Competition competition, List<Fixture> recentForm) {
        if (oneLiner.getGeneratedAt() == null) {
            return false;
        }
        return !hasPlayedSince(oneLiner.getGeneratedAt(), recentForm)
                && standingIsUnchanged(oneLiner, team, competition);
    }

    private static boolean hasPlayedSince(Instant generatedAt, List<Fixture> recentForm) {
        return recentForm.stream()
                .map(Fixture::getMatchDateTime)
                .filter(Objects::nonNull)
                .anyMatch(matchDateTime -> matchDateTime.isAfter(generatedAt));
    }

    /**
     * A team's league position moves when other teams play, so the snapshot comparison is
     * what keeps the sentence honest during a break in the team's own fixtures.
     */
    private static boolean standingIsUnchanged(TeamOneLiner oneLiner, TeamData team, Competition competition) {
        Standing standing = standing(team, competition);
        Integer position = standing == null ? null : standing.getPosition();
        Integer points = standing == null ? null : standing.getPoints();

        return Objects.equals(position, oneLiner.getPositionAtGeneration())
                && Objects.equals(points, oneLiner.getPointsAtGeneration());
    }

    private TeamOneLiner generate(TeamData team, Competition competition, Language lang, Perspective perspective,
                                  List<Fixture> recentForm, Fixture nextFixture, List<PlayerData> notablePlayers) {
        TeamPromptContext context =
                new TeamPromptContext(team, competition, recentForm, nextFixture, notablePlayers);
        PromptBuilder promptBuilder = promptBuilderFactory.create(context, lang, perspective);

        Standing standing = standing(team, competition);

        TeamOneLiner oneLiner = TeamOneLiner.builder()
                .language(lang)
                .competition(competition)
                .perspective(perspective)
                .text(aiService.generate(promptBuilder.buildPrompt()))
                .generatedAt(Instant.now())
                .positionAtGeneration(standing == null ? null : standing.getPosition())
                .pointsAtGeneration(standing == null ? null : standing.getPoints())
                .build();

        // replace, not add: TeamOneLiner equality ignores the text, so add() on a set that
        // already holds this key is a silent no-op and the stale sentence would survive.
        team.replaceOneLiner(oneLiner);
        teamDataService.save(team);

        return oneLiner;
    }

    /**
     * Picks the competition the sentence is about (plan §6.3): the requested one when it is
     * present, otherwise the team's domestic league — the entry that is neither the Champions
     * League nor the World Cup — and the most-played of those if there is still a choice.
     * <p>
     * A national side is rejected rather than answered: league position is meaningless for
     * one, and the alternative is a confidently wrong sentence.
     */
    private Competition resolveCompetition(TeamData team, Competition requested) throws SmallTalkException {
        Map<Competition, Standing> standings = team.getStandings();

        if (requested != null && requested != Competition.WORLD_CUP
                && standings != null && standings.containsKey(requested)) {
            return requested;
        }

        List<Competition> candidates = standings == null ? List.of() : standings.keySet().stream()
                .filter(competition -> competition != Competition.WORLD_CUP)
                .toList();

        if (candidates.isEmpty()) {
            log.info("Team {} has no standing outside the World Cup; rejecting the one-liner request", team.getId());
            throw new SmallTalkException(Messages.TEAM_HAS_NO_LEAGUE_STANDING);
        }

        List<Competition> domestic = candidates.stream()
                .filter(competition -> competition != Competition.CHAMPIONS_LEAGUE)
                .toList();

        return mostPlayed(team, domestic.isEmpty() ? candidates : domestic);
    }

    private static Competition mostPlayed(TeamData team, List<Competition> candidates) {
        return candidates.stream()
                .max(Comparator.comparingInt(competition -> playedMatches(team, competition)))
                .orElseThrow();
    }

    private static int playedMatches(TeamData team, Competition competition) {
        Standing standing = standing(team, competition);
        return standing == null || standing.getPlayedMatches() == null ? 0 : standing.getPlayedMatches();
    }

    private static Standing standing(TeamData team, Competition competition) {
        return team.getStandings() == null ? null : team.getStandings().get(competition);
    }
}
