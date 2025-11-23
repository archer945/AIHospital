package com.neusoft.neu23.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.entity.PatientCase;
import com.neusoft.neu23.mapper.PatientCaseMapper;
import com.neusoft.neu23.service.PatientCaseService;
import org.springframework.stereotype.Service;

@Service
public class PatientCaseServiceImpl extends ServiceImpl<PatientCaseMapper, PatientCase> implements PatientCaseService {
}

