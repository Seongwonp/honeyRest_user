package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.email.EmailRequestDTO;
import com.honeyrest.honeyrest_user.dto.user.*;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.security.JwtTokenProvider;
import com.honeyrest.honeyrest_user.service.EmailVerificationTokenService;
import com.honeyrest.honeyrest_user.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenService emailService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final FileUploadUtil fileUploadUtil;

    // 회원가입
    public UserResponseDTO signup(UserSignupRequestDTO dto) throws Exception {
        if (userRepository.existsByEmail((dto.getEmail()))){
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        String imgUrl = "";
        if(dto.getProfileImage() != null){
            imgUrl = fileUploadUtil.upload(dto.getProfileImage(),"profile");
        }
        User user = User.builder()
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .phone(dto.getPhone())
                .birthDate(dto.getBirthDate() != null ? LocalDate.parse(dto.getBirthDate()) : null)
                .gender(dto.getGender())
                .marketingAgree(Boolean.TRUE.equals(dto.getMarketingAgree()))
                .profileImage(imgUrl)
                .role("USER")
                .status("ACTIVE")
                .isVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        // 이메일 인증 메일 전송
        emailService.sendVerificationEmail(new EmailRequestDTO(savedUser.getEmail()));

        return UserResponseDTO.builder()
                .userId(savedUser.getUserId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .phone(savedUser.getPhone())
                .profileImage(savedUser.getProfileImage())
                .role(savedUser.getRole())
                .isVerified(savedUser.getIsVerified())
                .build();
    }

    // 로그인
    public LoginResponseDTO login(UserLoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        if (!user.getIsVerified()) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

        // JWT 토큰 추가
        String accessToken = jwtTokenProvider.createToken(user.getUserId(), user.getRole());
        user.updateLastLogin(LocalDateTime.now());
        userRepository.save(user);
        return LoginResponseDTO.builder()
                .user(UserResponseDTO.builder()
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .phone(user.getPhone())
                        .profileImage(user.getProfileImage())
                        .role(user.getRole())
                        .isVerified(user.getIsVerified())
                        .build())
                .accessToken(accessToken)
                .build();
    }

    // 소셜 로그인
    public LoginResponseDTO socialLogin(SocialLoginRequestDTO dto) {
        User user = userRepository.findBySocialTypeAndSocialId(dto.getSocialType(), dto.getSocialId())
                .orElseGet(() -> {
                    // 최초 로그인 → 자동 회원가입
                    User newUser = User.builder()
                            .email(dto.getEmail())
                            .socialType(dto.getSocialType())
                            .socialId(dto.getSocialId())
                            .name(dto.getName())
                            .profileImage(dto.getProfileImage())
                            .role("USER")
                            .status("ACTIVE")
                            .isVerified(true) // 소셜은 기본 인증 처리
                            .lastLogin(LocalDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });

        String accessToken = jwtTokenProvider.createToken(user.getUserId(), user.getRole());

        return LoginResponseDTO.builder()
                .user(UserResponseDTO.builder()
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .phone(user.getPhone())
                        .profileImage(user.getProfileImage())
                        .role(user.getRole())
                        .isVerified(user.getIsVerified())
                        .build())
                .accessToken(accessToken)
                .build();
    }
}