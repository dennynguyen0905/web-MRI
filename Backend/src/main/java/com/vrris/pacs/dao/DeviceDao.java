package com.vrris.pacs.dao;

import com.vrris.pacs.model.Device;

import java.util.List;

public interface DeviceDao extends BaseDao<Device, Long> {

    /**
     * department_id va modality la filter tuy chon tren GET /devices (doc/api/openapi.yaml).
     * Ca hai co the null - khi do khong loc theo dieu kien tuong ung.
     */
    List<Device> search(Long departmentId, String modality);
}
