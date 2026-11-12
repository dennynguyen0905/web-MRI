package com.vrris.pacs.dto.response;

import com.vrris.pacs.enu.StudyStatus;

import java.time.LocalDateTime;

/**
 * Khop schema "StudyDetail" trong doc/api/openapi.yaml (IN-08): "allOf" cua Study +
 * patient/order/service_item/device/current_report. Cac truong Study* giong het
 * dto.response.StudyResponse (cung ly do tra id tho cho performed_by/reading_user/approving_user -
 * xem Javadoc do). device/current_report nullable (device co the chua gan, ca chup co the chua co
 * bao cao).
 *
 * Duoc service.impl.StudyServiceImpl.getDetailById() ghep tu Study + cac DAO lien quan (patient qua
 * order.patient_id, order, service_item, device, bao cao hien hanh) trong MOT request, so luong
 * truy van co dinh (khong phu thuoc kich thuoc danh sach nao) - khop tieu chi "khong N+1" cua phieu
 * viec IN-08.
 */
public record StudyDetailResponse(
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
        LocalDateTime updatedAt,
        PatientResponse patient,
        ImagingOrderResponse order,
        ServiceItemResponse serviceItem,
        DeviceResponse device,
        ReportResponse currentReport
) {
}
