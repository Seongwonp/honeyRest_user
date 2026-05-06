package com.honeyrest.honeyrest_user.dto.user;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSignupRequestDTO {

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    @Size(max = 50)
    private String name;

    @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 전화번호 형식이 아닙니다")
    private String phone;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 yyyy-MM-dd 형식이어야 합니다")
    private String birthDate;

    private String gender;
    private Boolean marketingAgree;
    private MultipartFile profileImage;

}