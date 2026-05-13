package com.smalltalk.SmallTalkFootball.domain;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.Standing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Builder
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamData {

    @Id
    private String id;

    private String name;

    private String coach;

    private String crest;

    private Map<Competition, Standing> standings;
}
