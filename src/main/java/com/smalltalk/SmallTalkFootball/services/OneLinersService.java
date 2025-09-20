package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.OneLiner;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.system.utils.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OneLinersService {

    private final FixtureService fixtureService;
    private final AiService aiService;

    public OneLiner getOneLiner(String fixtureId, TeamType teamType, Language lang) throws SmallTalkException {
        Fixture fixture = fixtureService.getFixture(fixtureId).orElseThrow(() -> new SmallTalkException("Invalid fixture id"));


        return fixture.getOneLiners().stream()
                .filter(oneLiner -> matchesTeamAndLanguage(teamType, lang, oneLiner))
                .findAny()
                .orElse(generateOneLiner(teamType, lang, fixture));
    }

    private OneLiner generateOneLiner(TeamType teamType, Language lang, Fixture fixture) throws SmallTalkException {
        String promptText = PromptBuilder.forOneliner(fixture, teamType, lang);
        log.debug(promptText);
        String oneLinerText = aiService.generate(promptText);

        if (oneLinerText == null || oneLinerText.isBlank()) {
            throw new SmallTalkException("AI failed to generate one-liner");
        }

        OneLiner oneLiner = OneLiner.builder()
                .text(oneLinerText)
                .language(lang)
                .teamType(teamType)
                .build();

        fixture.addOneLiner(oneLiner);
        fixtureService.saveFixture(fixture);
        return oneLiner;
    }

    private static boolean matchesTeamAndLanguage(TeamType teamType, Language lang, OneLiner oneLiner) {
        return oneLiner.getTeamType() == teamType && oneLiner.getLanguage() == lang;
    }
}
