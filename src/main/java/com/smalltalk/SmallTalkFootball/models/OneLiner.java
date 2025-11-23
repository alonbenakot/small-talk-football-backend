package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.Objects;

@Getter
@Builder
@AllArgsConstructor
public class OneLiner {

    TeamType teamType;

    Language language;

    String text;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        OneLiner oneLiner = (OneLiner) obj;
        return Objects.equals(teamType, oneLiner.teamType) &&
                Objects.equals(language, oneLiner.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamType, language);
    }

}
