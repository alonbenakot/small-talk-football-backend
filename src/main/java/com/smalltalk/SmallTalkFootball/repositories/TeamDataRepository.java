package com.smalltalk.SmallTalkFootball.repositories;

import com.smalltalk.SmallTalkFootball.domain.TeamData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeamDataRepository extends MongoRepository<TeamData, String> {

}
