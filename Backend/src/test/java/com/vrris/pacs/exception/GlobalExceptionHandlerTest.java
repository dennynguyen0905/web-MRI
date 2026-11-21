package com.vrris.pacs.exception;

import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit test (khong can Docker) cho GlobalExceptionHandler: moi loai exception phai ra dung ma HTTP +
 * ApiError.code, khong roi vao handler Exception.class (500) nhu truoc. Controller gia chi dung
 * trong test nay de nem tung loai exception.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    enum Color { RED, GREEN }

    record Body(Color color, Integer size) {
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/data-conflict")
        void dataConflict() {
            throw new DataIntegrityViolationException(
                    "Duplicate entry '2026103100001' for key 'study.uk_study_accession_number'");
        }

        @GetMapping("/optimistic-spring")
        void optimisticSpring() {
            throw new ObjectOptimisticLockingFailureException(Object.class, 1L);
        }

        @GetMapping("/optimistic-jpa")
        void optimisticJpa() {
            throw new OptimisticLockException("stale");
        }

        @GetMapping("/api-500")
        void api500() {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "payload hong");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("bug noi bo co chi tiet nhay cam");
        }

        @GetMapping("/required-param")
        String requiredParam(@RequestParam("study_id") Long studyId) {
            return "ok";
        }

        @PostMapping(path = "/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        String body(@RequestBody Body body) {
            return "ok";
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void vi_pham_rang_buoc_db_tra_409_DATA_CONFLICT_khong_lo_sql() throws Exception {
        mockMvc.perform(get("/data-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_CONFLICT"))
                .andExpect(content().string(not(containsString("uk_study_accession_number"))));
    }

    @Test
    void khoa_lac_quan_spring_va_jpa_deu_tra_409_CONCURRENT_MODIFICATION() throws Exception {
        mockMvc.perform(get("/optimistic-spring"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
        mockMvc.perform(get("/optimistic-jpa"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    void thieu_tham_so_bat_buoc_tra_400() throws Exception {
        mockMvc.perform(get("/required-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("study_id"));
    }

    @Test
    void json_hong_tra_400() throws Exception {
        mockMvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{\"color\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void enum_sai_tra_400_neu_ten_truong_va_gia_tri_hop_le() throws Exception {
        mockMvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{\"color\":\"BLUE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("color"))
                .andExpect(jsonPath("$.details[0].issue").value(containsString("RED, GREEN")));
    }

    @Test
    void sai_kieu_so_trong_body_tra_400() throws Exception {
        mockMvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{\"size\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("size"));
    }

    @Test
    void sai_content_type_tra_415() throws Exception {
        mockMvc.perform(post("/body").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void sai_method_tra_405() throws Exception {
        mockMvc.perform(delete("/body"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void api_exception_5xx_giu_nguyen_ma() throws Exception {
        mockMvc.perform(get("/api-500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void loi_khong_mong_doi_tra_500_khong_lo_message_noi_bo() throws Exception {
        mockMvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("nhay cam"))));
    }
}
