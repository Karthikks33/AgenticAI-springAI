package com.springAI.javaAI.controller;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.print.attribute.standard.Media;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController()
@RequestMapping("/stream")
public class StreamingController {

    private final ChatClient chatClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public StreamingController(ChatClient.Builder chatBuilder) {
        this.chatClient = chatBuilder.build();
    }

    /**
     * SseEmitter: A subclass of ResponseBodyEmitter specifically designed to stream
     * Server-Sent Events (SSE), formatting emitted data objects into compliant
     * 'text/event-stream' lines for text-based, real-time client push notification
     * lines.
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamingChat(@RequestParam(defaultValue = "tell me about RAG in s sentence") String message) {

        SseEmitter emitter = new SseEmitter();

        executorService.submit(() -> chatClient
                .prompt()
                .user(message)
                .stream()
                .content()
                .subscribe(
                        token -> sendToken(emitter, token),
                        error -> emitter.completeWithError(error),
                        emitter::complete));

        return emitter;
    }

    @GetMapping(value = "/prompt", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter promptChat(@RequestParam(defaultValue = "Spring AI") String topic,
            @RequestParam(defaultValue = "beginner") String level) {

        SseEmitter emitter = new SseEmitter();
        PromptTemplate template = new PromptTemplate("write a {level}- friendly explaination of {topic} in 150 words");
        Prompt prompt = template.create(Map.of("topic", topic, "level", level));
        executorService.submit(() -> chatClient
                .prompt(prompt)
                .stream()
                .content()
                .subscribe(
                        token -> sendToken(emitter, token),
                        error -> emitter.completeWithError(error),
                        emitter::complete));

        return emitter;
    }

    @GetMapping(value = "/multi-turn", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter multiTurnChat(@RequestParam String message) {

        SseEmitter emitter = new SseEmitter();

        String historyContext = "Previous conversation:\n" +
                "User: What is Spring AI?\n" +
                "Assistant: Spring AI is a Spring framework module that simplifies building " +
                "AI-powered applications by providing a unified API across LLM providers.\n\n" +
                "Now continue the conversation. User says: " + message;

        executorService.submit(() -> {
            chatClient.prompt()
                    .user(historyContext)
                    .stream()
                    .content()
                    .subscribe(
                            token -> sendToken(emitter, token),
                            error -> emitter.completeWithError(error),
                            () -> {
                                // After the first response completes, send a follow-up question which has no
                                // details of above history
                                // its treated a new question by LLM
                                chatClient.prompt()
                                        .user("who developed this?")
                                        .stream()
                                        .content()
                                        .subscribe(
                                                token -> sendToken(emitter, token),
                                                error -> emitter.completeWithError(error),
                                                emitter::complete);
                            });
        });

        return emitter;
    }

    /**
     * ResponseBodyEmitter: A specialized asynchronous request processing return
     * type
     * used to emit multiple Java objects sequentially to the response, with each
     * object serialized asynchronously via a compatible Spring
     * HttpMessageConverter.
     */
    @GetMapping(value = "/raw-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseBodyEmitter rawStream(@RequestParam String message) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(120_000L);

        executorService.submit(() -> chatClient.prompt()
                .user(message)
                .stream()
                .content()
                .subscribe(
                        token -> {
                            try {
                                emitter.send(token, MediaType.TEXT_PLAIN);
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> emitter.completeWithError(error),
                        emitter::complete

                ));

        return emitter;

    }

    /**
     * StreamingResponseBody: A functional callback interface designed for direct,
     * asynchronous streaming of raw byte data or files to the response OutputStream
     * without clogging or locking up the primary Servlet container threads.
     */
    @GetMapping(value = "/output-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody outputSteam(@RequestParam String message) {

        return outputSteam -> {
            chatClient
                    .prompt()
                    .user(message)
                    .stream()
                    .content()
                    .doOnNext(
                            token -> {
                                try {
                                    outputSteam.write(token.getBytes());
                                    outputSteam.flush();
                                } catch (Exception e) {
                                    // TODO: handle exception
                                }
                            })
                    .doOnComplete(() -> {
                        try {
                            outputSteam.flush();
                            outputSteam.close();
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                    })
                    .blockLast();
        };
    }

    private void sendToken(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().data(token));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

}
