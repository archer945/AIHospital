package com.neusoft.neu23.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.entity.PatientInfo;
import com.neusoft.neu23.mapper.PatientInfoMapper;
import com.neusoft.neu23.service.PatientInfoService;
import org.springframework.stereotype.Service;

@Service
public class PatientInfoServiceImpl extends ServiceImpl<PatientInfoMapper, PatientInfo> implements PatientInfoService {
}

