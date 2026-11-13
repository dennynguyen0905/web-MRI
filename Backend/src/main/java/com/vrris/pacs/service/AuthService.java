package com.vrris.pacs.service;

import com.vrris.pacs.dto.request.LoginRequest;
import com.vrris.pacs.dto.response.CurrentUserResponse;
import com.vrris.pacs.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    CurrentUserResponse getCurrentUser();

    void logout();
}
