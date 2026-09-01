package com.springAI.javaAI.controller;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.springAI.javaAI.model.AuthorBooks;

@RestController
public class AIController {

    private final ChatClient chatClient;

    public AIController(ChatClient.Builder chatBuilder) {
        this.chatClient = chatBuilder.build();
    }

    @GetMapping("/chat")
    public String letsChat(@RequestParam(defaultValue = "tell me something") String message) {
        return chatClient.prompt().user(message).call().content();
    }

    @GetMapping("/prompt-engineering")
    public String promptEngg(@RequestParam(defaultValue = "java") String topic) {

        SystemMessage systemMessage = new SystemMessage(
                "You re very helpful, you expain things like a teacher very detailed");
        UserMessage userMessage = new UserMessage(topic);
        ChatOptions options = ChatOptions.builder()
                .maxTokens(500)
                .build();
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage), options);
        return chatClient.prompt(prompt).call().content();
    }

    @GetMapping("/tempate")
    public String getTemplate(@RequestParam(defaultValue = "java") String language) {

        PromptTemplate template = new PromptTemplate("tell me great facts about {language} programming");

        // Prompt prompt = template.create(Map.of("language", language));
        String prompt = template.render(Map.of("language", language));
        return chatClient.prompt(prompt).call().content();
    }

    @GetMapping("/author-books")
    public AuthorBooks getAuthoeBooks(@RequestParam(defaultValue = "paulo coelho") String author) {

        // 1. Setup the structured converter
        BeanOutputConverter<AuthorBooks> converter = new BeanOutputConverter<>(AuthorBooks.class);
        String format = converter.getFormat();

        // 2. Strict engineering prompt to prevent conversational filler text
        PromptTemplate template = new PromptTemplate(
                "Generate 5 top books written by {author}. Your output must strictly match this schema: {format}. " +
                        "Do not include markdown code blocks, backticks, or any introductory text. Return raw JSON text only.");

        // 3. Force Ollama to speak strictly in JSON format
        ChatOptions options = OllamaChatOptions.builder()
                .model("llama3.2")
                .format("json") // Mandates structural JSON syntax
                .temperature(0.0) // Low value stops creative syntax mutations
                .maxTokens(500)
                .build();

        // 4. Combine rendering context AND strict JSON configuration options
        Prompt prompt = new Prompt(template.render(Map.of("author", author, "format", format)), options);

        // 5. Call the local model
        String response = chatClient.prompt(prompt).call().content();

        // 6. Safely transform raw JSON string into your Java POJO object
        return converter.convert(response);

    }

}
