package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.email.EmailRequestDTO;
import com.honeyrest.honeyrest_user.dto.user.*;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.security.JwtTokenProvider;
import com.honeyrest.honeyrest_user.service.email.EmailVerificationTokenService;
import com.honeyrest.honeyrest_user.util.FileUploadUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenService emailService;
    private final RefreshTokenService refreshTokenService;
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
    public LoginResponseDTO login(UserLoginRequestDTO dto, HttpServletResponse response) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!user.getIsVerified()) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken();
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);

        refreshTokenService.invalidateAllByUser(user);
        refreshTokenService.saveRefreshToken(user, refreshToken, expiry);

        // RefreshToken을 HttpOnly 쿠키로 설정
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

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

    // 소셜로그인
    public LoginResponseDTO socialLogin(SocialLoginRequestDTO dto, HttpServletResponse response) {
        User user = userRepository.findBySocialTypeAndSocialId(dto.getSocialType(), dto.getSocialId())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(dto.getEmail())
                            .socialType(dto.getSocialType())
                            .socialId(dto.getSocialId())
                            .name(dto.getName())
                            .profileImage(dto.getProfileImage())
                            .role("USER")
                            .status("ACTIVE")
                            .isVerified(true)
                            .lastLogin(LocalDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken();
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);

        refreshTokenService.invalidateAllByUser(user);
        refreshTokenService.saveRefreshToken(user, refreshToken, expiry);

        // RefreshToken을 HttpOnly 쿠키로 설정
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

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

    public UserInfoDTO getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return UserInfoDTO.from(user);
    }




    public boolean verifyPassword(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    @Transactional
    public void updateProfile(Long userId, UserProfileUpdateRequestDTO dto) {
        if (!dto.isPasswordVerified()) {
            throw new SecurityException("비밀번호 인증이 필요합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        boolean emailChanged = !user.getEmail().equals(dto.getEmail());

        if (emailChanged) {
            throw new IllegalArgumentException("이메일은 별도 인증 절차를 통해 변경해야 합니다.");
        }

        user.updateProfile(dto.getName(), dto.getPhone());

        userRepository.save(user);
        log.info("✅ 프로필 수정 완료 (이메일 변경은 별도 인증 절차 필요)");
    }


    @Transactional
    public String updateProfileImage(Long userId, MultipartFile file) throws Exception {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        fileUploadUtil.delete("profile", user.getProfileImage());

        String imgUrl = fileUploadUtil.upload(file,"profile");
        user.updateProfileImage(imgUrl);
        userRepository.save(user);
        return imgUrl;
    }


    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("이전 비밀번호와 동일한 값은 사용할 수 없습니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("✅ 비밀번호 변경 완료: userId={}", userId);
    }


    @Transactional
    public void usePoint(Long userId, Integer usedPoint) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다"));

        int current = user.getPoint();
        if (current < usedPoint) {
            throw new IllegalArgumentException("사용 가능한 포인트가 부족합니다");
        }

        user.updatePoint(current - usedPoint);
        userRepository.save(user);

        log.info("🪙 포인트 차감 완료: userId={}, usedPoint={}, remainingPoint={}",
                userId, usedPoint, user.getPoint());
    }

    @Transactional
    public void addPoint(Long userId, Integer amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        int updated = user.getPoint() + amount;
        user.updatePoint(updated);
        userRepository.save(user);

        log.info("🪙 포인트 적립 완료: userId={}, amount={}, balance={}", userId, amount, updated);
    }

}