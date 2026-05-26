package com.smalltalk.SmallTalkFootball.services.email;

import com.smalltalk.SmallTalkFootball.domain.User;

public interface EmailService {

    void sendWelcomeMail(User user);

    void sendArticleUpForReviewMail();

    void sendArticleAvailableForReviewMail();

    void sendArticlePublishedMail();
}
