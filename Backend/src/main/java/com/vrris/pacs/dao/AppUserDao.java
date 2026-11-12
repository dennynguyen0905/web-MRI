package com.vrris.pacs.dao;

import com.vrris.pacs.model.AppUser;

import java.util.Optional;

public interface AppUserDao extends BaseDao<AppUser, Long> {

    /** Dung de doi username (claim "sub" cua JWT) sang id, so sanh voi Study.readingUserId (IN-10). */
    Optional<AppUser> findByUsername(String username);
}
