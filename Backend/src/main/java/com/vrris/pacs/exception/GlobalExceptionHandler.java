package com.vrris.pacs.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tra loi theo dung schema "Error" trong doc/api/openapi.yaml cho moi endpoint.
 *
 * Giu ApiError/ApiException (khong doi sang jakarta ProblemDetail) - quyet dinh tam o
 * doc/15-kien-truc-backend.md muc 12: doi schema se pha vo doc/api/openapi.yaml dang co, de lai
 * nhu no ky thuat rieng, chua lam trong lan refactor nay.
 *
 * Vi co handler {@code Exception.class} o cuoi, MOI exception cua Spring MVC (JSON sai, sai method,
 * URL la, thieu tham so...) deu roi vao day truoc DefaultHandlerExceptionResolver - neu khong co
 * handler rieng, chung bi tra nham 500. Vi vay tung loai duoc map ro ve dung ma 4xx ben duoi.
 *
 * Log: 5xx -> error kem stack trace (truoc day nuot im lang, khong co dau vet de dieu tra);
 * 4xx do nguoi dung -> debug/warn, khong kem stack trace, khong lo chi tiet SQL ra response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("ApiException {} {}: {}", ex.getStatus().value(), ex.getCode(), ex.getMessage(), ex);
        } else {
            log.debug("ApiException {} {}: {}", ex.getStatus().value(), ex.getCode(), ex.getMessage());
        }
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
        log.debug("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of("FORBIDDEN", "Khong co quyen truy cap"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String issue = "Gia tri khong hop le cho tham so '" + ex.getName() + "'";
        return validationError(issue, List.of(new ApiErrorDetail(ex.getName(), issue)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return validationError("Request khong hop le", details);
    }

    /** @Validated tren tham so @PathVariable/@RequestParam (jakarta.validation). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        List<ApiErrorDetail> details = ex.getConstraintViolations().stream()
                .map(v -> new ApiErrorDetail(String.valueOf(v.getPropertyPath()), v.getMessage()))
                .toList();
        return validationError("Request khong hop le", details);
    }

    /** Body khong phai JSON hop le, sai kieu, hoac gia tri enum khong ton tai (vi du to_status = "FOO"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidFormatException ife) {
            String field = jsonPath(ife);
            String issue = ife.getTargetType() != null && ife.getTargetType().isEnum()
                    ? "Gia tri '" + ife.getValue() + "' khong hop le, chi nhan: " + enumValues(ife.getTargetType())
                    : "Gia tri '" + ife.getValue() + "' sai kieu du lieu";
            return validationError("Gia tri khong hop le cho truong '" + field + "'",
                    List.of(new ApiErrorDetail(field, issue)));
        }
        if (ex.getCause() instanceof JsonMappingException jme && !jme.getPath().isEmpty()) {
            String field = jsonPath(jme);
            return validationError("Gia tri khong hop le cho truong '" + field + "'",
                    List.of(new ApiErrorDetail(field, "Sai kieu du lieu hoac dinh dang")));
        }
        return validationError("Body khong phai JSON hop le hoac bi thieu", null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
        String issue = "Thieu tham so bat buoc '" + ex.getParameterName() + "'";
        return validationError(issue, List.of(new ApiErrorDetail(ex.getParameterName(), issue)));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiError.of("METHOD_NOT_ALLOWED", "Phuong thuc " + ex.getMethod() + " khong duoc ho tro"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiError.of("UNSUPPORTED_MEDIA_TYPE", "Content-Type khong duoc ho tro"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("NOT_FOUND", "Khong tim thay duong dan yeu cau"));
    }

    /**
     * Vi pham rang buoc DB (UNIQUE accession_number/study_instance_uid/patient_code, NOT NULL, FK...)
     * - thuong do hai request dong thoi vuot qua buoc kiem tra truoc o service. Khong dua message
     * SQL vao response (lo ten bang/cot/gia tri); chi ghi log server.
     */
    @ExceptionHandler({DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class})
    public ResponseEntity<ApiError> handleDataIntegrity(RuntimeException ex) {
        log.warn("Vi pham rang buoc du lieu: {}", rootMessage(ex));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("DATA_CONFLICT",
                        "Du lieu xung dot voi ban ghi da co (trung khoa duy nhat hoac vi pham rang buoc)"));
    }

    /** @Version (khoa lac quan) - ban ghi da bi nguoi khac sua giua luc doc va luc ghi. */
    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ApiError> handleOptimisticLock(RuntimeException ex) {
        log.info("Xung dot cap nhat dong thoi: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("CONCURRENT_MODIFICATION",
                        "Du lieu vua bi nguoi khac thay doi, vui long tai lai va thu lai"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Loi khong mong doi", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "Loi he thong khong mong doi"));
    }

    private static ResponseEntity<ApiError> validationError(String message, List<ApiErrorDetail> details) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.of("VALIDATION_ERROR", message, details));
    }

    private static String jsonPath(JsonMappingException ex) {
        return ex.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."));
    }

    private static String enumValues(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants()).map(String::valueOf).collect(Collectors.joining(", "));
    }

    private static String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
