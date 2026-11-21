package com.vrris.pacs.service;

import com.vrris.pacs.dto.request.TransitionRequest;
import com.vrris.pacs.dto.response.StatusHistoryEntry;
import com.vrris.pacs.dto.response.TransitionResponse;

import java.util.List;

/** Chuyen trang thai ca chup T2..T10 va lich su trang thai (doc/03-state-machine.md). */
public interface StudyTransitionService {

    /**
     * POST /studies/{id}/transitions (operationId transitionStudy). Luat vai tro/dieu kien o
     * rule.TransitionPolicy; khong hop le -> ApiException 403/409/422. Hai yeu cau dong thoi tren
     * cung ca: yeu cau den sau that bai do khoa lac quan (study.row_version).
     */
    TransitionResponse transition(Long id, TransitionRequest request);

    /** GET /studies/{id}/status-history. */
    List<StatusHistoryEntry> listStatusHistory(Long studyId);
}
