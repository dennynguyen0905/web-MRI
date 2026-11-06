package com.vrris.pacs.mapper;

import com.vrris.pacs.dto.request.ImagingOrderCreateRequest;
import com.vrris.pacs.dto.request.ImagingOrderUpdateRequest;
import com.vrris.pacs.dto.response.ImagingOrderResponse;
import com.vrris.pacs.model.ImagingOrder;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * model.ImagingOrder.source/sync_status/patient_class la String trong DB; enu tuong ung o dto -
 * MapStruct tu sinh valueOf()/name() vi cung ten field va kieu dich la enum (giong
 * mapper.PatientMapper voi dob_precision/gender).
 */
@Mapper(componentModel = "spring")
public interface ImagingOrderMapper {

    @Mapping(target = "studies", ignore = true)
    ImagingOrderResponse toResponse(ImagingOrder order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderCode", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "hisOrderId", ignore = true)
    @Mapping(target = "syncStatus", ignore = true)
    @Mapping(target = "orderedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ImagingOrder toEntity(ImagingOrderCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderCode", ignore = true)
    @Mapping(target = "patientId", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "hisOrderId", ignore = true)
    @Mapping(target = "syncStatus", ignore = true)
    @Mapping(target = "orderedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ImagingOrderUpdateRequest request, @MappingTarget ImagingOrder order);
}
