package com.smalltalk.SmallTalkFootball.models;

/**
 * The body of {@code GET /one-liners/players/{playerId}}: the sentence, plus the facts it was
 * written from so the frontend can render the card alongside it.
 */
public record PlayerSmallTalk(PlayerOneLiner oneLiner, PlayerFacts facts) {
}
