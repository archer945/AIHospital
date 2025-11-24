package com.neusoft.neu23.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.neu23.entity.ConversationLog;

import java.util.List;

public interface ConversationLogService extends IService<ConversationLog> {

    void recordLog(String conversationId, String role, String content);

    /**
     * 查询指定会话的全部消息，按时间升序。
     */
    List<ConversationLog> listByConversationId(String conversationId);
}

