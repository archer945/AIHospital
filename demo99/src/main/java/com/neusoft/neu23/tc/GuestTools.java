package com.neusoft.neu23.tc;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class GuestTools {
    @Tool(description = "将客户的信息保存到数据库，并返回序列号。但必须收集客户的姓名和手机号")
    public String saveCustomerInfo(
            @ToolParam(description = "客户的姓名，参数name代表客户的姓名") String name,
            @ToolParam(description = "客户的电话号码，参数phone代表客户的电话号码") String phone) {
        System.out.println("name = " + name + ", phone = " + phone);
        return System.currentTimeMillis()+"";
    }
}
