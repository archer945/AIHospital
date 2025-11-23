package com.neusoft.neu23.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.entity.DrugInteraction;
import com.neusoft.neu23.mapper.DrugInteractionMapper;
import com.neusoft.neu23.service.DrugInteractionService;
import org.springframework.stereotype.Service;

@Service
public class DrugInteractionServiceImpl extends ServiceImpl<DrugInteractionMapper, DrugInteraction> implements DrugInteractionService {
}

