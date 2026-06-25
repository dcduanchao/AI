//package com.dc.ai.config;
//
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.openai.OpenAiChatModel;
//import org.springframework.ai.openai.OpenAiChatOptions;
//import org.springframework.ai.openai.api.OpenAiApi;
//import org.springframework.beans.factory.ObjectProvider;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.client.RestClient;
//
//@Configuration
//public class OpenAiChatClient {
//
//    @Bean("openAiChatModel")
//    public OpenAiChatModel openAiChatModel(OpenAiParamsConfig.OpenAiParams openAiParams,
//                                           ObjectProvider<RestClient.Builder> restClientBuilderProvider) {
//        OpenAiApi openAiApi = OpenAiApi.builder()
//                .baseUrl(openAiParams.baseUrl())
//                .apiKey(openAiParams.apiKey())
//                .restClientBuilder(restClientBuilderProvider.getIfAvailable(RestClient::builder))
//                .build();
//
//        return OpenAiChatModel.builder()
//                .openAiApi(openAiApi)
//                .defaultOptions(OpenAiChatOptions.builder().model(openAiParams.chatModel()).build())
//                .build();
//    }
//
//    @Bean("grokOpenAiChatModel")
//    public OpenAiChatModel grokOpenAiChatModel(OpenAiParamsConfig.OpenAiParams openAiParams,
//                                               ObjectProvider<RestClient.Builder> restClientBuilderProvider) {
//        OpenAiApi openAiApi = OpenAiApi.builder()
//                .baseUrl(openAiParams.grokBaseUrl())
//                .apiKey(openAiParams.grokApiKey())
//                .restClientBuilder(restClientBuilderProvider.getIfAvailable(RestClient::builder))
//                .build();
//
//        return OpenAiChatModel.builder()
//                .openAiApi(openAiApi)
//                .defaultOptions(OpenAiChatOptions.builder().model(openAiParams.grokChatModel()).build())
//                .build();
//    }
//
//    @Bean("chatClient")
//    public ChatClient chatClient(@Qualifier("openAiChatModel") OpenAiChatModel openAiChatModel) {
//        return ChatClient.builder(openAiChatModel).build();
//    }
//
//    @Bean("grokChatClient")
//    public ChatClient grokChatClient(@Qualifier("grokOpenAiChatModel") OpenAiChatModel grokOpenAiChatModel) {
//        return ChatClient.builder(grokOpenAiChatModel).build();
//    }
//}