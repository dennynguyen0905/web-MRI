package com.vrris.pacs.rest;

import com.vrris.pacs.dto.request.LoginRequest;
import com.vrris.pacs.dto.response.CurrentUserResponse;
import com.vrris.pacs.dto.response.LoginResponse;
import com.vrris.pacs.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "auth", description = "Đăng nhập, đăng xuất, người dùng hiện tại (IN-13)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Đăng xuất")
    public void logout() {
        authService.logout();
    }

    @GetMapping("/me")
    @Operation(summary = "Người dùng và vai trò hiện tại")
    public CurrentUserResponse getCurrentUser() {
        return authService.getCurrentUser();
    }
}

