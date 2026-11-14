package com.vrris.pacs.dao;

import com.vrris.pacs.model.Role;

import java.util.Optional;

public interface RoleDao extends BaseDao<Role, Long> {

    Optional<Role> findByCode(String code);
}

