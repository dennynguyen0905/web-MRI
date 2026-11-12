package com.vrris.pacs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Mapping dung bang "identity_conflict" trong database/migrations/V5__audit_and_conflict.sql
 * (IN-05, doc/04-his-ris-reconciliation.md). KHONG doi ten cot / them cot o day - muon doi schema
 * phai them migration V9+ (database/README.md).
 *
 * source/resolution luu String (dung kieu cot DB) - chi dua ra enu.ConflictSource/enu.ConflictResolution
 * o tang dto, mapper.IdentityConflictMapper tu chuyen doi hai chieu (giong cach model.ImagingOrder
 * dang lam voi source/sync_status).
 *
 * Pipeline phat hien/tao dong identity_conflict (doi chieu HIS <-> RIS luc dong bo chi dinh) CHUA
 * co trong pham vi nay (xem comment o ImagingOrderServiceImpl) - entity nay chi phuc vu doc/xu ly
 * cac dong da co (GET/POST /identity-conflicts*).
 */
@Entity
@Table(name = "identity_conflict")
public class IdentityConflict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "ris_value")
    private String risValue;

    @Column(name = "his_value")
    private String hisValue;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "resolution", nullable = false)
    private String resolution;

    @Column(name = "resolved_by_user_id")
    private Long resolvedByUserId;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getRisValue() {
        return risValue;
    }

    public void setRisValue(String risValue) {
        this.risValue = risValue;
    }

    public String getHisValue() {
        return hisValue;
    }

    public void setHisValue(String hisValue) {
        this.hisValue = hisValue;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public Long getResolvedByUserId() {
        return resolvedByUserId;
    }

    public void setResolvedByUserId(Long resolvedByUserId) {
        this.resolvedByUserId = resolvedByUserId;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
