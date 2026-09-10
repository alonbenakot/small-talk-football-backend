package com.smalltalk.SmallTalkFootball.enums;

/**
 * Who is speaking the team one-liner. The same facts read three ways: a supporter of the
 * team, a supporter of an (unnamed) rival, and someone with no allegiance. This is a
 * prompt-level concern only — no extra data is fetched — but it is part of the cache key,
 * so a team holds one sentence per perspective, language and competition.
 */
public enum Perspective {
    FAN,
    RIVAL_FAN,
    NEUTRAL
}
