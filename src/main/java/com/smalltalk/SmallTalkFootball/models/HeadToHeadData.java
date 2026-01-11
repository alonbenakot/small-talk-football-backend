package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
public class HeadToHeadData {
        private List<Fixture> firstTeamLastFixtures;
        private List<Fixture> secondTeamLastFixtures;
        private List<Fixture> teamsLastFixtures;
}
