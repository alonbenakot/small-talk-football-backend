package com.smalltalk.SmallTalkFootball.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@AllArgsConstructor
public class SmallInfo {

    @Id
    public String id;

    public String title;

    public String subtitle;

    public String text;

    public InfoCategory InfoCategory;

    public SmallInfo(String title, String subtitle, String text, com.smalltalk.SmallTalkFootball.entities.InfoCategory infoCategory) {
        this.title = title;
        this.subtitle = subtitle;
        this.text = text;
        InfoCategory = infoCategory;
    }
}