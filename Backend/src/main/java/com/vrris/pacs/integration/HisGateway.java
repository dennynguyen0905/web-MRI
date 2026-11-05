package com.vrris.pacs.integration;

import java.util.Optional;

/**
 * Cong vao du lieu tu HIS (01-scope.md muc 4.1). Trong 8 tuan chi co 1 implementation gia lap
 * (integration.impl.JsonFileHisGateway, doc tu file JSON tinh). Ban thuc (Hl7HisGateway, HL7 v2
 * qua MLLP) la H-03, ngoai pham vi 8 tuan - xem doc/08-hl7-message-spec.md (chua viet).
 */
public interface HisGateway {

    Optional<HisOrderSnapshot> findOrder(String hisOrderId);

    /**
     * Snapshot toi thieu tu HIS, dung cho quy tac doi chieu HIS <-> RIS (README 2.5,
     * doc/04-his-ris-reconciliation.md). Khong phai JPA entity - day la kieu rieng cua tang
     * integration, service chiu trach nhiem doi chieu/map sang model khi can.
     */
    record HisOrderSnapshot(String hisOrderId, String patientCode, String fullName) {
    }
}
