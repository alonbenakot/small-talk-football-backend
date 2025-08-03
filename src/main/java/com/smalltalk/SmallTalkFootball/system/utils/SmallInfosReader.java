package com.smalltalk.SmallTalkFootball.system.utils;

import com.smalltalk.SmallTalkFootball.domain.SmallInfo;

import java.nio.file.Path;
import java.util.List;

public class SmallInfosReader {

    private static final String INFOS_BASE_PATH = "src/main/resources/data/infos/";

    public static List<SmallInfo> read() {
        return JsonReader.read(getBasePath(), SmallInfo.class);
    }

    private static Path getBasePath() {
        return Path.of(INFOS_BASE_PATH);
    }

}
