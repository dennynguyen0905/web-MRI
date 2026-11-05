package com.vrris.pacs.integration.impl;

import com.vrris.pacs.integration.HisGateway;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Gia lap doc tu file JSON tinh - khung toi thieu, CHUA doc file thuc (chua co du lieu mau, xem
 * doc/12-seed-data-va-kich-ban-demo.md - chua viet). Tra Optional.empty() cho moi truy van.
 */
@Component
public class JsonFileHisGateway implements HisGateway {

    @Override
    public Optional<HisOrderSnapshot> findOrder(String hisOrderId) {
        return Optional.empty();
    }
}
