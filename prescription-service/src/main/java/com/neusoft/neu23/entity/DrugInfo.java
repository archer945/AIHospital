package com.neusoft.neu23.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("drug_info")
public class DrugInfo {
    
    @TableId(value = "drug_id", type = IdType.AUTO)
    private Long drugId;
    
    @TableField("name")
    private String name;
    
    @TableField("category")
    private String category;
    
    @TableField("indication")
    private String indication;
    
    @TableField("contraindication")
    private String contraindication;
    
    @TableField("side_effect")
    private String sideEffect;
    
    @TableField("dosage")
    private String dosage;
    
    @TableField("unit")
    private String unit;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}