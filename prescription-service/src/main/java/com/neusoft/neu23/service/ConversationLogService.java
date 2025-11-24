package com.neusoft.neu23.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.neu23.entity.ConversationLog;

public interface ConversationLogService extends IService<ConversationLog> {

    void recordLog(String conversationId, String role, String content);
}

