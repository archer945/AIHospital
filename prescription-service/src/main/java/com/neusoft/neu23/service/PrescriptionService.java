package com.neusoft.neu23.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.neu23.dto.PrescriptionReviewDTO;
import com.neusoft.neu23.entity.Prescription;
import com.neusoft.neu23.entity.PrescriptionDrug;

import java.util.List;

public interface PrescriptionService extends IService<Prescription> {
    PrescriptionReviewDTO getPrescriptionForReview(Long prescriptionId);
    boolean reviewPrescription(Prescription prescription, List<PrescriptionDrug> prescriptionDrugs);
}