package com.vrris.pacs.dto.request;

import com.vrris.pacs.enu.StudyStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionRequest(
        @NotNull(message = "to_status khong duoc de trong")
        StudyStatus toStatus,
        String reason,
        String note
) {
}
