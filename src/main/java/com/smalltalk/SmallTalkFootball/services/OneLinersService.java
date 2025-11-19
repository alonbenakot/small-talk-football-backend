package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.OneLiner;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.OneLinerPromptBuilder;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.PromptBuilder;
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
        Fixture fixture = fixtureService.getFixture(fixtureId);

        return fixture.getOneLiners().stream()
                .filter(oneLiner -> matchesTeamAndLanguage(teamType, lang, oneLiner))
                .findAny()
                .orElseGet(() -> generateOneLiner(teamType, lang, fixture));
    }

    private OneLiner generateOneLiner(TeamType teamType, Language lang, Fixture fixture){
        PromptBuilder promptBuilder = new OneLinerPromptBuilder(fixture, teamType, lang);
        String promptText = promptBuilder.buildPrompt();
        String oneLinerText = aiService.generate(promptText);

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
        boolean match = oneLiner.getTeamType() == teamType && oneLiner.getLanguage() == lang;
        log.debug("match: {}", match);
        return match;
    }
}
