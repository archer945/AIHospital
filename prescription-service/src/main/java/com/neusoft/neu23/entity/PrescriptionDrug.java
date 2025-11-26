package com.neusoft.neu23.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("prescription_drug")
public class PrescriptionDrug {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("prescription_id")
    private Long prescriptionId;
    
    @TableField("drug_id")
    private Long drugId;
    
    @TableField("dosage")
    private String dosage;
    
    @TableField("reason")
    private String reason;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String drugName;
}