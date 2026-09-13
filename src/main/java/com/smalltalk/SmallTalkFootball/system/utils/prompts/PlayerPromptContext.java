package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.MatchContribution;
import com.smalltalk.SmallTalkFootball.models.SquadContext;

import java.util.List;

/**
 * Everything the player one-liner prompt needs, gathered by the service. {@code team} and
 * {@code competition} are nullable: a player whose club no longer resolves still gets a
 * sentence about himself, with the club context phrased as unavailable (plan §7, §8).
 */
public record PlayerPromptContext(PlayerData player,
                                  SquadContext squadContext,
                                  TeamData team,
                                  Competition competition,
                                  List<Fixture> recentForm,
                                  Fixture nextFixture,
                                  List<MatchContribution> recentContributions) {
}
