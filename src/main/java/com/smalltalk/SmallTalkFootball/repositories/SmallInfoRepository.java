package com.smalltalk.SmallTalkFootball.repositories;

import com.smalltalk.SmallTalkFootball.entities.SmallInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmallInfoRepository extends MongoRepository<SmallInfo, String> {
}
