package com.vrris.pacs.dao;

import com.vrris.pacs.model.ServiceItem;

import java.util.List;

public interface ServiceItemDao extends BaseDao<ServiceItem, Long> {

    /** modality la filter tuy chon tren GET /service-items (doc/api/openapi.yaml). */
    List<ServiceItem> search(String modality);
}
