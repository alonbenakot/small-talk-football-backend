package com.smalltalk.SmallTalkFootball.services.jobs;

import com.smalltalk.SmallTalkFootball.services.TeamDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class TeamsJobTest {

    @Mock
    private TeamDataService teamDataService;

    @InjectMocks
    private TeamsJob job;

    @Test
    void runJob_delegatesToTeamDataService() {
        job.runJob();

        verify(teamDataService).saveCompetitionTeams();
        verifyNoMoreInteractions(teamDataService);
    }
}
