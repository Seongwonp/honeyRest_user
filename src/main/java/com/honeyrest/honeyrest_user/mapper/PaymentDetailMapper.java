package com.honeyrest.honeyrest_user.mapper;

import com.honeyrest.honeyrest_user.Enum.CardCompany;
import com.honeyrest.honeyrest_user.dto.payment.PaymentDetailDTO;
import com.honeyrest.honeyrest_user.entity.PaymentDetail;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class PaymentDetailMapper {
    public PaymentDetailDTO toDTO(PaymentDetail detail) {
        String cardCompanyName = "알 수 없음";
        try {
            int code = Integer.parseInt(detail.getCardCompany());
            cardCompanyName = CardCompany.getDisplayNameByCode(code);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return PaymentDetailDTO.builder()
                .cardCompanyName(cardCompanyName)
                .maskedCardNumber(detail.getCardNumber())
                .installmentMonths(detail.getInstallmentMonths())
                .virtualAccountBank(detail.getVirtualAccountBank())
                .virtualAccountNumber(detail.getVirtualAccountNumber())
                .virtualAccountHolder(detail.getVirtualAccountHolder())
                .virtualAccountExpire(detail.getVirtualAccountExpire())
                .build();
    }
}
