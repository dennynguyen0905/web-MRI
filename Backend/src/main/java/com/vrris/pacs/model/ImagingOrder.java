package com.vrris.pacs.model;

import com.vrris.pacs.enu.OrderSource;
import com.vrris.pacs.enu.PatientClass;
import com.vrris.pacs.enu.SyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * Mapping dung bang "imaging_order" trong database/migrations/V3__patient_order_study.sql.
 * KHONG doi ten cot / them cot o day - muon doi schema phai them migration V8+ (database/README.md).
 *
 * source/sync_status/patient_class la enu.OrderSource/SyncStatus/PatientClass, luu ten (name()) vao cot VARCHAR qua @Enumerated(EnumType.STRING) - CSDL khong doi, khong can migration.
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
    @Enumerated(EnumType.STRING)
    private OrderSource source;

    @Column(name = "his_order_id")
    private String hisOrderId;

    @Column(name = "sync_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SyncStatus syncStatus;

    @Column(name = "ordering_department_id", nullable = false)
    private Long orderingDepartmentId;

    @Column(name = "ordering_doctor_name")
    private String orderingDoctorName;

    @Column(name = "patient_class", nullable = false)
    @Enumerated(EnumType.STRING)
    private PatientClass patientClass;

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

    /** Khoa lac quan (V9, cot row_version) - hai yeu cau dong thoi khong cung ghi duoc (doc/03 muc 2.4 dieu 5). */
    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

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

    public OrderSource getSource() {
        return source;
    }

    public void setSource(OrderSource source) {
        this.source = source;
    }

    public String getHisOrderId() {
        return hisOrderId;
    }

    public void setHisOrderId(String hisOrderId) {
        this.hisOrderId = hisOrderId;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
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

    public PatientClass getPatientClass() {
        return patientClass;
    }

    public void setPatientClass(PatientClass patientClass) {
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

    public Long getRowVersion() {
        return rowVersion;
    }

    public void setRowVersion(Long rowVersion) {
        this.rowVersion = rowVersion;
    }
}
