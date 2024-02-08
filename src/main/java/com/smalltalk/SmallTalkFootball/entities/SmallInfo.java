package com.smalltalk.SmallTalkFootball.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@NoArgsConstructor
public class SmallInfo {

    @Id
    private String id;

    private String title;

    private String subtitle;

    private String text;

    private InfoCategory infoCategory;

    public SmallInfo(String title, String subtitle, String text, InfoCategory infoCategory) {
        this.title = title;
        this.subtitle = subtitle;
        this.text = text;
        this.infoCategory = infoCategory;
    }
}