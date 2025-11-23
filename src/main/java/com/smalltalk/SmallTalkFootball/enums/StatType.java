package com.smalltalk.SmallTalkFootball.enums;

import java.util.Arrays;
import java.util.List;

public enum StatType {
    POSSESSION("Ball Possession"),
    ATTACKS("Attacks"),
    OFF_TARGET("Off Target"),
    ON_TARGET("On Target");

    private final String value;

    StatType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static List<StatType> getStatTypes() {
        return Arrays.asList(StatType.values());
    }

    public static List<String> getStatTypesValue() {
        return getStatTypes().stream()
                .map(StatType::getValue).toList();
    }
}