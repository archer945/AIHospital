package com.neusoft.neu23.cfg;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private final   ChatMemory chatMemory;

    public AiConfig(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    private static final String SYSTEM_PROMPT = """
        【角色设定】你是一名著名的内科医生，你的名字是华佗，你有40多年的临床经验，你非常擅长诊断日常感冒，对感冒治疗有非常
        丰富的经验
        """;
    /**
     * 3  自动配置OpenAiChatModel
     */
    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * 在Spring容器中注入ChatClient
     * @param openAiChatModel
     * @return
     */
    @Bean
    @Qualifier("chatClient0")
    public ChatClient chatClient0(    OpenAiChatModel openAiChatModel ) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(SYSTEM_PROMPT) // 默认系统角色
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultAdvisors( new SimpleLoggerAdvisor() )
                .build();
    }
//    @Bean
//    @Qualifier("chatClient1")
//    public ChatClient chatClient4TC(    OpenAiChatModel openAiChatModel ) {
//        return ChatClient.builder(openAiChatModel)
//                .defaultSystem(HONGHONG_PROMPT) // 默认系统角色
//                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
//                .defaultAdvisors( new SimpleLoggerAdvisor() )
//                .build();
//    }
}
