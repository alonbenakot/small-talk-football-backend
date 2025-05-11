package com.smalltalk.SmallTalkFootball.system.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smalltalk.SmallTalkFootball.domain.SmallInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SmallInfosReader implements Reader<SmallInfo> {

    private final static String infosBasePath = "src/main/resources/data/infos/";

    @Override
    public List<SmallInfo> read() {
        try (Stream<Path> paths = Files.walk(Path.of(infosBasePath))) {

            return paths.filter(Files::isRegularFile)
                    .map(mapToSmallInfo())
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private Function<Path, SmallInfo> mapToSmallInfo() {
        return path -> {
            try {
                String json = Files.lines(path).collect(Collectors.joining());
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(json, SmallInfo.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

}
