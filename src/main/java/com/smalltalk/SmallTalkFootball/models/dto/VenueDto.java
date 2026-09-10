package com.smalltalk.SmallTalkFootball.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * The nested {@code venue} object inside a {@code get_teams} response. The published
 * apifootball documentation implies flat {@code venue_*} fields at team level; the live
 * response (see the plan appendix) nests them under a {@code venue} object instead, so
 * this is bound as its own type. Every field arrives as a string.
 */
@Getter
public class VenueDto {

    @JsonProperty("venue_name")
    private String venueName;

    @JsonProperty("venue_address")
    private String venueAddress;

    @JsonProperty("venue_city")
    private String venueCity;

    @JsonProperty("venue_capacity")
    private String venueCapacity;

    @JsonProperty("venue_surface")
    private String venueSurface;
}
