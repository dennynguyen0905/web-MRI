package com.vrris.pacs.dao;

import com.vrris.pacs.model.Patient;

import java.util.List;
import java.util.Optional;

public interface PatientDao extends BaseDao<Patient, Long> {

    Optional<Patient> findByPatientCode(String patientCode);

    /**
     * Dung de phat hien "nghi trung nguoi" khi BHXH khop mot benh nhan khac
     * (doc/04-his-ris-reconciliation.md muc 1.1, truong hop C).
     */
    Optional<Patient> findBySocialInsuranceNo(String socialInsuranceNo);

    SearchResult search(SearchCriteria criteria);

    /**
     * q: tim tu do tren patient_code/full_name/social_insurance_no (GET /patients?q=...).
     * patientCode/fullName: loc rieng tung truong, ket hop AND voi q neu ca hai cung co.
     * sortField: ten field CUA ENTITY (vi du "fullName", khong phai "full_name") - service da validate
     * va doi ten truoc khi goi xuong day, DAO khong tu doan ten cot.
     */
    record SearchCriteria(String q, String patientCode, String fullName,
                           int page, int pageSize, String sortField, boolean ascending) {
    }

    record SearchResult(List<Patient> items, long total) {
    }
}
