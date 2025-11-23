package com.smalltalk.SmallTalkFootball.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum Competition {
    PREMIER_LEAGUE(152),
    LA_LIGA(302),
    BUNDESLIGA(175),
    LIGAT_HA_AL(202),
    SERIA_A(207);

    private final int code;

    Competition(int code) {
        this.code = code;
    }

    private static final Map<Integer, Competition> CODE_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(Competition::getCode, Function.identity()));

    public static Competition fromCode(int code) {
        Competition competition = CODE_MAP.get(code);
        if (competition == null) {
            throw new IllegalArgumentException("No competition found for code: " + code);
        }
        return competition;
    }

    public static boolean isValidCode(int code) {
        return CODE_MAP.containsKey(code);
    }

}
