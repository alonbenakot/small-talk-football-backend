package com.smalltalk.SmallTalkFootball.models.dto;

import java.util.List;

public class MatchesByCompetitionsDTO {
    private CompetitionDTO competition;
    private Filters filters;
    private List<MatchesItemDTO> matches;
    private ResultSet resultSet;

    public CompetitionDTO getCompetition() {
        return competition;
    }

    public Filters getFilters() {
        return filters;
    }

    public List<MatchesItemDTO> getMatches() {
        return matches;
    }

    public ResultSet getResultSet() {
        return resultSet;
    }
}