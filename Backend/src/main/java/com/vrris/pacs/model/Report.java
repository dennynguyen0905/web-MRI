package com.vrris.pacs.model;

import com.vrris.pacs.enu.ReportOrigin;
import com.vrris.pacs.enu.ReportStatus;
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
 * Mapping dung bang "report" trong database/migrations/V4__workflow.sql.
 * KHONG doi ten cot / them cot o day - muon doi schema phai them migration V9+ (database/README.md).
 *
 * status/origin la enu.ReportStatus/enu.ReportOrigin, luu ten (name()) vao cot VARCHAR qua @Enumerated(EnumType.STRING) - CSDL khong doi, khong can migration.
 * created_by_user_id/approved_by_user_id giu la Long thuan vi chua co model.AppUser.
 *
 * Ban ghi "hien hanh" la ban co version lon nhat cua mot study (UK(study_id, version)) -
 * xem dao.ReportDao.findCurrentByStudyId.
 */
@Entity
@Table(name = "report")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @Column(name = "origin", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportOrigin origin;

    @Column(name = "technique")
    private String technique;

    @Column(name = "findings")
    private String findings;

    @Column(name = "diagnosis")
    private String diagnosis;

    @Column(name = "conclusion")
    private String conclusion;

    @Column(name = "recommendation")
    private String recommendation;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

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

    public Long getStudyId() {
        return studyId;
    }

    public void setStudyId(Long studyId) {
        this.studyId = studyId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public ReportOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(ReportOrigin origin) {
        this.origin = origin;
    }

    public String getTechnique() {
        return technique;
    }

    public void setTechnique(String technique) {
        this.technique = technique;
    }

    public String getFindings() {
        return findings;
    }

    public void setFindings(String findings) {
        this.findings = findings;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getApprovedByUserId() {
        return approvedByUserId;
    }

    public void setApprovedByUserId(Long approvedByUserId) {
        this.approvedByUserId = approvedByUserId;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Long getRowVersion() {
        return rowVersion;
    }

    public void setRowVersion(Long rowVersion) {
        this.rowVersion = rowVersion;
    }
}
