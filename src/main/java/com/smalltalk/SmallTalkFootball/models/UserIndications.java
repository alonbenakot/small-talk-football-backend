package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserIndications {

    private boolean pendingArticles;

    private Language preferredLanguage;

}
