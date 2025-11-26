package com.neusoft.neu23.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.dto.PrescriptionReviewDTO;
import com.neusoft.neu23.entity.PatientCase;
import com.neusoft.neu23.entity.Prescription;
import com.neusoft.neu23.entity.PrescriptionDrug;
import com.neusoft.neu23.entity.DrugInfo;
import com.neusoft.neu23.mapper.PrescriptionMapper;
import com.neusoft.neu23.service.PatientCaseService;
import com.neusoft.neu23.service.PrescriptionDrugService;
import com.neusoft.neu23.service.PrescriptionService;
import com.neusoft.neu23.service.DrugInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PrescriptionServiceImpl extends ServiceImpl<PrescriptionMapper, Prescription> implements PrescriptionService {

    @Autowired
    private PatientCaseService patientCaseService;

    @Autowired
    private PrescriptionDrugService prescriptionDrugService;

    @Autowired
    private DrugInfoService drugInfoService;

    @Override
    public PrescriptionReviewDTO getPrescriptionForReview(Long prescriptionId) {
        Prescription prescription = getById(prescriptionId);
        if (prescription == null) {
            return null;
        }

        PatientCase patientCase = patientCaseService.getOne(
                new LambdaQueryWrapper<PatientCase>()
                        .eq(PatientCase::getPatientId, prescription.getPatientId())
                        .orderByDesc(PatientCase::getCreateTime)
                        .last("limit 1")
        );

        List<PrescriptionDrug> prescriptionDrugs = prescriptionDrugService.list(
                new LambdaQueryWrapper<PrescriptionDrug>()
                        .eq(PrescriptionDrug::getPrescriptionId, prescriptionId)
        );

        // 获取所有药品信息映射
        List<Long> drugIds = prescriptionDrugs.stream()
                .map(PrescriptionDrug::getDrugId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, DrugInfo> drugInfoMap = new HashMap<>();
        if (!drugIds.isEmpty()) {
            drugInfoService.listByIds(drugIds).forEach(info -> drugInfoMap.put(info.getDrugId(), info));
        }

        // 为每个处方药品设置药品名称
        prescriptionDrugs.forEach(drug -> {
            DrugInfo info = drugInfoMap.get(drug.getDrugId());
            if (info != null) {
                drug.setDrugName(info.getName());
            }
        });

        PrescriptionReviewDTO reviewDTO = new PrescriptionReviewDTO();
        reviewDTO.setPrescription(prescription);
        reviewDTO.setPatientCase(patientCase);
        reviewDTO.setPrescriptionDrugs(prescriptionDrugs);

        return reviewDTO;
    }

    @Override
    @Transactional
    public boolean reviewPrescription(Prescription prescription, List<PrescriptionDrug> prescriptionDrugs) {
        // 1. 更新处方状态为已审核 (1)
        prescription.setStatus(1);
        boolean prescriptionUpdated = updateById(prescription);

        if (!prescriptionUpdated) {
            return false;
        }

        // 2. 删除旧的处方药品
        prescriptionDrugService.remove(
                new LambdaQueryWrapper<PrescriptionDrug>()
                        .eq(PrescriptionDrug::getPrescriptionId, prescription.getPrescriptionId())
        );

        // 3. 插入新的处方药品
        if (prescriptionDrugs != null && !prescriptionDrugs.isEmpty()) {
            prescriptionDrugs.forEach(drug -> drug.setPrescriptionId(prescription.getPrescriptionId()));
            return prescriptionDrugService.saveBatch(prescriptionDrugs);
        }

        return true;
    }
}

