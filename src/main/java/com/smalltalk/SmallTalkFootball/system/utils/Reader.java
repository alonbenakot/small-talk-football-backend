package com.smalltalk.SmallTalkFootball.system.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public abstract class Reader<T> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<T> read() {
        try (Stream<Path> paths = Files.walk(getBasePath())) {

            return paths.filter(Files::isRegularFile)
                    .map(mapToObject())
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private Function<Path, T> mapToObject() {
        return path -> {
            try {
                String json = Files.lines(path).collect(Collectors.joining());
                return objectMapper.readValue(json, getTypeClass());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    protected abstract Path getBasePath();

    protected abstract Class<T> getTypeClass();
}
