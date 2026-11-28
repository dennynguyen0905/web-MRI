package com.vrris.pacs.mapper;

import com.vrris.pacs.dto.response.ReportResponse;
import com.vrris.pacs.model.Report;
import org.mapstruct.Mapper;

/**
 * status/origin o model va dto deu la enu.ReportStatus/enu.ReportOrigin - MapStruct map enum -> enum
 * theo ten.
 */
@Mapper(componentModel = "spring")
public interface ReportMapper {

    ReportResponse toResponse(Report report);
}
