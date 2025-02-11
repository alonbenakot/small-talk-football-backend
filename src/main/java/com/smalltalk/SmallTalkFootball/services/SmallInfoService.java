package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.entities.InfoCategory;
import com.smalltalk.SmallTalkFootball.entities.SmallInfo;
import com.smalltalk.SmallTalkFootball.repositories.SmallInfoRepository;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.system.exceptions.InfoAlreadyExistsException;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
//import com.smalltalk.SmallTalkFootball.system.utils.SmallInfosInitiator;
import com.smalltalk.SmallTalkFootball.system.utils.SmallInfosInitiator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SmallInfoService {

    private final SmallInfoRepository repository;

    public SmallTalkResponse<SmallInfo> addInfo(SmallInfo info) throws InfoAlreadyExistsException {
        if (repository.findByTitle(info.getTitle()).isPresent()) {
            throw new InfoAlreadyExistsException(Messages.INFO_ALREADY_EXISTS);
        }
        return new SmallTalkResponse<>(repository.save(info));
    }

    public SmallTalkResponse<List<SmallInfo>> getAllInfos() {
        List<SmallInfo> infos = repository.findAll();
        infos.sort(Comparator.comparing(SmallInfo::getInfoCategory));
        return new SmallTalkResponse<>(infos);
    }

    public SmallTalkResponse<SmallInfo> getOneInfo(String infoId) throws SmallTalkException {
        Optional<SmallInfo> optionalInfo = repository.findById(infoId);
        SmallInfo info = optionalInfo.orElseThrow(() -> new SmallTalkException(Messages.NO_INFO_FOUND));
        return new SmallTalkResponse<>(info);
    }

    public void deleteSmallInfo(String id) throws SmallTalkException {
        repository.findById(id).orElseThrow(() -> new SmallTalkException(Messages.NO_INFO_TO_DELETE));
        repository.deleteById(id);
    }

    public SmallTalkResponse<List<InfoCategory>> getCategories() {
        List<InfoCategory> categories = Arrays.asList(InfoCategory.values());
        return new SmallTalkResponse<>(categories);
    }

    public SmallTalkResponse<List<SmallInfo>> initSmallInfos() {
        repository.deleteAll();
        List<SmallInfo> generatedInfos = SmallInfosInitiator.init();
        return new SmallTalkResponse<>(repository.saveAll(generatedInfos));
    }

}
