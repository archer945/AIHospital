package com.neusoft.neu23.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.entity.PrescriptionDrug;
import com.neusoft.neu23.mapper.PrescriptionDrugMapper;
import com.neusoft.neu23.service.PrescriptionDrugService;
import org.springframework.stereotype.Service;

@Service
public class PrescriptionDrugServiceImpl extends ServiceImpl<PrescriptionDrugMapper, PrescriptionDrug> implements PrescriptionDrugService {
}

