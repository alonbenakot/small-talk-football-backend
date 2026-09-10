package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;

import java.util.List;

/**
 * Everything the team one-liner prompt needs, gathered by the service so the factory takes
 * one argument rather than five positional ones.
 *
 * @param notablePlayers the full ranked ten shown on the card; the builder narrows it to the
 *                       0-3 that actually earn a mention.
 */
public record TeamPromptContext(TeamData team,
                                Competition competition,
                                List<Fixture> recentForm,
                                Fixture nextFixture,
                                List<PlayerData> notablePlayers) {
}
