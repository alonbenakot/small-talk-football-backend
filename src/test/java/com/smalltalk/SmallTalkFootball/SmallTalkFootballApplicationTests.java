package com.smalltalk.SmallTalkFootball;

import com.smalltalk.SmallTalkFootball.services.jobs.FixturesJob;
import com.smalltalk.SmallTalkFootball.services.jobs.StandingsJob;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Verifies the Spring context wires up. Configuration comes from
 * src/test/resources/application.properties, which shadows the main one so that
 * no test needs real credentials or a reachable MongoDB.
 * <p>
 * The scheduled jobs are mocked out: {@code @EnableScheduling} is on the application
 * class, and a job firing mid-test would make real calls to apifootball.com.
 */
@SpringBootTest
class SmallTalkFootballApplicationTests {

    @MockBean
    private FixturesJob fixturesJob;

    @MockBean
    private StandingsJob standingsJob;

    @Test
    void contextLoads() {
    }

}
