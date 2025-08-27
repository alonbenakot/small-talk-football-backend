package com.smalltalk.SmallTalkFootball.repositories;

import com.smalltalk.SmallTalkFootball.domain.TeamData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TeamDataRepository extends MongoRepository<TeamData, String> {

    void deleteByCompetitionCode(int competitionCode);

    List<TeamData> findByCompetitionCode(int competitionCode);

}
