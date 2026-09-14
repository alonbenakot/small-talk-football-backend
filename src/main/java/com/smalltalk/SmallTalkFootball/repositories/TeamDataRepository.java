package com.smalltalk.SmallTalkFootball.repositories;

import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface TeamDataRepository extends MongoRepository<TeamData, String> {

    /**
     * A team is in a competition exactly when it has a standing for it: the map key is the
     * competition's enum name. Unsorted: the {@code ?0} placeholder is not substituted inside
     * {@code sort}, so the service orders by position.
     */
    @Query("{ 'standings.?0': { $exists: true } }")
    List<TeamData> findByCompetition(Competition competition);
}
