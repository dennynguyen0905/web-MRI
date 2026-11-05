package com.vrris.pacs.service.impl;

import com.vrris.pacs.dao.DeviceDao;
import com.vrris.pacs.dto.response.DeviceResponse;
import com.vrris.pacs.enu.Modality;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.DeviceMapper;
import com.vrris.pacs.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceDao deviceDao;
    private final DeviceMapper deviceMapper;

    public DeviceServiceImpl(DeviceDao deviceDao, DeviceMapper deviceMapper) {
        this.deviceDao = deviceDao;
        this.deviceMapper = deviceMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> listDevices(Long departmentId, String modality) {
        String normalizedModality = parseModality(modality);
        return deviceMapper.toResponseList(deviceDao.search(departmentId, normalizedModality));
    }

    private String parseModality(String modality) {
        if (modality == null) {
            return null;
        }
        try {
            return Modality.valueOf(modality).name();
        } catch (IllegalArgumentException ex) {
            String issue = "Gia tri khong hop le cho tham so 'modality'";
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", issue);
        }
    }
}
