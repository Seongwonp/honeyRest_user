package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.email.EmailRequestDTO;
import com.honeyrest.honeyrest_user.dto.user.*;
import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.security.JwtTokenProvider;
import com.honeyrest.honeyrest_user.service.email.EmailVerificationTokenService;
import com.honeyrest.honeyrest_user.storage.FileStorage;
import com.honeyrest.honeyrest_user.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
    private final FileStorage fileStorage;

    /** refresh token 유효 기간(일). 쿠키 max-age 기본값(7일)과 맞춘다. */
    private static final int REFRESH_TOKEN_VALID_DAYS = 7;

    // 회원가입
    @Transactional
    public UserResponseDTO signup(UserSignupRequestDTO dto) throws Exception {
        if (userRepository.existsByEmail((dto.getEmail()))){
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        String imgUrl = "";
        if(dto.getProfileImage() != null){
            imgUrl = fileStorage.upload(dto.getProfileImage(),"profile");
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

        return toUserResponse(savedUser);
    }

    // 로그인 작업
    @Transactional
    public LoginResult login(UserLoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));


        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if ("DELETED".equals(user.getStatus())) {
            throw new IllegalStateException("탈퇴한 계정은 로그인할 수 없습니다.");
        }

        if (!user.getIsVerified()) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

        LoginResult result = issueTokens(user);

        user.updateLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return result;
    }

    // 소셜로그인
    @Transactional
    public LoginResult socialLogin(SocialLoginRequestDTO dto) {
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

        if ("DELETED".equals(user.getStatus())) {
            throw new IllegalStateException("탈퇴한 계정은 로그인할 수 없습니다.");
        }

        return issueTokens(user);
    }

    /**
     * access/refresh token 을 발급하고 기존 refresh token 을 모두 폐기한 뒤 새 토큰을 저장한다.
     * refresh token 쿠키는 컨트롤러가 {@link LoginResult#refreshToken()} 으로 만든다.
     */
    private LoginResult issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken();
        LocalDateTime expiry = LocalDateTime.now().plusDays(REFRESH_TOKEN_VALID_DAYS);

        refreshTokenService.invalidateAllByUser(user);
        refreshTokenService.saveRefreshToken(user, refreshToken, expiry);

        LoginResponseDTO response = LoginResponseDTO.builder()
                .user(toUserResponse(user))
                .accessToken(accessToken)
                .build();
        return new LoginResult(response, refreshToken);
    }

    private static UserResponseDTO toUserResponse(User user) {
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .profileImage(user.getProfileImage())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .build();
    }

    @Transactional(readOnly = true)
    public UserInfoDTO getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다."));
        return UserInfoDTO.from(user);
    }




    @Transactional(readOnly = true)
    public boolean verifyPassword(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다."));
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    @Transactional
    public void updateProfile(Long userId, UserProfileUpdateRequestDTO dto) {
        if (!dto.isPasswordVerified()) {
            throw new SecurityException("비밀번호 인증이 필요합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다."));

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
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다."));
        fileStorage.delete("profile", user.getProfileImage());

        String imgUrl = fileStorage.upload(file,"profile");
        user.updateProfileImage(imgUrl);
        userRepository.save(user);
        return imgUrl;
    }


    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("이전 비밀번호와 동일한 값은 사용할 수 없습니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        // 비밀번호 변경 전에 유출됐을 수 있는 access token 을 모두 무효화한다.
        user.revokeExistingTokens();
        userRepository.save(user);
        // refresh token 도 전부 삭제한다. 쿠키 삭제는 컨트롤러에서 한다.
        refreshTokenService.invalidateAllByUser(user);

        log.info("✅ 비밀번호 변경 완료: userId={}", userId);
    }

    /** 로그아웃 시 호출: 기존에 발급된 access token 을 모두 무효화한다. */
    @Transactional
    public void revokeTokens(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.revokeExistingTokens();
            userRepository.save(user);
        });
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));
        user.deleteAccount();
        userRepository.save(user);
    }
}
