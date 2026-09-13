package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.models.PlayerFacts;
import com.smalltalk.SmallTalkFootball.models.PlayerOneLiner;
import com.smalltalk.SmallTalkFootball.models.PlayerSmallTalk;
import com.smalltalk.SmallTalkFootball.models.SquadContext;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.PlayerPromptContext;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.PromptBuilder;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.PromptBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The player one-liner: a sentence about one footballer plus the facts behind it. A third
 * service rather than a method on {@link TeamOneLinersService} because the caching rule is its
 * own (plan §5): no TTL, and a cached sentence is stale only when one of the four snapshot
 * fields — appearances, goals or assists, the injury flag, the scoring-charts place — has
 * moved on the stored document since it was written. {@code TeamsJob} is the only thing that
 * moves them, so in practice a sentence is regenerated at most a couple of times a week.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PlayerOneLinersService {

    private static final int RECENT_FORM_LIMIT = 5;

    private final PlayerDataService playerDataService;

    private final TeamDataService teamDataService;

    private final FixtureService fixtureService;

    private final AiService aiService;

    private final PromptBuilderFactory promptBuilderFactory;

    public PlayerSmallTalk getPlayerSmallTalk(String playerId, Language lang) throws NotFoundException {
        PlayerData player = playerDataService.getPlayerById(playerId);
        String teamId = player.getTeamId();

        SquadContext squadContext = SquadContext.of(player, playerDataService.getPlayersByTeam(teamId));
        // Null-tolerant on purpose: a player whose club no longer resolves still gets a
        // sentence about himself, with the club context omitted (plan §8).
        TeamData team = teamDataService.findTeamById(teamId).orElse(null);
        Competition competition = team == null ? null : team.primaryCompetition().orElse(null);
        List<Fixture> recentForm = fixtureService.getRecentFinishedForTeam(teamId, RECENT_FORM_LIMIT);
        Fixture nextFixture = fixtureService.getNextFixtureForTeam(teamId).orElse(null);

        PlayerOneLiner oneLiner = player.findOneLiner(lang)
                .filter(cached -> isFresh(cached, player))
                .orElseGet(() -> generate(new PlayerPromptContext(
                        player, squadContext, team, competition, recentForm, nextFixture), lang));

        return new PlayerSmallTalk(oneLiner, PlayerFacts.from(player, squadContext, team, competition, nextFixture));
    }

    private static boolean isFresh(PlayerOneLiner cached, PlayerData player) {
        return cached.getGeneratedAt() != null
                && Objects.equals(cached.getMatchesPlayedAtGeneration(), player.getMatchesPlayed())
                && Objects.equals(cached.getGoalsAtGeneration(), player.getGoals())
                && Objects.equals(cached.getAssistsAtGeneration(), player.getAssists())
                && Objects.equals(cached.getInjuredAtGeneration(), player.isInjured())
                && Objects.equals(cached.getScorerRankAtGeneration(), player.getLeagueScorerRank());
    }

    private PlayerOneLiner generate(PlayerPromptContext context, Language lang) {
        PlayerData player = context.player();
        PromptBuilder promptBuilder = promptBuilderFactory.create(context, lang);

        PlayerOneLiner oneLiner = PlayerOneLiner.builder()
                .language(lang)
                .text(AiService.singleLine(aiService.generate(promptBuilder.buildPrompt())))
                .generatedAt(Instant.now())
                .matchesPlayedAtGeneration(player.getMatchesPlayed())
                .goalsAtGeneration(player.getGoals())
                .assistsAtGeneration(player.getAssists())
                .injuredAtGeneration(player.isInjured())
                .scorerRankAtGeneration(player.getLeagueScorerRank())
                .build();

        // replace, not add: PlayerOneLiner equality ignores the text, so add() on a set that
        // already holds this language is a silent no-op and the stale sentence would survive.
        player.replaceOneLiner(oneLiner);
        playerDataService.save(player);

        return oneLiner;
    }
}
