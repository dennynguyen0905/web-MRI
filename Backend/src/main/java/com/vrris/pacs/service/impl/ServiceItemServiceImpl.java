package com.vrris.pacs.service.impl;

import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dto.response.ServiceItemResponse;
import com.vrris.pacs.enu.Modality;
import com.vrris.pacs.mapper.ServiceItemMapper;
import com.vrris.pacs.service.ServiceItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.vrris.pacs.service.impl.ServiceSupport.parseEnumOrNull;

@Service
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceItemDao serviceItemDao;
    private final ServiceItemMapper serviceItemMapper;

    // Khởi tạo service với DAO và mapper của danh mục dịch vụ chụp.
    public ServiceItemServiceImpl(ServiceItemDao serviceItemDao, ServiceItemMapper serviceItemMapper) {
        this.serviceItemDao = serviceItemDao;
        this.serviceItemMapper = serviceItemMapper;
    }

    // Lấy danh sách dịch vụ chụp, có thể lọc theo modality.
    @Override
    @Transactional(readOnly = true)
    public List<ServiceItemResponse> listServiceItems(String modality) {
        String normalizedModality = parseEnumOrNull(modality, Modality.class, "modality");
        return serviceItemMapper.toResponseList(serviceItemDao.search(normalizedModality));
    }
}
