package com.vrris.pacs.mapper;

import com.vrris.pacs.dto.response.IdentityConflictResponse;
import com.vrris.pacs.model.IdentityConflict;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * source/resolution o model va dto deu la enu.ConflictSource/enu.ConflictResolution - MapStruct map
 * enum -> enum theo ten.
 */
@Mapper(componentModel = "spring")
public interface IdentityConflictMapper {

    @Mapping(target = "resolvedBy", ignore = true)
    IdentityConflictResponse toResponse(IdentityConflict conflict);
}
