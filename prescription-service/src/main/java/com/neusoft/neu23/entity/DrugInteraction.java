package com.neusoft.neu23.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("drug_interaction")
public class DrugInteraction {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("drug_a")
    private Long drugA;
    
    @TableField("drug_b")
    private Long drugB;
    
    @TableField("level")
    private String level; // 禁忌/慎用/安全
    
    @TableField("description")
    private String description;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}