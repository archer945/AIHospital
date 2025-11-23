package com.neusoft.neu23.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("patient_case")
public class PatientCase {
    
    @TableId(value = "case_id", type = IdType.AUTO)
    private Long caseId;
    
    @TableField("patient_id")
    private Integer patientId;
    
    @TableField("symptoms")
    private String symptoms; // JSON格式存储症状
    
    @TableField("vitals")
    private String vitals; // JSON格式存储体征
    
    @TableField("medical_history")
    private String medicalHistory; // JSON格式存储病史
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}