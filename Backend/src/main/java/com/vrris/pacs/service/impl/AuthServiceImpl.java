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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.vrris.pacs.service.impl.ServiceSupport.currentUser;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS_MSG = "Tên đăng nhập hoặc mật khẩu không đúng";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;

    private final AppUserDao appUserDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Map<String, AttemptInfo> loginAttempts = new ConcurrentHashMap<>();

    public AuthServiceImpl(AppUserDao appUserDao, PasswordEncoder passwordEncoder, JwtService jwtService) {
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
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            recordFailedAttempt(username);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", INVALID_CREDENTIALS_MSG);
        }

        loginAttempts.remove(username.toLowerCase(Locale.ROOT));

        CurrentUserResponse userResponse = toCurrentUserResponse(user);
        String token = jwtService.generateToken(user.getId(), user.getUsername(), userResponse.roles());

        AuditContext.recordActorUsername(user.getUsername());
        AuditContext.recordEntityId(user.getId());

        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), userResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser() {
        AppUser user = currentUser(appUserDao)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Chua dang nhap hoac token khong hop le"));
        return toCurrentUserResponse(user);
    }

    @Override
    public void logout() {
        // Stateless JWT: frontend xóa token, token tự hết hạn theo TTL.
    }

    private CurrentUserResponse toCurrentUserResponse(AppUser user) {
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
                roleCodes);
    }

    private boolean isAccountLocked(String username) {
        String key = username.toLowerCase(Locale.ROOT);
        AttemptInfo info = loginAttempts.get(key);
        if (info == null || info.lockedUntil() == null) {
            return false;
        }
        if (Instant.now().isBefore(info.lockedUntil())) {
            return true;
        }
        loginAttempts.remove(key, info);
        return false;
    }

    private void recordFailedAttempt(String username) {
        String key = username.toLowerCase(Locale.ROOT);
        loginAttempts.compute(key, (k, current) -> {
            int count = (current == null ? 0 : current.failedCount()) + 1;
            Instant lockedUntil = count >= MAX_FAILED_ATTEMPTS
                    ? Instant.now().plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES)
                    : (current == null ? null : current.lockedUntil());
            return new AttemptInfo(count, lockedUntil);
        });
    }

    private record AttemptInfo(int failedCount, Instant lockedUntil) {
    }
}
