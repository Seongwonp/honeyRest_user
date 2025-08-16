package com.honeyrest.honeyrest_user.security;

import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 이메일 추출 (구글 기준)
        String email = oAuth2User.getAttribute("email");

        // DB에 사용자 존재 여부 확인
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // 없으면 새로 등록
                    User newUser = User.builder()
                            .email(email)
                            .role("USER") // 기본 권한
                            .build();
                    return userRepository.save(newUser);
                });

        // 인증 객체로 사용할 커스텀 프린시펄 생성
        return new CustomUserPrincipal(user, oAuth2User.getAttributes());
    }
}