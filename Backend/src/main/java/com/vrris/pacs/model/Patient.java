package com.vrris.pacs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mapping dung bang "patient" trong database/migrations/V3__patient_order_study.sql.
 * KHONG doi ten cot / them cot o day - muon doi schema phai them migration V8+ (database/README.md).
 *
 * dob_precision va gender luu String (dung kieu cot DB) - chi dua ra enu.DobPrecision/enu.Gender o
 * tang dto, mapper.PatientMapper tu chuyen doi hai chieu (giong cach hibernateDao/model.Device +
 * mapper.DeviceMapper dang lam voi modality).
 *
 * created_at/updated_at/created_by/updated_by do service.impl.PatientServiceImpl dat truc tiep
 * (khong de Hibernate phu thuoc DEFAULT/ON UPDATE CURRENT_TIMESTAMP cua MySQL) - don gian hon cho
 * skeleton nay va tranh lech du lieu do first-level cache khong tu refresh gia tri DB sinh ra.
 */
@Entity
@Table(name = "patient")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_code", nullable = false, unique = true)
    private String patientCode;

    @Column(name = "his_patient_id")
    private String hisPatientId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "dob_precision", nullable = false)
    private String dobPrecision;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "social_insurance_no")
    private String socialInsuranceNo;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public String getHisPatientId() {
        return hisPatientId;
    }

    public void setHisPatientId(String hisPatientId) {
        this.hisPatientId = hisPatientId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getDobPrecision() {
        return dobPrecision;
    }

    public void setDobPrecision(String dobPrecision) {
        this.dobPrecision = dobPrecision;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getSocialInsuranceNo() {
        return socialInsuranceNo;
    }

    public void setSocialInsuranceNo(String socialInsuranceNo) {
        this.socialInsuranceNo = socialInsuranceNo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
