package com.smalltalk.SmallTalkFootball.repositories;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FixtureRepository extends MongoRepository<Fixture, String> {

    long deleteByMatchDateTimeBefore(LocalDateTime earliestMatchDay);

    List<Fixture> findByMatchDateTimeAfter(LocalDateTime earliestMatchDay);

}
