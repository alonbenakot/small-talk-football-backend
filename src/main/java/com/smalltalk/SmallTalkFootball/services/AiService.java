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

    public String generate(String promptText) {
        try {
            var options = OpenAiChatOptions.builder()
                    .reasoningEffort("medium")
                    .temperature(1.0)
                    .build();
            Prompt prompt = new Prompt(promptText, options);

            log.debug("Sending prompt: {}", promptText);

            long start = System.currentTimeMillis();
            String text = client.call(prompt).getResult().getOutput().getText();
            long end = System.currentTimeMillis();
            log.warn("AI call took {} ms", (end - start));

            log.debug("Generated text: '{}'", text);

            return text;

        } catch (Exception e) {
            log.error("Error calling AI service", e);
            throw e;
        }
    }
}
