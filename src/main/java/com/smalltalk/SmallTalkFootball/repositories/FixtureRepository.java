package com.smalltalk.SmallTalkFootball.repositories;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface FixtureRepository extends MongoRepository<Fixture, String> {

    long deleteByMatchDateTimeBefore(Instant earliestMatchDay);

    List<Fixture> findByMatchDateTimeAfter(Instant earliestMatchDay);

}
