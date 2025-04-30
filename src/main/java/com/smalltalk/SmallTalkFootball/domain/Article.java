package com.smalltalk.SmallTalkFootball.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@NoArgsConstructor
public class Article {

    @Id
    private String id;

    @Indexed(unique = true)
    private String title;

    private String author;

    private String text;

    private boolean published;
}
