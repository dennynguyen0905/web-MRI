package com.vrris.pacs.mapper;

import com.vrris.pacs.dto.request.StudyCreateRequest;
import com.vrris.pacs.dto.response.StudyResponse;
import com.vrris.pacs.model.Study;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * model.Study.status la String trong DB; enu.StudyStatus o dto - MapStruct tu sinh valueOf()/name()
 * vi cung ten field va kieu dich la enum (giong cach mapper.PatientMapper dang lam voi dob_precision).
 */
@Mapper(componentModel = "spring")
public interface StudyMapper {

    StudyResponse toResponse(Study study);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accessionNumber", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "studyInstanceUid", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "dueAt", ignore = true)
    @Mapping(target = "performedByUserId", ignore = true)
    @Mapping(target = "readingUserId", ignore = true)
    @Mapping(target = "approvingUserId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Study toEntity(StudyCreateRequest request);
}
