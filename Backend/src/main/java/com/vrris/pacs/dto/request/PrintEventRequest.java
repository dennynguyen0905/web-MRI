package com.vrris.pacs.dto.request;

import com.vrris.pacs.enu.PrintSection;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Body cua POST /patients/{id}/print-events: ghi nhan mot lan in bang hop thoai trinh duyet.
 *
 * @param studyId  ca chup duoc in
 * @param sections cac phan da in trong lan do
 */
public record PrintEventRequest(
        @NotNull Long studyId,
        @NotEmpty List<PrintSection> sections) {
}
