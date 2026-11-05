package com.vrris.pacs.integration.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vrris.pacs.integration.HisGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gia lap doc tu file JSON tinh (01-scope.md muc 4.1, doc/08-hl7-message-spec.md muc 9,
 * doc/12-seed-data-va-kich-ban-demo.md muc 3.2). Doc toan bo file JSON MOT lan luc khoi dong bean
 * (du lieu la file tinh, khong doi trong luc backend chay), nap vao map theo hisOrderId.
 *
 * Dung ObjectMapper RIENG (khong phai bean Jackson mac dinh cua Spring) vi file nay dung quy uoc
 * camelCase dung nhu ten field Java (hisOrderId, patientCode, ...) - bean Jackson cua Spring o day
 * dang cau hinh property-naming-strategy: SNAKE_CASE (application.yml, cho API /api/v1/...), neu
 * dung chung se khong doc duoc field nao (Jackson se tim "his_order_id" thay vi "hisOrderId" va
 * luon nhan null). his-orders.json la du lieu NOI BO cua gateway gia lap, khong phai payload API,
 * nen khong can theo quy uoc JSON cua OpenAPI.
 */
@Component
public class JsonFileHisGateway implements HisGateway {

    private static final Logger log = LoggerFactory.getLogger(JsonFileHisGateway.class);

    private final Map<String, HisOrderSnapshot> ordersByHisOrderId;

    public JsonFileHisGateway(
            ResourceLoader resourceLoader,
            @Value("${app.his.mock-data-path:classpath:his-mock/his-orders.json}") String mockDataPath) {
        this.ordersByHisOrderId = load(resourceLoader, mockDataPath);
    }

    @Override
    public Optional<HisOrderSnapshot> findOrder(String hisOrderId) {
        if (hisOrderId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ordersByHisOrderId.get(hisOrderId));
    }

    private static Map<String, HisOrderSnapshot> load(ResourceLoader resourceLoader, String mockDataPath) {
        Resource resource = resourceLoader.getResource(mockDataPath);
        if (!resource.exists()) {
            log.warn("JsonFileHisGateway: khong tim thay file du lieu gia lap '{}' - moi findOrder() se tra "
                    + "Optional.empty() (giong hanh vi khung cu truoc 2026-11-05)", mockDataPath);
            return Collections.emptyMap();
        }
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        try (InputStream in = resource.getInputStream()) {
            List<HisOrderSnapshot> snapshots = mapper.readValue(
                    in, mapper.getTypeFactory().constructCollectionType(List.class, HisOrderSnapshot.class));
            Map<String, HisOrderSnapshot> byId = new HashMap<>();
            for (HisOrderSnapshot snapshot : snapshots) {
                byId.put(snapshot.hisOrderId(), snapshot);
            }
            log.info("JsonFileHisGateway: nap {} ban ghi gia lap tu '{}'", byId.size(), mockDataPath);
            return Collections.unmodifiableMap(byId);
        } catch (IOException e) {
            // doc/08-hl7-message-spec.md muc 8.4: de xuat nem HisGatewayException rieng cho loi doc
            // file (khac "khong tim thay ban ghi" = Optional.empty()) - CHUA lam trong lan sua nay
            // (exception/ chua co lop do), ghi log loi va coi nhu rong de khong lam sap boot backend
            // vi mot file mau demo. ❓ giu nguyen de xuat cua 08 cho Pha sau.
            log.warn("JsonFileHisGateway: loi doc file du lieu gia lap '{}' - moi findOrder() se tra "
                    + "Optional.empty()", mockDataPath, e);
            return Collections.emptyMap();
        }
    }
}
