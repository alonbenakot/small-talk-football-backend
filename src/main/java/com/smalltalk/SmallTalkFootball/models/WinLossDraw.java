package com.smalltalk.SmallTalkFootball.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WinLossDraw {

    private Integer wins;

    private Integer losses;

    private Integer draws;
}
