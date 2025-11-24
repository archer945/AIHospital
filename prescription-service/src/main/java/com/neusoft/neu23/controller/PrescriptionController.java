package com.neusoft.neu23.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.neu23.dto.PrescriptionReviewDTO;
import com.neusoft.neu23.dto.PrescriptionSubmitDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.entity.ConversationLog;
import com.neusoft.neu23.entity.PatientCase;
import com.neusoft.neu23.service.ConversationLogService;
import com.neusoft.neu23.service.PatientCaseService;
import com.neusoft.neu23.service.PrescriptionService;
import com.neusoft.neu23.tc.PrescriptionTools;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.neusoft.neu23.cfg.AiConfig.SYSTEM_PROMPT;


@RestController
@RequestMapping("/prescription")
@CrossOrigin
@Slf4j
@Validated
public class PrescriptionController {
    private ChatClient chatClient;
    private final PatientCaseService patientCaseService;

    @Autowired
    private PrescriptionService prescriptionService;
    private final ConversationLogService conversationLogService;
    
    public PrescriptionController(OpenAiChatModel openAiChatModel, ChatMemory chatMemory,
                                  PrescriptionTools prescriptionTools,
                                  PatientCaseService patientCaseService,
                                  ConversationLogService conversationLogService) {
        this.chatClient = ChatClient.builder(openAiChatModel)
                .defaultSystem(SYSTEM_PROMPT) // 默认系统角色
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultAdvisors( new SimpleLoggerAdvisor() )
                .defaultTools(prescriptionTools) // 添加处方保存工具
                .build();
        this.patientCaseService = patientCaseService;
        this.conversationLogService = conversationLogService;
    }
    @PostMapping("/diagnosis")
    public ResponseEntity<Map<String, Object>> aiDiagnosis(@RequestParam(value = "msg") @NotBlank String msg,
                                                           @RequestParam(value = "registerId", required = false) @Min(1) Integer registerId,
                                                           @RequestParam(value = "patientId", required = false) @Min(1) Integer patientId) {
        String conversationId = resolveConversationId(registerId, patientId);
        try {
            // 记录医生输入
            conversationLogService.recordLog(conversationId, "user", msg);

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
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();

            // 记录AI回复
            conversationLogService.recordLog(conversationId, "assistant", diagnosisResult);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("diagnosis", diagnosisResult);
            result.put("registerId", registerId);
            result.put("patientId", patientId);

            log.info("AI诊断完成，挂号ID: {}, 患者ID: {}", registerId, patientId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMsg = "诊断失败: " + e.getMessage();
            conversationLogService.recordLog(conversationId, "assistant_error", errorMsg);
            log.error("AI诊断失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", errorMsg
            ));
        }
    }

    @GetMapping("/review/{prescriptionId}")
    public ResponseEntity<PrescriptionReviewDTO> getPrescriptionForReview(@PathVariable Long prescriptionId) {
        PrescriptionReviewDTO reviewDTO = prescriptionService.getPrescriptionForReview(prescriptionId);
        if (reviewDTO == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reviewDTO);
    }

    @PostMapping("/submitReview")
    public ResponseEntity<Map<String, Object>> submitPrescriptionReview(@RequestBody PrescriptionSubmitDTO submitDTO) {
        try {
            boolean success = prescriptionService.reviewPrescription(submitDTO.getPrescription(), submitDTO.getPrescriptionDrugs());
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "处方审核提交成功"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "处方审核提交失败"));
            }
        } catch (Exception e) {
            log.error("提交处方审核失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "提交处方审核失败: " + e.getMessage()));
        }
    /**
     * 医生查询历史对话
     */
    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getConversations(@RequestParam(value = "registerId", required = false) @Min(1) Integer registerId,
                                                                @RequestParam(value = "patientId", required = false) @Min(1) Integer patientId,
                                                                @RequestParam(value = "page", defaultValue = "1") @Min(1) long page,
                                                                @RequestParam(value = "size", defaultValue = "20") @Min(1) long size) {
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        if (registerId != null || patientId != null) {
            wrapper.eq(ConversationLog::getConversationId, resolveConversationId(registerId, patientId));
        }
        wrapper.orderByDesc(ConversationLog::getTimestamp);

        Page<ConversationLog> pageResult =
                conversationLogService.page(new Page<>(page, size), wrapper);

        Map<String, Object> payload = new HashMap<>();
        payload.put("success", true);
        payload.put("records", pageResult.getRecords());
        payload.put("total", pageResult.getTotal());
        payload.put("page", pageResult.getCurrent());
        payload.put("size", pageResult.getSize());

        return ResponseEntity.ok(payload);
    }

    private String resolveConversationId(Integer registerId, Integer patientId) {
        return Optional.ofNullable(registerId)
                .map(id -> "register-" + id)
                .orElseGet(() -> Optional.ofNullable(patientId)
                        .map(id -> "patient-" + id)
                        .orElse("general"));
    }

}
