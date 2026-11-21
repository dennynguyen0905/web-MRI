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

/**
 * Dang nhap JWT (IN-13). Chinh sach khoa: sai 5 lan -> khoa 15 phut theo username (giu nguyen).
 *
 * Bo dem dat trong bo nho (1 replica - doc/14 muc 8.2): mat khi restart va khong chia se giua cac
 * replica - ❓ neu tang replica can chuyen sang DB/Redis. Ban ghi tu het han sau LOCKOUT_DURATION ke
 * tu lan sai cuoi va map bi gioi han MAX_TRACKED_USERNAMES (don ban ghi het han khi day) de khong
 * phinh vo han khi bi thu hang loat username.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS_MSG = "Tên đăng nhập hoặc mật khẩu không đúng";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;
    private static final int MAX_TRACKED_USERNAMES = 10_000;

    private final AppUserDao appUserDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Map<String, AttemptInfo> loginAttempts = new ConcurrentHashMap<>();
    /**
     * Hash gia de van chay passwordEncoder.matches khi username khong ton tai/khong active - thoi gian
     * phan hoi giong truong hop sai mat khau, khong dung do thoi gian de do username co ton tai.
     */
    private final String dummyPasswordHash;

    // Khởi tạo service với DAO người dùng, bộ mã hóa mật khẩu, JwtService; tạo sẵn hash giả để chống đo thời gian phản hồi khi dò username.
    public AuthServiceImpl(AppUserDao appUserDao, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.appUserDao = appUserDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyPasswordHash = passwordEncoder.encode("vr-ris-timing-equalizer-not-a-real-password");
    }

    // Đăng nhập: kiểm khóa tài khoản, so khớp mật khẩu (dùng hash giả nếu user không tồn tại/không active để đồng nhất thời gian phản hồi), phát JWT nếu hợp lệ.
    @Override
    @Audited(action = AuditAction.LOGIN, entityType = "APP_USER")
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        // actor cho dong audit LOGIN / LOGIN_FAILED (chua co SecurityContext luc dang nhap).
        AuditContext.recordActorUsername(username);

        if (isAccountLocked(username)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", INVALID_CREDENTIALS_MSG);
        }

        AppUser user = appUserDao.findByUsername(username).orElse(null);
        boolean usable = user != null && Boolean.TRUE.equals(user.getIsActive());
        boolean passwordOk = passwordEncoder.matches(request.getPassword(),
                usable ? user.getPasswordHash() : dummyPasswordHash);
        if (!usable || !passwordOk) {
            recordFailedAttempt(username);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", INVALID_CREDENTIALS_MSG);
        }

        loginAttempts.remove(key(username));

        CurrentUserResponse userResponse = toCurrentUserResponse(user);
        String token = jwtService.generateToken(user.getId(), user.getUsername(), userResponse.getRoles());

        AuditContext.recordActorUsername(user.getUsername());
        AuditContext.recordEntityId(user.getId());

        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), userResponse);
    }

    // Lấy thông tin người dùng hiện tại từ token đã xác thực trong SecurityContext.
    @Override
    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser() {
        AppUser user = currentUser(appUserDao)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Chua dang nhap hoac token khong hop le"));
        return toCurrentUserResponse(user);
    }

    // Đăng xuất: không cần xử lý phía server vì dùng JWT stateless, frontend tự xóa token.
    @Override
    public void logout() {
        // Stateless JWT: frontend xóa token, token tự hết hạn theo TTL.
    }

    // Chuyển entity AppUser sang DTO CurrentUserResponse kèm danh sách mã vai trò đã sắp xếp.
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

    // Chuẩn hóa username về dạng viết thường để dùng làm khóa trong map đếm số lần đăng nhập sai.
    private static String key(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    // Kiểm tra tài khoản có đang bị khóa tạm hay không; dọn bản ghi nếu đã hết hạn.
    private boolean isAccountLocked(String username) {
        String key = key(username);
        AttemptInfo info = loginAttempts.get(key);
        if (info == null) {
            return false;
        }
        Instant now = Instant.now();
        if (info.lockedUntil() != null && now.isBefore(info.lockedUntil())) {
            return true;
        }
        if (info.isExpired(now)) {
            loginAttempts.remove(key, info);
        }
        return false;
    }

    // Ghi nhận một lần đăng nhập sai; khóa tài khoản tạm nếu vượt quá số lần cho phép; dọn bớt bản ghi hết hạn khi bảng đếm đầy.
    private void recordFailedAttempt(String username) {
        Instant now = Instant.now();
        if (loginAttempts.size() >= MAX_TRACKED_USERNAMES) {
            loginAttempts.entrySet().removeIf(e -> e.getValue().isExpired(now));
        }
        loginAttempts.compute(key(username), (k, current) -> {
            // Ban ghi da het han (khoa da qua hoac khong sai them trong 15 phut) -> dem lai tu dau.
            AttemptInfo base = current == null || current.isExpired(now) ? null : current;
            int count = (base == null ? 0 : base.failedCount()) + 1;
            Instant lockedUntil = count >= MAX_FAILED_ATTEMPTS
                    ? now.plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES)
                    : (base == null ? null : base.lockedUntil());
            return new AttemptInfo(count, lockedUntil, now);
        });
    }

    private record AttemptInfo(int failedCount, Instant lockedUntil, Instant lastFailedAt) {

        /** Het han khi khong con bi khoa VA lan sai cuoi da qua LOCKOUT_DURATION. */
        // Kiểm tra bản ghi đếm số lần sai đã hết hạn (không còn bị khóa và đã quá lâu không có lần sai mới) hay chưa.
        boolean isExpired(Instant now) {
            boolean lockOver = lockedUntil == null || !now.isBefore(lockedUntil);
            boolean stale = lastFailedAt.plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES).isBefore(now);
            return lockOver && stale;
        }
    }
}
