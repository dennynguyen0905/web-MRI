package com.vrris.pacs.service.impl;

import com.vrris.pacs.dao.ImagingOrderDao;
import com.vrris.pacs.dao.StudyDao;
import com.vrris.pacs.enu.StudyStatus;
import com.vrris.pacs.enu.UserRole;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.model.ImagingOrder;
import com.vrris.pacs.model.Study;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Tien ich noi bo dung chung cho cac service luong ca chup (StudyServiceImpl,
 * StudyTransitionServiceImpl, StudyExportServiceImpl). Package-private (R-05).
 */
final class StudyServiceSupport {

    // Không cho khởi tạo instance — class chỉ chứa hàm tiện ích static dùng chung cho các service luồng ca chụp.
    private StudyServiceSupport() {
    }

    // Tìm ca chụp theo id, báo lỗi 404 nếu không tồn tại.
    static Study findStudyOrThrow(StudyDao studyDao, Long id) {
        return studyDao.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDY_NOT_FOUND",
                        "Khong tim thay ca chup id=" + id));
    }

    // Tìm chỉ định chụp theo id, báo lỗi 404 nếu không tồn tại.
    static ImagingOrder findOrderOrThrow(ImagingOrderDao imagingOrderDao, Long orderId) {
        return imagingOrderDao.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IMAGING_ORDER_NOT_FOUND",
                        "Khong tim thay chi dinh id=" + orderId));
    }

    // Tra ra id bệnh nhân của một ca chụp thông qua chỉ định chụp liên quan.
    static Long resolvePatientId(ImagingOrderDao imagingOrderDao, Study study) {
        if (study == null || study.getOrderId() == null) {
            return null;
        }
        return imagingOrderDao.findById(study.getOrderId())
                .map(ImagingOrder::getPatientId)
                .orElse(null);
    }

    /**
     * Trang thai hien tai cua ca chup. Model da map cot study.status sang enu.StudyStatus
     * (@Enumerated) nen chi con phai chan truong hop NULL (du lieu lech, cot NOT NULL trong DB).
     */
    static StudyStatus parseStoredStatus(Study study) {
        StudyStatus status = study.getStatus();
        if (status == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STUDY_STATUS_INVALID",
                    "Ca chup id=" + study.getId() + " khong co trang thai trong CSDL");
        }
        return status;
    }

    /** Cac vai tro (enu.UserRole) cua nguoi dang dang nhap; rong neu chua dang nhap. */
    // Lấy tập vai trò (enu.UserRole) của người đang đăng nhập từ Authentication; trả tập rỗng nếu chưa đăng nhập.
    static Set<UserRole> rolesOf(Authentication auth) {
        Set<UserRole> roles = EnumSet.noneOf(UserRole.class);
        if (auth == null) {
            return roles;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            UserRole role = UserRole.fromAuthority(authority.getAuthority());
            if (role != null) {
                roles.add(role);
            }
        }
        return roles;
    }

    // Kiểm tra người đang đăng nhập có giữ bất kỳ vai trò nào trong danh sách truyền vào hay không.
    static boolean hasAnyRole(Authentication auth, UserRole... roles) {
        Set<UserRole> actual = rolesOf(auth);
        for (UserRole role : roles) {
            if (actual.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
