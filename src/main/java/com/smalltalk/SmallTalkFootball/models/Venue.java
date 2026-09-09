package com.smalltalk.SmallTalkFootball.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A team's home ground, embedded on {@link com.smalltalk.SmallTalkFootball.domain.TeamData}
 * and echoed back on the team one-liner card. Capacity is kept as a string because it
 * arrives as one from apifootball and is only ever displayed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Venue {

    private String name;

    private String address;

    private String city;

    private String capacity;

    private String surface;
}
