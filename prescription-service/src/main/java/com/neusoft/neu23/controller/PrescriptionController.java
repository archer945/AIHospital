package com.neusoft.neu23.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.neu23.entity.PatientCase;
import com.neusoft.neu23.service.PatientCaseService;
import com.neusoft.neu23.tc.PrescriptionTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.neusoft.neu23.cfg.AiConfig.SYSTEM_PROMPT;


@RestController
@RequestMapping("/prescription")
@CrossOrigin
@Slf4j
public class PrescriptionController {
    private ChatClient chatClient;
    private final PatientCaseService patientCaseService;
    
    public PrescriptionController(OpenAiChatModel openAiChatModel, ChatMemory chatMemory,
                                  PrescriptionTools prescriptionTools,
                                  PatientCaseService patientCaseService) {
        this.chatClient = ChatClient.builder(openAiChatModel)
                .defaultSystem(SYSTEM_PROMPT) // 默认系统角色
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultAdvisors( new SimpleLoggerAdvisor() )
                .defaultTools(prescriptionTools) // 添加处方保存工具
                .build();
        this.patientCaseService = patientCaseService;
    }
    @PostMapping("/diagnosis")
    public ResponseEntity<Map<String, Object>> aiDiagnosis(@RequestParam(value = "msg",defaultValue = "你是谁") String msg,
                                                           @RequestParam( value = "registerId" ,defaultValue = "neu.edu.cn") Integer registerId,
                                                           @RequestParam( value = "patientId" ,defaultValue = "neu.edu.cn") Integer patientId) {
        try {
            // 1. 根据patientId查询患者病例信息
            StringBuilder patientInfoBuilder = new StringBuilder();
            patientInfoBuilder.append("患者当前症状描述：").append(msg).append("\n\n");
            
            if (patientId != null) {
                List<PatientCase> patientCases = patientCaseService.list(
                        new LambdaQueryWrapper<PatientCase>()
                                .eq(PatientCase::getPatientId, patientId)
                                .orderByDesc(PatientCase::getCreateTime)
                );
                
                if (!patientCases.isEmpty()) {
                    patientInfoBuilder.append("【患者历史病例信息】\n");
                    
                    // 获取最新的病例信息（按创建时间倒序，取第一条）
                    PatientCase latestCase = patientCases.get(0);
                    
                    if (latestCase.getSymptoms() != null && !latestCase.getSymptoms().trim().isEmpty() 
                            && !latestCase.getSymptoms().equals("{}")) {
                        patientInfoBuilder.append("历史症状：").append(latestCase.getSymptoms()).append("\n");
                    }
                    
                    if (latestCase.getVitals() != null && !latestCase.getVitals().trim().isEmpty() 
                            && !latestCase.getVitals().equals("{}")) {
                        patientInfoBuilder.append("体征信息：").append(latestCase.getVitals()).append("\n");
                    }
                    
                    if (latestCase.getMedicalHistory() != null && !latestCase.getMedicalHistory().trim().isEmpty() 
                            && !latestCase.getMedicalHistory().equals("{}")) {
                        patientInfoBuilder.append("既往病史：").append(latestCase.getMedicalHistory()).append("\n");
                    }
                    
                    // 如果有多个历史病例，也提供汇总信息
                    if (patientCases.size() > 1) {
                        patientInfoBuilder.append("\n该患者共有 ").append(patientCases.size()).append(" 条历史病例记录。\n");
                    }
                    
                    log.info("查询到患者病例信息，患者ID: {}, 病例数量: {}", patientId, patientCases.size());
                } else {
                    log.warn("未找到患者病例信息，患者ID: {}", patientId);
                    patientInfoBuilder.append("（未找到该患者的历史病例信息）\n");
                }
            }
            
            // 不添加任何要求立即生成处方的提示，让AI根据SYSTEM_PROMPT自行判断
            // 只在需要时提供患者ID和挂号ID信息
            if (patientId != null || registerId != null) {
                patientInfoBuilder.append("\n【患者信息】");
                if (patientId != null) {
                    patientInfoBuilder.append("\n患者ID：").append(patientId);
                }
                if (registerId != null) {
                    patientInfoBuilder.append("\n挂号ID：").append(registerId);
                }
            }
            
            // 2. 调用AI，让AI根据SYSTEM_PROMPT自行判断是否需要收集更多信息
            String diagnosisResult = chatClient.prompt()
                    .user(patientInfoBuilder.toString())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, registerId != null ? registerId.toString() : "default"))
                    .call()
                    .content();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("diagnosis", diagnosisResult);
            result.put("registerId", registerId);
            result.put("patientId", patientId);

            log.info("AI诊断完成，挂号ID: {}, 患者ID: {}", registerId, patientId);
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
