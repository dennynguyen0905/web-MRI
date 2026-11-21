package com.vrris.pacs.mapper;

import com.vrris.pacs.dto.response.DeviceResponse;
import com.vrris.pacs.model.Device;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * model.Device.modality va DeviceResponse.modality deu la enu.Modality - MapStruct map enum -> enum theo
 * ten, khong can @Mapping rieng.
 */
@Mapper(componentModel = "spring")
public interface DeviceMapper {

    DeviceResponse toResponse(Device device);

    List<DeviceResponse> toResponseList(List<Device> devices);
}
