package com.vrris.pacs.mapper;

import com.vrris.pacs.dto.response.ServiceItemResponse;
import com.vrris.pacs.model.ServiceItem;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServiceItemMapper {

    ServiceItemResponse toResponse(ServiceItem serviceItem);

    List<ServiceItemResponse> toResponseList(List<ServiceItem> serviceItems);
}
