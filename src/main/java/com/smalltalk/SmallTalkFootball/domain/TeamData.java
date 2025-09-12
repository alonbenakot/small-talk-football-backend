package com.smalltalk.SmallTalkFootball.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamData {

    @Id
    private String id;

    private String externalKey;

    private String name;

    private String coach;

    private String crest;

    private int competitionCode;
}
