package com.neusoft.neu23.tc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.neu23.entity.DrugInfo;
import com.neusoft.neu23.entity.PatientCase;
import com.neusoft.neu23.entity.Prescription;
import com.neusoft.neu23.entity.PrescriptionDrug;
import com.neusoft.neu23.entity.DrugInteraction;
import com.neusoft.neu23.service.DrugInfoService;
import com.neusoft.neu23.service.DrugInteractionService;
import com.neusoft.neu23.service.PatientCaseService;
import com.neusoft.neu23.service.PrescriptionDrugService;
import com.neusoft.neu23.service.PrescriptionService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处方保存工具类
 * 用于将 AI 生成的处方数据保存到数据库
 */
@Component
@Slf4j
public class PrescriptionTools {

    private final PrescriptionService prescriptionService;
    private final PrescriptionDrugService prescriptionDrugService;
    private final PatientCaseService patientCaseService;
    private final DrugInfoService drugInfoService;
    private final DrugInteractionService drugInteractionService;
    private final ObjectMapper objectMapper;

    public PrescriptionTools(PrescriptionService prescriptionService,
                            PrescriptionDrugService prescriptionDrugService,
                            PatientCaseService patientCaseService,
                            DrugInfoService drugInfoService,
                            DrugInteractionService drugInteractionService) {
        this.prescriptionService = prescriptionService;
        this.prescriptionDrugService = prescriptionDrugService;
        this.patientCaseService = patientCaseService;
        this.drugInfoService = drugInfoService;
        this.drugInteractionService = drugInteractionService;
        // 配置ObjectMapper忽略未知属性，支持字段名映射
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 查询药品信息
     * 根据药品名称、适应症或分类查询药品信息
     */
    @Tool(description = "查询药品信息。可以根据药品名称、适应症或分类查询数据库中的药品信息，用于设计处方时选择合适的药品。")
    public String queryDrugInfo(
            @ToolParam(description = "药品名称，支持模糊查询，可为空") String drugName,
            @ToolParam(description = "适应症关键词，可为空") String indication,
            @ToolParam(description = "药品分类，可为空") String category) {
        try {
            LambdaQueryWrapper<DrugInfo> queryWrapper = new LambdaQueryWrapper<>();
            
            if (drugName != null && !drugName.trim().isEmpty()) {
                queryWrapper.like(DrugInfo::getName, drugName.trim());
            }
            if (indication != null && !indication.trim().isEmpty()) {
                queryWrapper.like(DrugInfo::getIndication, indication.trim());
            }
            if (category != null && !category.trim().isEmpty()) {
                queryWrapper.eq(DrugInfo::getCategory, category.trim());
            }
            
            List<DrugInfo> drugList = drugInfoService.list(queryWrapper);
            
            if (drugList.isEmpty()) {
                return "未找到匹配的药品信息。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("找到 %d 种药品：\n\n", drugList.size()));
            
            for (DrugInfo drug : drugList) {
                result.append(String.format("药品ID: %d\n", drug.getDrugId()));
                result.append(String.format("药品名称: %s\n", drug.getName()));
                result.append(String.format("分类: %s\n", drug.getCategory() != null ? drug.getCategory() : "未分类"));
                result.append(String.format("适应症: %s\n", drug.getIndication() != null ? drug.getIndication() : "无"));
                result.append(String.format("禁忌症: %s\n", drug.getContraindication() != null ? drug.getContraindication() : "无"));
                result.append(String.format("副作用: %s\n", drug.getSideEffect() != null ? drug.getSideEffect() : "无"));
                result.append(String.format("剂量: %s %s\n", drug.getDosage() != null ? drug.getDosage() : "未指定", 
                        drug.getUnit() != null ? drug.getUnit() : ""));
                result.append("---\n");
            }
            
            log.info("查询药品信息成功，找到 {} 种药品", drugList.size());
            return result.toString();
            
        } catch (Exception e) {
            log.error("查询药品信息失败: {}", e.getMessage(), e);
            return "查询药品信息失败: " + e.getMessage();
        }
    }

    /**
     * 查询药品相互作用
     * 检查两个药品之间是否存在相互作用
     */
    @Tool(description = "查询两个药品之间的相互作用。根据药品ID或药品名称检查两个药品之间是否存在相互作用，用于设计处方时避免禁忌药品组合。")
    public String queryDrugInteraction(
            @ToolParam(description = "第一个药品的ID或名称") String drugA,
            @ToolParam(description = "第二个药品的ID或名称") String drugB) {
        try {
            // 先根据名称查找药品ID
            Long drugAId = getDrugIdByNameOrId(drugA);
            Long drugBId = getDrugIdByNameOrId(drugB);
            
            if (drugAId == null || drugBId == null) {
                return String.format("无法找到药品信息。drugA: %s, drugB: %s", drugA, drugB);
            }
            
            // 查询相互作用（双向查询）
            LambdaQueryWrapper<DrugInteraction> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.and(wrapper -> 
                wrapper.and(w -> w.eq(DrugInteraction::getDrugA, drugAId).eq(DrugInteraction::getDrugB, drugBId))
                      .or(w -> w.eq(DrugInteraction::getDrugA, drugBId).eq(DrugInteraction::getDrugB, drugAId))
            );
            
            List<DrugInteraction> interactions = drugInteractionService.list(queryWrapper);
            
            if (interactions.isEmpty()) {
                return String.format("药品 %s 和 %s 之间未发现相互作用，可以安全使用。", drugA, drugB);
            }
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("发现药品 %s 和 %s 之间的相互作用：\n\n", drugA, drugB));
            
            for (DrugInteraction interaction : interactions) {
                result.append(String.format("相互作用级别: %s\n", interaction.getLevel()));
                result.append(String.format("描述: %s\n", interaction.getDescription() != null ? interaction.getDescription() : "无"));
                result.append("---\n");
            }
            
            log.info("查询药品相互作用成功，drugA: {}, drugB: {}", drugA, drugB);
            return result.toString();
            
        } catch (Exception e) {
            log.error("查询药品相互作用失败: {}", e.getMessage(), e);
            return "查询药品相互作用失败: " + e.getMessage();
        }
    }

    /**
     * 检查多个药品之间的相互作用
     * 用于在设计处方时检查多个药品组合的安全性
     */
    @Tool(description = "检查多个药品之间的相互作用。根据药品名称列表（JSON格式字符串数组），检查这些药品之间是否存在相互作用，用于设计处方时确保药品组合的安全性。")
    public String checkMultipleDrugInteractions(
            @ToolParam(description = "药品名称列表，JSON格式字符串数组，例如：[\"阿莫西林\",\"布洛芬\"]") String drugNames) {
        try {
            List<String> drugNameList = objectMapper.readValue(drugNames,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            
            if (drugNameList == null || drugNameList.isEmpty()) {
                return "药品列表为空。";
            }
            
            // 获取所有药品ID
            List<Long> drugIds = new ArrayList<>();
            Map<Long, String> drugIdToName = new HashMap<>();
            
            for (String drugName : drugNameList) {
                Long drugId = getDrugIdByNameOrId(drugName);
                if (drugId != null) {
                    drugIds.add(drugId);
                    drugIdToName.put(drugId, drugName);
                } else {
                    log.warn("未找到药品: {}", drugName);
                }
            }
            
            if (drugIds.size() < 2) {
                return "至少需要2种药品才能检查相互作用。";
            }
            
            // 检查所有药品对之间的相互作用
            List<DrugInteraction> allInteractions = new ArrayList<>();
            for (int i = 0; i < drugIds.size(); i++) {
                for (int j = i + 1; j < drugIds.size(); j++) {
                    Long drugAId = drugIds.get(i);
                    Long drugBId = drugIds.get(j);
                    
                    LambdaQueryWrapper<DrugInteraction> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.and(wrapper -> 
                        wrapper.and(w -> w.eq(DrugInteraction::getDrugA, drugAId).eq(DrugInteraction::getDrugB, drugBId))
                              .or(w -> w.eq(DrugInteraction::getDrugA, drugBId).eq(DrugInteraction::getDrugB, drugAId))
                    );
                    
                    List<DrugInteraction> interactions = drugInteractionService.list(queryWrapper);
                    allInteractions.addAll(interactions);
                }
            }
            
            if (allInteractions.isEmpty()) {
                return String.format("检查了 %d 种药品之间的相互作用，未发现任何相互作用，可以安全使用。", drugIds.size());
            }
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("检查了 %d 种药品，发现以下相互作用：\n\n", drugIds.size()));
            
            for (DrugInteraction interaction : allInteractions) {
                String drugAName = drugIdToName.get(interaction.getDrugA());
                String drugBName = drugIdToName.get(interaction.getDrugB());
                result.append(String.format("药品组合: %s 和 %s\n", drugAName, drugBName));
                result.append(String.format("相互作用级别: %s\n", interaction.getLevel()));
                result.append(String.format("描述: %s\n", interaction.getDescription() != null ? interaction.getDescription() : "无"));
                result.append("---\n");
            }
            
            log.info("检查多个药品相互作用成功，药品数量: {}, 发现相互作用: {}", drugIds.size(), allInteractions.size());
            return result.toString();
            
        } catch (Exception e) {
            log.error("检查多个药品相互作用失败: {}", e.getMessage(), e);
            return "检查多个药品相互作用失败: " + e.getMessage();
        }
    }

    /**
     * 根据药品名称或ID获取药品ID
     */
    private Long getDrugIdByNameOrId(String drugNameOrId) {
        try {
            // 尝试作为ID解析
            try {
                Long drugId = Long.parseLong(drugNameOrId);
                DrugInfo drug = drugInfoService.getById(drugId);
                if (drug != null) {
                    return drug.getDrugId();
                }
            } catch (NumberFormatException e) {
                // 不是数字，继续按名称查找
            }
            
            // 按名称查找
            DrugInfo drug = drugInfoService.getOne(
                    new LambdaQueryWrapper<DrugInfo>()
                            .eq(DrugInfo::getName, drugNameOrId)
                            .last("LIMIT 1")
            );
            
            return drug != null ? drug.getDrugId() : null;
        } catch (Exception e) {
            log.error("获取药品ID失败: {}", e.getMessage());
            return null;
        }
    }

    @Tool(description = "保存处方到数据库。根据诊断结果、症状、推荐药品等信息创建处方记录。必须提供挂号ID、患者ID、诊断结果和至少一个推荐药品。推荐药品应使用药品名称，系统会自动查询药品ID。")
    public String savePrescription(
            @ToolParam(description = "挂号ID，整数类型") Integer registerId,
            @ToolParam(description = "患者ID，字符串类型") Integer patientId,
            @ToolParam(description = "诊断结果，字符串类型") String diagnosis,
            @ToolParam(description = "症状列表，JSON格式字符串数组，例如：[\"发热\",\"咳嗽\"]") String symptoms,
            @ToolParam(description = "治疗方案，字符串类型") String treatmentPlan,
            @ToolParam(description = "推荐药品列表，JSON格式字符串，格式：[{\"drug_name\":\"药品名称\",\"dosage\":\"剂量\",\"usage\":\"使用方法\",\"note\":\"注意事项\"}]") String recommendedDrugs,
            @ToolParam(description = "药品相互作用信息，JSON格式字符串，可为空") String drugInteractions,
            @ToolParam(description = "警告信息，字符串类型，可为空") String warnings,
            @ToolParam(description = "健康管理建议，字符串类型，可为空") String advice,
            @ToolParam(description = "医生ID，整数类型，可为空，默认为0表示未审核") Integer doctorId) {
        
        try {
            log.info("开始保存处方，挂号ID: {}, 患者ID: {}", registerId, patientId);
            
            // 1. 保存患者病例信息
            PatientCase patientCase = new PatientCase();
            patientCase.setPatientId(patientId);
            patientCase.setSymptoms(symptoms);
            patientCase.setVitals("{}"); // 体征信息，可根据需要扩展
            patientCase.setMedicalHistory("{}"); // 病史信息，可根据需要扩展
            patientCaseService.save(patientCase);
            log.info("患者病例保存成功，病例ID: {}", patientCase.getCaseId());
            
            // 2. 构建 AI 推荐信息 JSON
            String aiRecommendation = buildAiRecommendation(treatmentPlan, recommendedDrugs, 
                    drugInteractions, warnings, advice);
            
            // 3. 保存处方主记录
            Prescription prescription = new Prescription();
            prescription.setRegisterId(registerId);
            prescription.setPatientId(patientId);
            prescription.setDiagnosis(diagnosis);
            prescription.setAiRecommendation(aiRecommendation);
            prescription.setDoctorId(doctorId != null ? doctorId : 0);
            prescription.setStatus(0); // 0未审核，1已审核
            prescriptionService.save(prescription);
            log.info("处方保存成功，处方ID: {}", prescription.getPrescriptionId());
            
            // 4. 解析并保存处方药品
            List<PrescriptionDrug> prescriptionDrugs = parseAndSaveDrugs(
                    prescription.getPrescriptionId(), recommendedDrugs);
            
            return String.format("处方保存成功！处方ID: %d, 包含 %d 种药品", 
                    prescription.getPrescriptionId(), prescriptionDrugs.size());
            
        } catch (Exception e) {
            log.error("保存处方失败: {}", e.getMessage(), e);
            return "保存处方失败: " + e.getMessage();
        }
    }

    /**
     * 构建 AI 推荐信息 JSON
     */
    private String buildAiRecommendation(String treatmentPlan, String recommendedDrugs,
                                        String drugInteractions, String warnings, String advice) {
        try {
            PrescriptionRecommendation recommendation = new PrescriptionRecommendation();
            recommendation.setTreatmentPlan(treatmentPlan);
            recommendation.setRecommendedDrugs(parseRecommendedDrugs(recommendedDrugs));
            recommendation.setDrugInteractions(parseDrugInteractions(drugInteractions));
            recommendation.setWarnings(warnings);
            recommendation.setAdvice(advice);
            return objectMapper.writeValueAsString(recommendation);
        } catch (JsonProcessingException e) {
            log.error("构建 AI 推荐信息失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 解析推荐药品列表
     */
    private List<DrugInfoDTO> parseRecommendedDrugs(String recommendedDrugsJson) {
        try {
            if (recommendedDrugsJson == null || recommendedDrugsJson.trim().isEmpty()) {
                log.warn("推荐药品JSON为空");
                return new ArrayList<>();
            }
            
            // 使用已配置的ObjectMapper（已在构造函数中配置忽略未知属性）
            List<DrugInfoDTO> result = objectMapper.readValue(recommendedDrugsJson, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DrugInfoDTO.class));
            
            log.info("成功解析 {} 个推荐药品", result.size());
            return result;
        } catch (Exception e) {
            log.error("解析推荐药品列表失败: {}, JSON内容: {}", e.getMessage(), recommendedDrugsJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * 解析药品相互作用信息
     */
    private List<DrugInteractionDTO> parseDrugInteractions(String drugInteractionsJson) {
        try {
            if (drugInteractionsJson == null || drugInteractionsJson.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(drugInteractionsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DrugInteractionDTO.class));
        } catch (Exception e) {
            log.error("解析药品相互作用信息失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 解析并保存处方药品
     * 根据药品名称查询数据库中的药品信息，并保存到 prescription_drug 表
     */
    private List<PrescriptionDrug> parseAndSaveDrugs(Long prescriptionId, String recommendedDrugsJson) {
        List<PrescriptionDrug> prescriptionDrugs = new ArrayList<>();
        
        try {
            List<DrugInfoDTO> drugList = parseRecommendedDrugs(recommendedDrugsJson);
            
            if (drugList.isEmpty()) {
                log.warn("推荐药品列表为空，无法保存处方药品。JSON内容: {}", recommendedDrugsJson);
                return prescriptionDrugs;
            }
            
            log.info("开始保存 {} 个处方药品到数据库", drugList.size());
            
            // 检查药品之间的相互作用
            List<String> drugNames = new ArrayList<>();
            for (DrugInfoDTO drugDTO : drugList) {
                drugNames.add(drugDTO.getDrugName());
            }
            try {
                String drugNamesJson = objectMapper.writeValueAsString(drugNames);
                String interactionCheck = checkMultipleDrugInteractions(drugNamesJson);
                log.info("药品相互作用检查结果: {}", interactionCheck);
            } catch (Exception e) {
                log.warn("检查药品相互作用时出错: {}", e.getMessage());
            }
            
            for (DrugInfoDTO drugDTO : drugList) {
                try {
                    // 检查药品名称是否为空
                    if (drugDTO.getDrugName() == null || drugDTO.getDrugName().trim().isEmpty()) {
                        log.warn("药品名称为空，跳过该药品");
                        continue;
                    }
                    
                    // 根据药品名称查找药品ID和详细信息
                    DrugInfo drugInfo = drugInfoService.getOne(
                            new LambdaQueryWrapper<DrugInfo>()
                                    .eq(DrugInfo::getName, drugDTO.getDrugName().trim())
                                    .last("LIMIT 1")
                    );
                    
                    Long drugId = null;
                    String finalDosage = drugDTO.getDosage();
                    String finalReason = "";
                    
                    if (drugInfo != null) {
                        drugId = drugInfo.getDrugId();
                        // 如果AI没有提供剂量，使用数据库中的默认剂量
                        if (finalDosage == null || finalDosage.trim().isEmpty()) {
                            finalDosage = drugInfo.getDosage() != null ? drugInfo.getDosage() : "按说明书";
                        }
                        
                        // 构建处方药品原因说明，包含数据库中的信息
                        StringBuilder reasonBuilder = new StringBuilder();
                        reasonBuilder.append(String.format("剂量: %s", finalDosage));
                        if (drugDTO.getUsage() != null && !drugDTO.getUsage().trim().isEmpty()) {
                            reasonBuilder.append(String.format(", 用法: %s", drugDTO.getUsage()));
                        }
                        if (drugInfo.getIndication() != null && !drugInfo.getIndication().trim().isEmpty()) {
                            reasonBuilder.append(String.format(", 适应症: %s", drugInfo.getIndication()));
                        }
                        if (drugDTO.getNote() != null && !drugDTO.getNote().trim().isEmpty()) {
                            reasonBuilder.append(String.format(", 注意事项: %s", drugDTO.getNote()));
                        }
                        if (drugInfo.getContraindication() != null && !drugInfo.getContraindication().trim().isEmpty()) {
                            reasonBuilder.append(String.format(", 禁忌症: %s", drugInfo.getContraindication()));
                        }
                        finalReason = reasonBuilder.toString();
                        
                        log.info("找到药品信息: {} (ID: {})", drugDTO.getDrugName(), drugId);
                    } else {
                        log.warn("未找到药品: {}, 将保存药品名称（drugId为null）", drugDTO.getDrugName());
                        // 即使找不到药品，也保存记录，但 drugId 为 null
                        finalDosage = drugDTO.getDosage() != null && !drugDTO.getDosage().trim().isEmpty() 
                                ? drugDTO.getDosage() : "未指定";
                        finalReason = String.format("剂量: %s, 用法: %s, 注意事项: %s", 
                                finalDosage, 
                                drugDTO.getUsage() != null && !drugDTO.getUsage().trim().isEmpty() 
                                        ? drugDTO.getUsage() : "未指定",
                                drugDTO.getNote() != null && !drugDTO.getNote().trim().isEmpty() 
                                        ? drugDTO.getNote() : "无");
                    }
                    
                    // 保存处方药品到 prescription_drug 表
                    PrescriptionDrug prescriptionDrug = new PrescriptionDrug();
                    prescriptionDrug.setPrescriptionId(prescriptionId);
                    prescriptionDrug.setDrugId(drugId);
                    prescriptionDrug.setDosage(finalDosage != null ? finalDosage : "未指定");
                    prescriptionDrug.setReason(finalReason);
                    
                    boolean saved = prescriptionDrugService.save(prescriptionDrug);
                    if (saved) {
                        prescriptionDrugs.add(prescriptionDrug);
                        log.info("处方药品保存成功: {} (处方药品ID: {}, 药品ID: {})", 
                                drugDTO.getDrugName(), prescriptionDrug.getId(), drugId);
                    } else {
                        log.error("处方药品保存失败: {}", drugDTO.getDrugName());
                    }
                } catch (Exception e) {
                    log.error("保存单个处方药品失败: {}, 错误: {}", drugDTO.getDrugName(), e.getMessage(), e);
                    // 继续处理下一个药品
                }
            }
            
            log.info("处方药品保存完成，成功保存 {} 个药品", prescriptionDrugs.size());
        } catch (Exception e) {
            log.error("解析并保存处方药品失败: {}", e.getMessage(), e);
        }
        
        return prescriptionDrugs;
    }

    /**
     * 处方推荐信息 DTO
     */
    private static class PrescriptionRecommendation {
        private String treatmentPlan;
        private List<DrugInfoDTO> recommendedDrugs;
        private List<DrugInteractionDTO> drugInteractions;
        private String warnings;
        private String advice;

        // Getters and Setters
        public String getTreatmentPlan() { return treatmentPlan; }
        public void setTreatmentPlan(String treatmentPlan) { this.treatmentPlan = treatmentPlan; }
        public List<DrugInfoDTO> getRecommendedDrugs() { return recommendedDrugs; }
        public void setRecommendedDrugs(List<DrugInfoDTO> recommendedDrugs) { this.recommendedDrugs = recommendedDrugs; }
        public List<DrugInteractionDTO> getDrugInteractions() { return drugInteractions; }
        public void setDrugInteractions(List<DrugInteractionDTO> drugInteractions) { this.drugInteractions = drugInteractions; }
        public String getWarnings() { return warnings; }
        public void setWarnings(String warnings) { this.warnings = warnings; }
        public String getAdvice() { return advice; }
        public void setAdvice(String advice) { this.advice = advice; }
    }

    /**
     * 药品信息 DTO
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DrugInfoDTO {
        @JsonProperty("drug_name")
        private String drugName;
        private String dosage;
        private String usage;
        private String note;

        // Getters and Setters
        public String getDrugName() { return drugName; }
        public void setDrugName(String drugName) { this.drugName = drugName; }
        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }
        public String getUsage() { return usage; }
        public void setUsage(String usage) { this.usage = usage; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    /**
     * 药品相互作用 DTO
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DrugInteractionDTO {
        @JsonProperty("drug_a")
        private String drugA;
        @JsonProperty("drug_b")
        private String drugB;
        @JsonProperty("interaction_level")
        private String interactionLevel;
        private String description;

        // Getters and Setters
        public String getDrugA() { return drugA; }
        public void setDrugA(String drugA) { this.drugA = drugA; }
        public String getDrugB() { return drugB; }
        public void setDrugB(String drugB) { this.drugB = drugB; }
        public String getInteractionLevel() { return interactionLevel; }
        public void setInteractionLevel(String interactionLevel) { this.interactionLevel = interactionLevel; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}

