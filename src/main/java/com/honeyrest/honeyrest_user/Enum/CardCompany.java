package com.honeyrest.honeyrest_user.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CardCompany {
    SAMSUNG(51, "삼성카드"),
    SHINHAN(41, "신한카드"),
    HYUNDAI(61, "현대카드"),
    LOTTE(71, "롯데카드"),
    KOOKMIN(11, "KB국민카드"),
    NONGHYEOP(91, "NH농협카드"),
    HANA(21, "하나카드"),
    WOORI(33, "우리BC카드"),
    CITI(36, "씨티카드"),
    BC(31, "BC카드"),
    IBK_BC(3, "기업BC카드"),
    GWANGJU(46, "광주은행"),
    KDB(30, "KDB산업은행"),
    SAEMAUL(38, "새마을금고"),
    SHINHYEOP(62, "신협"),
    POST(37, "우체국예금보험"),
    JEONBUK(35, "전북은행"),
    JEJU(42, "제주은행"),
    KAKAOBANK(15, "카카오뱅크"),
    KBANK(3, "케이뱅크"),
    TOSSBANK(24, "토스뱅크");

    private final int code;
    private final String displayName;

    public static String getDisplayNameByCode(int code) {
        for (CardCompany company : values()) {
            if (company.code == code) {
                return company.displayName;
            }
        }
        return "알 수 없음";
    }
}