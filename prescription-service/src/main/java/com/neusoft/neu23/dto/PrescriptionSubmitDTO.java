package com.neusoft.neu23.dto;

import com.neusoft.neu23.entity.Prescription;
import com.neusoft.neu23.entity.PrescriptionDrug;
import lombok.Data;

import java.util.List;

@Data
public class PrescriptionSubmitDTO {
    private Prescription prescription;
    private List<PrescriptionDrug> prescriptionDrugs;
}
