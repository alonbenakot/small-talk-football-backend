package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.enums.InfoCategory;
import com.smalltalk.SmallTalkFootball.domain.SmallInfo;
import com.smalltalk.SmallTalkFootball.repositories.SmallInfoRepository;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallInfoException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import com.smalltalk.SmallTalkFootball.system.utils.Reader;
import com.smalltalk.SmallTalkFootball.system.utils.SmallInfosReader;
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

    public SmallTalkResponse<SmallInfo> addInfo(SmallInfo info) throws SmallInfoException {
        if (repository.findByTitle(info.getTitle()).isPresent()) {
            throw new SmallInfoException(Messages.INFO_ALREADY_EXISTS);
        }
        return new SmallTalkResponse<>(repository.save(info));
    }

    public SmallTalkResponse<List<SmallInfo>> getAllInfos() {
        List<SmallInfo> infos = repository.findAll();
        infos.sort(Comparator.comparing(SmallInfo::getInfoCategory));
        return new SmallTalkResponse<>(infos);
    }

    public SmallTalkResponse<SmallInfo> getOneInfo(String infoId) throws NotFoundException {
        Optional<SmallInfo> optionalInfo = repository.findById(infoId);
        SmallInfo info = optionalInfo.orElseThrow(() -> new NotFoundException(Messages.NO_INFO_FOUND));
        return new SmallTalkResponse<>(info);
    }

    public void deleteSmallInfo(String id) throws NotFoundException {
        repository.findById(id).orElseThrow(() -> new NotFoundException(Messages.NO_INFO_FOUND));
        repository.deleteById(id);
    }

    public SmallTalkResponse<List<InfoCategory>> getCategories() {
        List<InfoCategory> categories = Arrays.asList(InfoCategory.values());
        return new SmallTalkResponse<>(categories);
    }

    public void initSmallInfos() {
        repository.deleteAll();
        Reader<SmallInfo> reader = new SmallInfosReader();
        List<SmallInfo> generatedInfos = reader.read();
        repository.saveAll(generatedInfos);
    }

}
