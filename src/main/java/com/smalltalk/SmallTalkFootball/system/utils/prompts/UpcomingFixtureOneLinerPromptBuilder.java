package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.HeadToHeadData;

public class UpcomingFixtureOneLinerPromptBuilder implements PromptBuilder {

    private Fixture fixture;
    private Language language;
    private String preferredTeam;
    private HeadToHeadData headToHeadData;

    public UpcomingFixtureOneLinerPromptBuilder(Fixture fixture, TeamType preferredTeam, Language language, HeadToHeadData headToHeadData) {
        this.fixture = fixture;
        this.language = language;
        this.headToHeadData = headToHeadData;
        setPreferredTeam(preferredTeam);
    }

    public void setPreferredTeam(TeamType teamType) {
        this.preferredTeam = teamType == null
                ? ""
                : teamType == TeamType.HOME ? fixture.getHomeTeam().getName() : fixture.getAwayTeam().getName();
    }

    @Override
    public String examples() {
        return "";
    }

    @Override
    public String role() {
        return """
                You are a %s fan chatting with your friends about an upcoming football (soccer) match.
                """.formatted(preferredTeam);
    }

    @Override
    public String task() {
        return "";
    }

    @Override
    public String style() {
        return "";
    }

    @Override
    public String structure() {
        return "";
    }

    @Override
    public String constraints() {
        return "";
    }

    @Override
    public String data() {
        return """
                %s
                
                """.formatted(getTeamsVs());
    }

    private String getTeamsVs() {
        return """
                %s vs %s
                """.formatted(fixture.getHomeTeam().getName(), fixture.getAwayTeam().getName());
    }
}
