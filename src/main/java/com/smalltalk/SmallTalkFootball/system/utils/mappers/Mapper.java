package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import javax.validation.constraints.NotNull;

public interface Mapper<S, T> {

    T map(@NotNull S sourceObject);
}

