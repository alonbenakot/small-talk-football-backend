package com.smalltalk.SmallTalkFootball.repositories;

import com.smalltalk.SmallTalkFootball.domain.CompetitionData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionDataRepository extends MongoRepository<CompetitionData, String> {

}
