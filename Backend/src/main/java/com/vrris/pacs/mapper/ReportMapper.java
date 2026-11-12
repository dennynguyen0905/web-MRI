package com.vrris.pacs.mapper;

import com.vrris.pacs.dto.response.ReportResponse;
import com.vrris.pacs.model.Report;
import org.mapstruct.Mapper;

/**
 * report.status/report.origin la String trong DB (model.Report); enu.ReportStatus/enu.ReportOrigin
 * o dto - MapStruct tu sinh valueOf() vi cung ten field va kieu dich la enum (giong cach
 * mapper.DeviceMapper dang lam voi modality).
 */
@Mapper(componentModel = "spring")
public interface ReportMapper {

    ReportResponse toResponse(Report report);
}
