package com.vrris.pacs.service.impl;

import com.vrris.pacs.dao.DeviceDao;
import com.vrris.pacs.dto.response.DeviceResponse;
import com.vrris.pacs.enu.Modality;
import com.vrris.pacs.mapper.DeviceMapper;
import com.vrris.pacs.service.DeviceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.vrris.pacs.service.impl.ServiceSupport.parseEnumOrNull;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceDao deviceDao;
    private final DeviceMapper deviceMapper;

    // Khởi tạo service với DAO và mapper của máy chụp.
    public DeviceServiceImpl(DeviceDao deviceDao, DeviceMapper deviceMapper) {
        this.deviceDao = deviceDao;
        this.deviceMapper = deviceMapper;
    }

    // Lấy danh sách máy chụp, có thể lọc theo khoa và modality.
    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> listDevices(Long departmentId, String modality) {
        Modality normalizedModality = parseEnumOrNull(modality, Modality.class, "modality");
        return deviceMapper.toResponseList(deviceDao.search(departmentId, normalizedModality));
    }
}
