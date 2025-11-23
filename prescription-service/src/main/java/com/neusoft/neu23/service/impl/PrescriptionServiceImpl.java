package com.neusoft.neu23.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.entity.Prescription;
import com.neusoft.neu23.mapper.PrescriptionMapper;
import com.neusoft.neu23.service.PrescriptionService;
import org.springframework.stereotype.Service;

@Service
public class PrescriptionServiceImpl extends ServiceImpl<PrescriptionMapper, Prescription> implements PrescriptionService {
}

