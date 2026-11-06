package com.vrris.pacs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Mapping dung bang "study" trong database/migrations/V3__patient_order_study.sql.
 * KHONG doi ten cot / them cot o day - muon doi schema phai them migration V8+ (database/README.md).
 *
 * status luu String (dung kieu cot DB) - chi dua ra enu.StudyStatus o tang dto, giong cach
 * model.Patient dang lam voi dob_precision/gender.
 *
 * performed_by_user_id/reading_user_id/approving_user_id la FK toi app_user - bang/entity do CHUA
 * co trong codebase nay (chi moi patient/imaging_order/study theo pham vi lan nay). Giu nguyen la
 * Long thuan (giong cach model.Device giu department_id la Long, khong @ManyToOne) - dto/response
 * tra ve id tho, KHONG join ra UserRef nhu schema "Study" day du trong doc/api/openapi.yaml -
 * lam sau khi co model.AppUser.
 */
@Entity
@Table(name = "study")
public class Study {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "accession_number", nullable = false, unique = true)
    private String accessionNumber;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "service_item_id", nullable = false)
    private Long serviceItemId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "study_instance_uid", unique = true)
    private String studyInstanceUid;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "performed_by_user_id")
    private Long performedByUserId;

    @Column(name = "reading_user_id")
    private Long readingUserId;

    @Column(name = "approving_user_id")
    private Long approvingUserId;

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

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getServiceItemId() {
        return serviceItemId;
    }

    public void setServiceItemId(Long serviceItemId) {
        this.serviceItemId = serviceItemId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStudyInstanceUid() {
        return studyInstanceUid;
    }

    public void setStudyInstanceUid(String studyInstanceUid) {
        this.studyInstanceUid = studyInstanceUid;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public Long getPerformedByUserId() {
        return performedByUserId;
    }

    public void setPerformedByUserId(Long performedByUserId) {
        this.performedByUserId = performedByUserId;
    }

    public Long getReadingUserId() {
        return readingUserId;
    }

    public void setReadingUserId(Long readingUserId) {
        this.readingUserId = readingUserId;
    }

    public Long getApprovingUserId() {
        return approvingUserId;
    }

    public void setApprovingUserId(Long approvingUserId) {
        this.approvingUserId = approvingUserId;
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
