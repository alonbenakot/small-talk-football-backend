package com.smalltalk.SmallTalkFootball.repositories;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FixtureRepository extends MongoRepository<Fixture, String> {


}
