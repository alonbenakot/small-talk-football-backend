package com.smalltalk.SmallTalkFootball.system.utils.prompts;

public interface PromptBuilder {

    default String buildPrompt() {
        return """
                %s
                
                Your task:
                %s
                
                Follow these rules:
                - Tone and Style:
                    %s
                - Structure:
                    %s
                - Constraints:
                    %s
                
                Examples:
                %s
                
                Your data:
                ##############################
                
                %s
                
                ##############################""".formatted(role(), task(), style(), structure(), constraints(), examples(), data());


    }

    String examples();

    String role();

    String task();

    String style();

    String structure();

    String constraints();

    String data();

}
