package com.neusoft.neu23.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static com.neusoft.neu23.cfg.AiConfig.SYSTEM_PROMPT;


@RestController
@RequestMapping("/prescription")
@CrossOrigin
@Slf4j
public class PrescriptionController {
    private ChatClient chatClient;
    public PrescriptionController(OpenAiChatModel openAiChatModel, ChatMemory chatMemory
    ) {
        this.chatClient = ChatClient.builder(openAiChatModel)
                .defaultSystem(SYSTEM_PROMPT) // 默认系统角色
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultAdvisors( new SimpleLoggerAdvisor() )
                .build();
    }
    @PostMapping("/diagnosis")
    public ResponseEntity<Map<String, Object>> aiDiagnosis(@RequestParam(value = "msg",defaultValue = "你是谁") String msg,
                                                           @RequestParam( value = "registerId" ,defaultValue = "neu.edu.cn") String registerId) {
        try {
            String diagnosisResult = chatClient.prompt()
                    .user(msg)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, registerId))
                    .call()
                    .content();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("diagnosis", diagnosisResult);
            result.put("registerId", registerId);

            log.info("AI诊断完成，挂号ID: {}", registerId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("AI诊断失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "诊断失败: " + e.getMessage()
            ));
        }
    }
}
