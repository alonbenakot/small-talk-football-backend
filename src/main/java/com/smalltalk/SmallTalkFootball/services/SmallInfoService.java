package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.entities.SmallInfo;
import com.smalltalk.SmallTalkFootball.repositories.SmallInfoRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SmallInfoService {

    private final SmallInfoRepository repository;

    public SmallInfo addInfo(SmallInfo info) {
        return repository.save(info);
    }

    public List<SmallInfo> getAllInfos() {
        return repository.findAll();
    }

    public SmallInfo getOneInfo(String infoId) throws Exception {
        Optional<SmallInfo> optionalInfo = repository.findById(infoId);
        return optionalInfo.orElseThrow(() -> new Exception("No small info was found."));
    }
}
