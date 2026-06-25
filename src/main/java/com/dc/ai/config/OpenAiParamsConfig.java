//package com.dc.ai.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class OpenAiParamsConfig {
//
//    @Value("${spring.ai.openai.base-url}")
//    private String baseUrl;
//
//    @Value("${spring.ai.openai.api-key}")
//    private String apiKey;
//
//    @Value("${spring.ai.openai.chat.options.model}")
//    private String chatModel;
//
//    @Value("${spring.ai.openai.grok.base-url}")
//    private String grokBaseUrl;
//
//    @Value("${spring.ai.openai.grok.api-key}")
//    private String grokApiKey;
//
//    @Value("${spring.ai.openai.grok.chat.options.model}")
//    private String grokChatModel;
//
//    @Bean
//    public OpenAiParams openAiParams() {
//        return new OpenAiParams(baseUrl, apiKey, chatModel, grokBaseUrl, grokApiKey, grokChatModel);
//    }
//
//    public record OpenAiParams(
//            String baseUrl,
//            String apiKey,
//            String chatModel,
//            String grokBaseUrl,
//            String grokApiKey,
//            String grokChatModel
//    ) {
//    }
//}