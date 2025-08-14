package com.honeyrest.honeyrest_user.controller.auth;

import com.honeyrest.honeyrest_user.dto.user.*;
import com.honeyrest.honeyrest_user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public UserResponseDTO signup(@ModelAttribute UserSignupRequestDTO request) throws Exception {
        return userService.signup(request);
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody UserLoginRequestDTO request) {
        return userService.login(request);
    }

    @PostMapping("/social-login")
    public LoginResponseDTO socialLogin(@RequestBody SocialLoginRequestDTO request) {
        return userService.socialLogin(request);
    }
}