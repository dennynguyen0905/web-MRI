package com.vrris.pacs.service.impl;

import com.vrris.pacs.audit.AuditContext;
import com.vrris.pacs.audit.Audited;
import com.vrris.pacs.dao.AppUserDao;
import com.vrris.pacs.dto.request.LoginRequest;
import com.vrris.pacs.dto.response.CurrentUserResponse;
import com.vrris.pacs.dto.response.LoginResponse;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.model.AppUser;
import com.vrris.pacs.model.Role;
import com.vrris.pacs.security.JwtService;
import com.vrris.pacs.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Xử lý xác thực đăng nhập/đăng xuất và lấy thông tin người dùng hiện tại (IN-13).
 *
 * Khóa tài khoản sau 5 lần sai liên tiếp trong 15 phút (doc/09 mục 1.4) được đếm trong bộ nhớ
 * (ghi nợ kỹ thuật cho đồ án 8 tuần).
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS_MSG = "Tên đăng nhập hoặc mật khẩu không đúng";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;

    private final AppUserDao appUserDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Lưu số lần đăng nhập sai và thời điểm khóa trong bộ nhớ
    private final Map<String, AttemptInfo> loginAttempts = new ConcurrentHashMap<>();

    public AuthServiceImpl(AppUserDao appUserDao,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.appUserDao = appUserDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Audited(action = AuditAction.LOGIN, entityType = "APP_USER")
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();

        if (isAccountLocked(username)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", INVALID_CREDENTIALS_MSG);
        }

        AppUser user = appUserDao.findByUsername(username).orElse(null);

        if (user == null || !Boolean.TRUE.equals(user.getIsActive()) ||
                !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            recordFailedAttempt(username);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", INVALID_CREDENTIALS_MSG);
        }

        // Đăng nhập thành công: xóa lịch sử đăng nhập sai
        resetFailedAttempts(username);

        List<String> roleCodes = user.getRoles().stream()
                .map(Role::getCode)
                .sorted()
                .toList();

        String token = jwtService.generateToken(user.getId(), user.getUsername(), roleCodes);

        CurrentUserResponse userResponse = new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getDepartmentId(),
                roleCodes
        );

        // Cung cấp thông tin audit log qua AuditContext
        AuditContext.recordActorUsername(user.getUsername());
        AuditContext.recordEntityId(user.getId());

        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), userResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Chua dang nhap hoac token khong hop le");
        }

        String username = auth.getName();
        AppUser user = appUserDao.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Nguoi dung khong ton tai"));

        List<String> roleCodes = user.getRoles().stream()
                .map(Role::getCode)
                .sorted()
                .toList();

        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getDepartmentId(),
                roleCodes
        );
    }

    @Override
    public void logout() {
        // Trong 8 tuần: không duy trì blacklist phía server. Frontend xóa token, token tự hết hạn tự nhiên.
    }

    private synchronized boolean isAccountLocked(String username) {
        String key = username.toLowerCase();
        AttemptInfo info = loginAttempts.get(key);
        if (info != null && info.lockedUntil != null) {
            if (Instant.now().isBefore(info.lockedUntil)) {
                return true;
            } else {
                // Khóa đã hết hạn: reset lại trạng thái
                loginAttempts.remove(key);
            }
        }
        return false;
    }

    private synchronized void recordFailedAttempt(String username) {
        String key = username.toLowerCase();
        AttemptInfo info = loginAttempts.computeIfAbsent(key, k -> new AttemptInfo());
        info.failedCount++;
        if (info.failedCount >= MAX_FAILED_ATTEMPTS) {
            info.lockedUntil = Instant.now().plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES);
        }
    }

    private synchronized void resetFailedAttempts(String username) {
        loginAttempts.remove(username.toLowerCase());
    }

    private static class AttemptInfo {
        int failedCount;
        Instant lockedUntil;
    }
}

