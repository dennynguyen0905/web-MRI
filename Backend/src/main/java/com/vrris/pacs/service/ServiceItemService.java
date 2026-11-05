package com.vrris.pacs.service;

import com.vrris.pacs.dto.response.ServiceItemResponse;

import java.util.List;

public interface ServiceItemService {

    /** modality nhan String tho - xem ghi chu o DeviceService. */
    List<ServiceItemResponse> listServiceItems(String modality);
}
