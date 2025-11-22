package com.neusoft.neu23.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("prescription")
public class Prescription {
    
    @TableId(value = "prescription_id", type = IdType.AUTO)
    private Long prescriptionId;
    
    @TableField("register_id")
    private Integer registerId;
    
    @TableField("patient_id")
    private String patientId;
    
    @TableField("diagnosis")
    private String diagnosis;
    
    @TableField("ai_recommendation")
    private String aiRecommendation; // JSON格式存储AI建议
    
    @TableField("doctor_id")
    private Integer doctorId;
    
    @TableField("status")
    private Integer status; // 0未审核，1已审核
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}