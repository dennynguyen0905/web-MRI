package com.vrris.pacs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Mapping dung bang "imaging_order" trong database/migrations/V3__patient_order_study.sql.
 * KHONG doi ten cot / them cot o day - muon doi schema phai them migration V8+ (database/README.md).
 *
 * source/sync_status/patient_class luu String (dung kieu cot DB) - chi dua ra enu.OrderSource/
 * enu.SyncStatus/enu.PatientClass o tang dto, mapper.ImagingOrderMapper tu chuyen doi hai chieu
 * (giong cach model.Patient dang lam voi dob_precision/gender).
 *
 * Bang nay khong co created_by/updated_by (khac patient) - dung theo dung cot trong V3, khong them.
 */
@Entity
@Table(name = "imaging_order")
public class ImagingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true)
    private String orderCode;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "visit_code")
    private String visitCode;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "his_order_id")
    private String hisOrderId;

    @Column(name = "sync_status", nullable = false)
    private String syncStatus;

    @Column(name = "ordering_department_id", nullable = false)
    private Long orderingDepartmentId;

    @Column(name = "ordering_doctor_name")
    private String orderingDoctorName;

    @Column(name = "patient_class", nullable = false)
    private String patientClass;

    @Column(name = "room")
    private String room;

    @Column(name = "bed")
    private String bed;

    @Column(name = "clinical_diagnosis")
    private String clinicalDiagnosis;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getVisitCode() {
        return visitCode;
    }

    public void setVisitCode(String visitCode) {
        this.visitCode = visitCode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getHisOrderId() {
        return hisOrderId;
    }

    public void setHisOrderId(String hisOrderId) {
        this.hisOrderId = hisOrderId;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public Long getOrderingDepartmentId() {
        return orderingDepartmentId;
    }

    public void setOrderingDepartmentId(Long orderingDepartmentId) {
        this.orderingDepartmentId = orderingDepartmentId;
    }

    public String getOrderingDoctorName() {
        return orderingDoctorName;
    }

    public void setOrderingDoctorName(String orderingDoctorName) {
        this.orderingDoctorName = orderingDoctorName;
    }

    public String getPatientClass() {
        return patientClass;
    }

    public void setPatientClass(String patientClass) {
        this.patientClass = patientClass;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getBed() {
        return bed;
    }

    public void setBed(String bed) {
        this.bed = bed;
    }

    public String getClinicalDiagnosis() {
        return clinicalDiagnosis;
    }

    public void setClinicalDiagnosis(String clinicalDiagnosis) {
        this.clinicalDiagnosis = clinicalDiagnosis;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public void setOrderedAt(LocalDateTime orderedAt) {
        this.orderedAt = orderedAt;
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
}
