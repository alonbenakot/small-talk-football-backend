package com.smalltalk.SmallTalkFootball.repositories;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface FixtureRepository extends MongoRepository<Fixture, String> {

    long deleteByMatchDateTimeBefore(Instant earliestMatchDay);

    List<Fixture> findByMatchDateTimeAfter(Instant earliestMatchDay);

    /**
     * Every finished fixture a team appeared in, either side of the tie. Spring Data's
     * PartTree splits on {@code Or} first and {@code And}s within each branch, so this parses
     * as {@code (finished AND homeTeam.id = ?0) OR (finished AND awayTeam.id = ?1)} — there is
     * no parenthesis syntax, so repeating {@code FinishedTrue} is required rather than
     * redundant. Pass the same team id for both arguments.
     * <p>
     * {@code HomeTeamId} has no matching property on Fixture, so it resolves to the nested
     * path {@code homeTeam.id}. That resolution is silent: a typo would yield an
     * always-empty query rather than an error, so these two queries need checking against a
     * real database before the feature merges.
     */
    List<Fixture> findByFinishedTrueAndHomeTeamIdOrFinishedTrueAndAwayTeamId(String homeTeamId,
                                                                             String awayTeamId,
                                                                             Sort sort);

    /** The mirror of the above for fixtures not yet played. */
    List<Fixture> findByFinishedFalseAndHomeTeamIdOrFinishedFalseAndAwayTeamId(String homeTeamId,
                                                                               String awayTeamId,
                                                                               Sort sort);

}
