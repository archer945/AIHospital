package com.neusoft.neu23.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("register")
public class Register {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("card_number")
    private String cardNumber;

    @TableField("real_name")
    private String realName;

    @TableField("employee_id")
    private Integer employeeId;
}

