package com.vrris.pacs.service.impl;

import com.vrris.pacs.dao.ServiceItemDao;
import com.vrris.pacs.dto.response.ServiceItemResponse;
import com.vrris.pacs.enu.Modality;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.mapper.ServiceItemMapper;
import com.vrris.pacs.service.ServiceItemService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceItemDao serviceItemDao;
    private final ServiceItemMapper serviceItemMapper;

    public ServiceItemServiceImpl(ServiceItemDao serviceItemDao, ServiceItemMapper serviceItemMapper) {
        this.serviceItemDao = serviceItemDao;
        this.serviceItemMapper = serviceItemMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceItemResponse> listServiceItems(String modality) {
        String normalizedModality = parseModality(modality);
        return serviceItemMapper.toResponseList(serviceItemDao.search(normalizedModality));
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
