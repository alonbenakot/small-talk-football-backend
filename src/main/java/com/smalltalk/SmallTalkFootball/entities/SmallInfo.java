package com.smalltalk.SmallTalkFootball.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
@Data
@NoArgsConstructor
public class SmallInfo {

    @Id
    private String id;

    @Indexed(unique = true)
    private String title;

    private String subtitle;

    private List<InfoText> infoTexts;

    private InfoCategory infoCategory;

    public SmallInfo(String title, String subtitle, List<InfoText> infoTexts, InfoCategory infoCategory) {
        this.title = title;
        this.subtitle = subtitle;
        this.infoTexts = infoTexts;
        this.infoCategory = infoCategory;
    }
}