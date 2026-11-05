package com.vrris.pacs.mapper;

import com.vrris.pacs.dto.response.DeviceResponse;
import com.vrris.pacs.model.Device;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * device.modality la String trong DB (model.Device.getModality()); DeviceResponse.modality la
 * enu.Modality - MapStruct tu sinh Modality.valueOf(...) vi hai ten field giong nhau va kieu dich
 * la enum (khong can @Mapping rieng).
 */
@Mapper(componentModel = "spring")
public interface DeviceMapper {

    DeviceResponse toResponse(Device device);

    List<DeviceResponse> toResponseList(List<Device> devices);
}
