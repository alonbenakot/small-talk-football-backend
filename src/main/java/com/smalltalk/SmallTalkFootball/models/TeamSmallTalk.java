package com.smalltalk.SmallTalkFootball.models;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * The body of {@code GET /one-liners/teams/{teamId}}: the sentence, plus the facts it was
 * written from so the frontend can render the card alongside it.
 */
@Data
@AllArgsConstructor
public class TeamSmallTalk {

    private TeamOneLiner oneLiner;

    private TeamFacts facts;
}
