package com.neusoft.neu23.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 映射 spring_ai_chat_memory 表。
 */
@Data
@TableName("spring_ai_chat_memory")
public class ConversationLog {

    @TableField("conversation_id")
    private String conversationId;

    @TableField("content")
    private String content;

    /**
     * 角色：user / assistant / assistant_error。
     */
    @TableField("type")
    private String type;

    @TableField("timestamp")
    private LocalDateTime timestamp;
}

