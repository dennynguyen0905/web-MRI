package com.vrris.pacs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Tra loi theo dung schema "Error" trong doc/api/openapi.yaml cho moi endpoint.
 *
 * Giu ApiError/ApiException (khong doi sang jakarta ProblemDetail) - quyet dinh tam o
 * doc/15-kien-truc-backend.md muc 12: doi schema se pha vo doc/api/openapi.yaml dang co, de lai
 * nhu no ky thuat rieng, chua lam trong lan refactor nay.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiError.of(ex.getCode(), ex.getMessage()));
    }

    /**
     * @PreAuthorize (rest/*.java) la AOP tren method - khi bi chan, no nem
     * org.springframework.security.authorization.AuthorizationDeniedException (ke thua
     * AccessDeniedException) TRONG LUC DispatcherServlet goi controller, nen @RestControllerAdvice
     * nay bat duoc truoc khi no co co hoi lan toi ExceptionTranslationFilter. Vi vay
     * security.RestAccessDeniedHandler (dang ky qua HttpSecurity.exceptionHandling) THUC TE
     * khong bao gio chay voi @PreAuthorize - no chi con y nghia phong khi co luat chan o tang
     * filter (authorizeHttpRequests) sau nay. Giu ca hai, tra cung mot dang ApiError.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of("FORBIDDEN", "Khong co quyen truy cap"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String issue = "Gia tri khong hop le cho tham so '" + ex.getName() + "'";
        ApiError error = ApiError.of(
                "VALIDATION_ERROR",
                issue,
                List.of(new ApiErrorDetail(ex.getName(), issue))
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ApiError error = ApiError.of("VALIDATION_ERROR", "Request khong hop le", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "Loi he thong khong mong doi"));
    }
}
