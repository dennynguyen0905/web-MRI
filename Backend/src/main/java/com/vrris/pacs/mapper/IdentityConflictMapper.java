package com.vrris.pacs.mapper;

import com.vrris.pacs.dto.response.IdentityConflictResponse;
import com.vrris.pacs.model.IdentityConflict;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * model.IdentityConflict.source/resolution la String trong DB; enu.ConflictSource/enu.ConflictResolution
 * o dto - MapStruct tu sinh valueOf()/name() vi cung ten field va kieu dich la enum (giong cach
 * mapper.ImagingOrderMapper voi source/sync_status).
 */
@Mapper(componentModel = "spring")
public interface IdentityConflictMapper {

    @Mapping(target = "resolvedBy", ignore = true)
    IdentityConflictResponse toResponse(IdentityConflict conflict);
}
