package com.vrris.pacs.dto.response;

import com.vrris.pacs.enu.ReportOrigin;
import com.vrris.pacs.enu.ReportStatus;

import java.time.LocalDateTime;

/**
 * Khop phan lon schema "Report" trong doc/api/openapi.yaml, TRU created_by/approved_by: schema goc
 * tra ve UserRef (join app_user), nhung model.AppUser chua co trong codebase nay - tra id tho
 * (createdByUserId/approvedByUserId), giong cach dto.response.StudyResponse dang lam voi
 * performedByUserId/readingUserId/approvingUserId.
 */
public record ReportResponse(
        Long id,
        Long studyId,
        Integer version,
        ReportStatus status,
        ReportOrigin origin,
        String technique,
        String findings,
        String diagnosis,
        String conclusion,
        String recommendation,
        Long createdByUserId,
        LocalDateTime createdAt,
        Long approvedByUserId,
        LocalDateTime approvedAt
) {
}
