package com.honeyrest.honeyrest_user.dto.user;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSignupRequestDTO {


    private String email;
    private String password;
    private String name;



    private String phone;
    private String birthDate; // yyyy-MM-dd 형식
    private String gender;
    private Boolean marketingAgree;
    private MultipartFile profileImage;

}