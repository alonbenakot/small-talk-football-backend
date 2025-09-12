package com.smalltalk.SmallTalkFootball.system.utils.readers;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class JsonReader {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> List<T> read(Path basePath, Class<T> resourceClass) {
        try (Stream<Path> paths = Files.walk(basePath)) {
            return paths.filter(Files::isRegularFile)
                    .map(path -> deserialize(path, resourceClass))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static <T> T deserialize(Path path, Class<T> resourceClass) {
        try {
            String json = Files.readString(path);
            return objectMapper.readValue(json, resourceClass);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read or parse file: " + path, e);
        }
    }
}

