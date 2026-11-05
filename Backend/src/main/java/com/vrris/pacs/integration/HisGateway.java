package com.vrris.pacs.integration;

import com.vrris.pacs.enu.DobPrecision;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cong vao du lieu tu HIS (01-scope.md muc 4.1). Trong 8 tuan chi co 1 implementation gia lap
 * (integration.impl.JsonFileHisGateway, doc tu file JSON tinh). Ban thuc (Hl7HisGateway, HL7 v2
 * qua MLLP) la H-03, ngoai pham vi 8 tuan - xem doc/08-hl7-message-spec.md muc 10.
 *
 * HisOrderSnapshot mo rong 2026-11-05 theo doc/08-hl7-message-spec.md muc 8.3 (tu 3 field len 17
 * field) - khop du lieu can cho doi chieu HIS <-> RIS (doc/04-his-ris-reconciliation.md: PID-3/5/7/8)
 * va cho imaging_order (doc/02-erd.md: visit_code, patient_class, room, bed, ordering_doctor_name,
 * clinical_diagnosis, service_item). Moi ten field va comment ORC-x/PID-x/PV1-x/OBR-x giu dung
 * nhu dac ta - khong doi them.
 */
public interface HisGateway {

    Optional<HisOrderSnapshot> findOrder(String hisOrderId);

    /**
     * Snapshot tu HIS, dung cho quy tac doi chieu HIS <-> RIS (README 2.5,
     * doc/04-his-ris-reconciliation.md). Khong phai JPA entity - day la kieu rieng cua tang
     * integration, service chiu trach nhiem doi chieu/map sang model khi can (de xuat dat logic
     * do o rule.IdentityReconciler - chua co code, xem doc/08-hl7-message-spec.md muc 8.3).
     */
    record HisOrderSnapshot(
            String hisOrderId,          // ORC-2
            String patientCode,         // PID-3 (PI)
            String socialInsuranceNo,   // PID-3 (BHXH), nullable
            String fullName,            // PID-5, da ghep
            LocalDate dateOfBirth,      // PID-7, nullable
            DobPrecision dobPrecision,  // suy tu do dai PID-7
            String gender,              // PID-8, da chuan hoa M|F|O|U
            String address,             // PID-11, nullable
            String phone,               // PID-13, nullable
            String visitCode,           // PV1-19, nullable
            String patientClass,        // PV1-2, da chuan hoa INPATIENT|OUTPATIENT
            String room,                // PV1-3 component 2, nullable
            String bed,                 // PV1-3 component 3, nullable
            String orderingDoctorName,  // PV1-8
            String clinicalDiagnosis,   // OBR-31, nullable
            String serviceItemCode,     // OBR-4 component 1 - tra service_item theo code nay
            LocalDateTime scheduledAt,  // OBR-6
            String messageControlId     // MSH-10 - danh cho kiem idempotency sau nay (H-03)
    ) {
    }
}
