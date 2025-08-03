package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.SmallInfo;
import com.smalltalk.SmallTalkFootball.enums.InfoCategory;
import com.smalltalk.SmallTalkFootball.repositories.SmallInfoRepository;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallInfoException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
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

    public SmallInfo addInfo(SmallInfo info) throws SmallInfoException {
        if (repository.findByTitle(info.getTitle()).isPresent()) {
            throw new SmallInfoException(Messages.INFO_ALREADY_EXISTS);
        }
        return repository.save(info);
    }

    public List<SmallInfo> getAllInfos() {
        List<SmallInfo> infos = repository.findAll();
        infos.sort(Comparator.comparing(SmallInfo::getInfoCategory));
        return infos;
    }

    public SmallInfo getOneInfo(String infoId) throws NotFoundException {
        Optional<SmallInfo> optionalInfo = repository.findById(infoId);
        return optionalInfo.orElseThrow(() -> new NotFoundException(Messages.NO_INFO_FOUND));
    }

    public void deleteSmallInfo(String id) throws NotFoundException {
        repository.findById(id).orElseThrow(() -> new NotFoundException(Messages.NO_INFO_FOUND));
        repository.deleteById(id);
    }

    public List<InfoCategory> getCategories() {
        return Arrays.asList(InfoCategory.values());
    }

    public void initSmallInfos() {
        repository.deleteAll();
        repository.saveAll(SmallInfosReader.read());
    }

}
