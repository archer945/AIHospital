package com.neusoft.neu23.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.entity.ConversationLog;
import com.neusoft.neu23.mapper.ConversationLogMapper;
import com.neusoft.neu23.service.ConversationLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationLogServiceImpl extends ServiceImpl<ConversationLogMapper, ConversationLog>
        implements ConversationLogService {

    @Override
    public void recordLog(String conversationId, String role, String content) {
        ConversationLog log = new ConversationLog();
        log.setConversationId(conversationId);
        log.setType(role);
        log.setContent(content);
        log.setTimestamp(java.time.LocalDateTime.now());
        this.save(log);
    }

    @Override
    public List<ConversationLog> listByConversationId(String conversationId) {
        return this.list(new LambdaQueryWrapper<ConversationLog>()
                .eq(ConversationLog::getConversationId, conversationId)
                .orderByAsc(ConversationLog::getTimestamp));
    }
}

