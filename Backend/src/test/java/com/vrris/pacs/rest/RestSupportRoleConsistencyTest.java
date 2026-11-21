package com.vrris.pacs.rest;

import com.vrris.pacs.enu.UserRole;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bieu thuc @PreAuthorize phai la hang so chuoi luc bien dich (gioi han cua annotation Java), nen
 * RestSupport khong dung truc tiep enu.UserRole duoc. Test nay khoa hai ben voi nhau: moi 'ROLE_X'
 * trong RestSupport phai la mot UserRole co that - go sai ten vai tro thi test do, thay vi am tham
 * chan het moi nguoi o runtime.
 */
class RestSupportRoleConsistencyTest {

    private static final Pattern AUTHORITY = Pattern.compile("'(ROLE_[A-Z_]+)'");

    @Test
    void moi_vai_tro_trong_PreAuthorize_deu_co_trong_UserRole() throws Exception {
        List<String> authorities = new ArrayList<>();
        for (Field field : RestSupport.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                field.setAccessible(true);
                Matcher m = AUTHORITY.matcher((String) field.get(null));
                while (m.find()) {
                    authorities.add(m.group(1));
                }
            }
        }

        assertThat(authorities).isNotEmpty();
        assertThat(authorities)
                .allSatisfy(a -> assertThat(UserRole.fromAuthority(a))
                        .as("'%s' trong RestSupport khong khop enu.UserRole nao", a)
                        .isNotNull());
    }
}
