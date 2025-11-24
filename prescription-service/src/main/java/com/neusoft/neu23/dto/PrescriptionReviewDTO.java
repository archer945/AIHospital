package com.neusoft.neu23.dto;

import com.neusoft.neu23.entity.PatientCase;
import com.neusoft.neu23.entity.Prescription;
import com.neusoft.neu23.entity.PrescriptionDrug;
import lombok.Data;

import java.util.List;

@Data
public class PrescriptionReviewDTO {
    private Prescription prescription;
    private PatientCase patientCase;
    private List<PrescriptionDrug> prescriptionDrugs;
}
