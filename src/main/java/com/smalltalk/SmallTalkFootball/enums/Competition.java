package com.smalltalk.SmallTalkFootball.enums;

public enum Competition {
    GHANA_PREMIER(177),
    NON_PREMIER(149);
//    PREMIER_LEAGUE(152),
//    CHAMPIONSHIP(153);
//    LIGUE_2(164);
//    LIGAT_HA_AL(202),
//    LA_LIGA(302),
//    BUNDESLIGA(175),
//    SERIA_A(207);

    private final int code;

    Competition(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Competition fromCode(int code) {
        for (Competition competition : Competition.values()) {
            if (competition.getCode() == code) {
                return competition;
            }
        }
        throw new IllegalArgumentException("No competition found for code: " + code);
    }
}
