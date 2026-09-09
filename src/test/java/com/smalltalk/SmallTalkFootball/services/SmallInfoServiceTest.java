package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.SmallInfo;
import com.smalltalk.SmallTalkFootball.enums.InfoCategory;
import com.smalltalk.SmallTalkFootball.repositories.SmallInfoRepository;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallInfoException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmallInfoServiceTest {

    @Mock
    private SmallInfoRepository repository;

    private SmallInfoService service;

    @BeforeEach
    void setUp() {
        service = new SmallInfoService(repository);
    }

    private static SmallInfo info(String title, InfoCategory category) {
        return new SmallInfo(title, "A subtitle", List.of(), category);
    }

    @Test
    void savesANewInfo() throws SmallInfoException {
        SmallInfo info = info("Offside", InfoCategory.RULES);
        when(repository.findByTitle("Offside")).thenReturn(Optional.empty());
        when(repository.save(info)).thenReturn(info);

        assertThat(service.addInfo(info)).isSameAs(info);
    }

    @Test
    void rejectsADuplicateTitle() {
        SmallInfo info = info("Offside", InfoCategory.RULES);
        when(repository.findByTitle("Offside")).thenReturn(Optional.of(info));

        assertThatThrownBy(() -> service.addInfo(info))
                .isInstanceOf(SmallInfoException.class)
                .hasMessage(Messages.INFO_ALREADY_EXISTS);

        verify(repository, never()).save(any());
    }

    @Test
    void groupsInfosByCategory() {
        when(repository.findAll()).thenReturn(new ArrayList<>(List.of(
                info("Rivalries", InfoCategory.FAN_CULTURE),
                info("Offside", InfoCategory.RULES),
                info("Understanding the Game", InfoCategory.BASICS))));

        assertThat(service.getAllInfos()).extracting(SmallInfo::getInfoCategory)
                .containsExactly(InfoCategory.BASICS, InfoCategory.RULES, InfoCategory.FAN_CULTURE);
    }

    @Test
    void listsEveryCategory() {
        assertThat(service.getCategories()).containsExactly(InfoCategory.values());
    }

    @Test
    void returnsAStoredInfo() throws NotFoundException {
        SmallInfo info = info("Offside", InfoCategory.RULES);
        when(repository.findById("1")).thenReturn(Optional.of(info));

        assertThat(service.getOneInfo("1")).isSameAs(info);
    }

    @Test
    void reportsAMissingInfo() {
        when(repository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOneInfo("nope"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(Messages.NO_INFO_FOUND);
    }

    @Test
    void deletesAStoredInfo() throws NotFoundException {
        when(repository.findById("1")).thenReturn(Optional.of(info("Offside", InfoCategory.RULES)));

        service.deleteSmallInfo("1");

        verify(repository).deleteById("1");
    }

    @Test
    void refusesToDeleteAnInfoThatDoesNotExist() {
        when(repository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSmallInfo("nope"))
                .isInstanceOf(NotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void replacesTheCollectionFromTheShippedFiles() {
        service.initSmallInfos();

        verify(repository).deleteAll();
        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(
                infos -> ((List<SmallInfo>) infos).size() > 0));
    }
}
