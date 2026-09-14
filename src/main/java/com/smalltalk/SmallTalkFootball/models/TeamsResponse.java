package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.enums.Competition;

import java.util.List;

/**
 * The Teams page in one call, shaped like {@link FixturesResponse}: the competitions that have
 * teams, and every team once per competition it has a table in, in competition then table order.
 */
public record TeamsResponse(List<Competition> competitions, List<TeamSummary> teams) {
}
