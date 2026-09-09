package com.smalltalk.SmallTalkFootball.repositories;

import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PlayerDataRepository extends MongoRepository<PlayerData, String> {

    List<PlayerData> findByTeamId(String teamId);
}
