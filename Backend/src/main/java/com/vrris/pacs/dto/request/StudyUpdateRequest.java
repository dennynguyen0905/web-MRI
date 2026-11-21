package com.vrris.pacs.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * PATCH /studies/{id}: truong null = giu nguyen.
 *
 * study_instance_uid: DICOM UID (0020,000D) - chi gom chu so va dau cham, toi da 64 ky tu
 * (DICOM PS3.5 muc 9.1; cot study.study_instance_uid VARCHAR(64)). Chuoi rong bi tu choi thay vi
 * luu "" (truoc day "" lan thu hai lam vo UNIQUE uk_study_instance_uid -> 500).
 */
public record StudyUpdateRequest(
        Long serviceItemId,
        Long deviceId,
        LocalDateTime scheduledAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        @Size(max = 64, message = "study_instance_uid toi da 64 ky tu")
        @Pattern(regexp = "^[0-9]+(\\.[0-9]+)*$",
                message = "study_instance_uid phai la DICOM UID (chi gom chu so va dau cham, vi du 1.2.840.10008.1)")
        String studyInstanceUid
) {
}
