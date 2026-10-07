package com.smalltalk.SmallTalkFootball.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final ChatModel client;

    /**
     * The cards render a sentence on one line, but the model happily returns two, split on a
     * hard newline with trailing spaces. Collapsing every whitespace run to a single space is
     * cheaper and more reliable than asking the prompt not to do it.
     */
    public static String singleLine(String text) {
        return text == null ? null : text.strip().replaceAll("\\s+", " ");
    }

    public String generate(String promptText) {
        try {
            var options = OpenAiChatOptions.builder()
                    .temperature(1.0)
                    .build();
            Prompt prompt = new Prompt(promptText, options);

            return client.call(prompt).getResult().getOutput().getText();

        } catch (Exception e) {
            log.error("Error calling AI service", e);
            throw e;
        }
    }
}
