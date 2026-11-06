package com.vrris.pacs.dto.response;

import com.vrris.pacs.enu.StudyStatus;

import java.time.LocalDateTime;

/**
 * Khop phan lon schema "Study" trong doc/api/openapi.yaml, TRU performed_by/reading_user/
 * approving_user: schema goc tra ve UserRef (join app_user), nhung model.AppUser/dao.AppUserDao
 * chua co trong pham vi lan nay (chi patient/imaging_order/study) - tra id tho
 * (performedByUserId/readingUserId/approvingUserId), lam sau khi co AppUser.
 */
public record StudyResponse(
        Long id,
        String accessionNumber,
        Long orderId,
        Long serviceItemId,
        Long deviceId,
        StudyStatus status,
        String studyInstanceUid,
        LocalDateTime scheduledAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime dueAt,
        Long performedByUserId,
        Long readingUserId,
        Long approvingUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
