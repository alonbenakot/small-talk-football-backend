package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.entities.SmallInfo;
import com.smalltalk.SmallTalkFootball.repositories.SmallInfoRepository;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.system.messages.ErrorMessages;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SmallInfoService {

    private final SmallInfoRepository repository;

    public SmallTalkResponse<SmallInfo> addInfo(SmallInfo info) {
        return new SmallTalkResponse<>(repository.save(info));
    }

    public SmallTalkResponse<List<SmallInfo>> getAllInfos() {
        return new SmallTalkResponse<>(repository.findAll());
    }

    public SmallTalkResponse<SmallInfo> getOneInfo(String infoId) throws SmallTalkException {
        Optional<SmallInfo> optionalInfo = repository.findById(infoId);
        SmallInfo info = optionalInfo.orElseThrow(() -> new SmallTalkException(ErrorMessages.NO_INFO_FOUND));
        return new SmallTalkResponse<>(info);
    }

    public void deleteSmallInfo(String id) throws SmallTalkException {
        repository.findById(id).orElseThrow(() -> new SmallTalkException(ErrorMessages.NO_INFO_TO_DELETE));
        repository.deleteById(id);
    }
}
