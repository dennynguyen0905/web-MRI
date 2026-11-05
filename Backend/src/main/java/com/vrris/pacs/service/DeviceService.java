package com.vrris.pacs.service;

import com.vrris.pacs.dto.response.DeviceResponse;

import java.util.List;

public interface DeviceService {

    /**
     * modality nhan dang String tho tu query param (rest khong phu thuoc enu - chi service/dto);
     * gia tri khong hop le nem ApiException (VALIDATION_ERROR, 400) thay cho co che convert tu dong
     * cua Spring (xem service.impl.DeviceServiceImpl).
     */
    List<DeviceResponse> listDevices(Long departmentId, String modality);
}
