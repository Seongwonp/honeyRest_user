package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.email.EmailRequestDTO;
import com.honeyrest.honeyrest_user.dto.user.*;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.security.JwtTokenProvider;
import com.honeyrest.honeyrest_user.service.email.EmailVerificationTokenService;
import com.honeyrest.honeyrest_user.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Log4j2
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
        // 생년월일 검증 (만 14세 미만 가입 불가)
        if (dto.getBirthDate() != null) {
            LocalDate birthDate = LocalDate.parse(dto.getBirthDate());
            LocalDate today = LocalDate.now();
            int age = today.getYear() - birthDate.getYear();
            if (birthDate.plusYears(age).isAfter(today)) {
                age--; // 아직 생일 안 지났으면 나이 -1
            }
            if (age < 14) {
                throw new IllegalArgumentException("만 14세 미만은 회원가입이 불가능합니다.");
            }
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

    // 로그인 작업
    public LoginResponseDTO login(UserLoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!user.getIsVerified()) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

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

    public boolean verifyPassword(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    @Transactional
    public void updateProfile(Long userId, UserProfileUpdateRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        boolean emailChanged = !user.getEmail().equals(dto.getEmail());

        user.updateProfile(dto.getName(), dto.getPhone());

        if (emailChanged) {
            emailService.sendEmailChangeToken(user, dto.getEmail());
            log.info("📧 이메일 변경 요청 처리: {} → {}", user.getEmail(), dto.getEmail());
        }

        userRepository.save(user);
        log.info("✅ 프로필 수정 완료{}", emailChanged ? " (이메일 변경은 인증 후 반영)" : "");
    }

}