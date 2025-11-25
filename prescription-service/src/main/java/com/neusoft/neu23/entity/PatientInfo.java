package com.neusoft.neu23.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("patient_info")
public class PatientInfo {

    @TableId(value = "patient_id", type = IdType.AUTO)
    private Integer patientId;

    @TableField("card_number")
    private String cardNumber;

    @TableField("real_name")
    private String realName;
}

