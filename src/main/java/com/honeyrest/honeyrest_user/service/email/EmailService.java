package com.honeyrest.honeyrest_user.service.email;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Log4j2
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendReservationConfirmation(String email, ReservationCompleteDTO dto) {
        try {
            String html = """
                        <html>
                          <body style="font-family: 'Arial', sans-serif; line-height: 1.6; background-color: #f9f9f9; padding: 20px;">
                            <div style="max-width: 600px; margin: auto; background-color: #fff; border-radius: 8px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);">
                              <h2 style="color: #ff9900;">HoneyRest 예약 완료 안내 🎉</h2>
                    
                              <p><strong>{guestName}</strong> 님, 예약이 정상적으로 완료되었습니다.</p>
                    
                              <hr style="margin: 20px 0;" />
                    
                              <h3 style="margin-bottom: 8px;">📍 숙소 정보</h3>
                              <p>
                                <strong>숙소명:</strong> {accommodationName}<br/>
                                <strong>객실명:</strong> {roomName}<br/>
                                <strong>예약번호:</strong> {reservationCode}
                              </p>
                    
                              <h3 style="margin-bottom: 8px;">📅 예약 일정</h3>
                              <p>
                                <strong>체크인:</strong> {checkIn}<br/>
                                <strong>체크아웃:</strong> {checkOut}<br/>
                                <strong>인원:</strong> {guests}명
                              </p>
                    
                              <h3 style="margin-bottom: 8px;">💳 결제 정보</h3>
                              <p>
                                <strong>결제 수단:</strong> {paymentMethod}<br/>
                                <strong>결제 상태:</strong> {paymentStatus}<br/>
                                <strong>원가:</strong> {originalPrice}원<br/>
                                <strong>할인:</strong> -{discountAmount}원<br/>
                                <strong>쿠폰:</strong> {couponName}<br/>
                                <strong>최종 결제 금액:</strong> <span style="color: #0077cc; font-weight: bold;">{finalPrice}원</span>
                              </p>
                                <p style="font-size: 14px; color: #555; margin-top: 16px;">
                                              예약 상세 정보는 <strong>마이페이지 &gt; 내 예약</strong>에서 확인하실 수 있습니다.
                                            </p>
                              <hr style="margin: 20px 0;" />
                    
                              <p style="font-size: 14px; color: #555;">
                                예약 관련 문의는 언제든지 고객센터로 연락 주세요.<br/>
                                HoneyRest와 함께 편안한 여행 되시길 바랍니다 ☀️
                              </p>
                    
                              <p style="margin-top: 24px; font-size: 12px; color: #999;">
                                본 메일은 발신 전용입니다. <br/>
                                ⓒ HoneyRest. All rights reserved.
                              </p>
                            </div>
                          </body>
                        </html>
                    """.replace("{guestName}", dto.getGuestName())
                    .replace("{accommodationName}", dto.getAccommodationName())
                    .replace("{roomName}", dto.getRoomName())
                    .replace("{reservationCode}", dto.getReservationCode())
                    .replace("{checkIn}", dto.getCheckIn().toString())
                    .replace("{checkOut}", dto.getCheckOut().toString())
                    .replace("{guests}", String.valueOf(dto.getGuests()))
                    .replace("{paymentMethod}", dto.getPaymentMethod())
                    .replace("{paymentStatus}", dto.getPaymentStatus())
                    .replace("{originalPrice}", dto.getOriginalPrice().toPlainString())
                    .replace("{discountAmount}", dto.getDiscountAmount().toPlainString())
                    .replace("{couponName}", dto.getCouponName() != null ? dto.getCouponName() : "미사용")
                    .replace("{finalPrice}", dto.getFinalPrice().toPlainString());

            sendHtmlMail(email, "HoneyRest 예약 완료 안내", html);

            log.info("📧 예약 완료 이메일 발송 성공: {}", email);
        } catch (Exception e) {
            log.error("📧 예약 완료 이메일 발송 실패", e);
        }
    }

    @Async
    public void sendPasswordReset(String email, String tokenValue) {
        try {
            String resetLink = "http://localhost:5173/reset-password/change?token=" + tokenValue;

            String html = """
            <html>
              <body style="font-family: 'Arial', sans-serif; line-height: 1.6; background-color: #f9f9f9; padding: 20px;">
                <div style="max-width: 600px; margin: auto; background-color: #fff; border-radius: 8px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);">
                  <h2 style="color: #ff9900;">HoneyRest 비밀번호 재설정 안내 🔐</h2>

                  <p>비밀번호를 재설정하시려면 아래 버튼을 클릭해주세요.</p>

                  <div style="margin: 20px 0;">
                    <a href="%s" style="display: inline-block; background-color: #ff9900; color: #fff; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: bold;">
                      비밀번호 재설정하기
                    </a>
                  </div>

                  <p style="font-size: 14px; color: #555;">
                    위 링크는 30분 후 만료됩니다.<br/>
                    본인이 요청하지 않았다면 이 이메일을 무시해주세요.
                  </p>

                  <hr style="margin: 20px 0;" />

                  <p style="margin-top: 24px; font-size: 12px; color: #999;">
                    본 메일은 발신 전용입니다. <br/>
                    ⓒ HoneyRest. All rights reserved.
                  </p>
                </div>
              </body>
            </html>
        """.formatted(resetLink);

            sendHtmlMail(email, "HoneyRest 비밀번호 재설정 안내", html);
        } catch (Exception e) {
            log.error("📧 비밀번호 재설정 이메일 발송 실패", e);
        }
    }

    @Async
    public void sendVerificationEmail(String email, String link) {
        try {
            String htmlContent = """
                    <html>
                      <body style="font-family: 'Pretendard', sans-serif; background-color: #f9f9f9; padding: 40px;">
                        <div style="max-width: 600px; margin: auto; background-color: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); padding: 32px;">
                          <h2 style="color: #ffb300; margin-bottom: 16px;">HoneyRest 이메일 인증</h2>
                          <p style="font-size: 16px; color: #333;">안녕하세요, <strong>HoneyRest</strong>를 이용해 주셔서 감사합니다.<br>아래 버튼을 클릭하여 이메일 인증을 완료해 주세요.</p>
                          <div style="margin: 24px 0;">
                            <a href="%s" style="display: inline-block; background-color: #ffb300; color: #fff; padding: 14px 28px; border-radius: 8px; text-decoration: none; font-weight: bold;">
                              이메일 인증하기
                            </a>
                          </div>
                          <p style="font-size: 14px; color: #999;">이 링크는 24시간 동안 유효합니다.<br>문의사항은 support@honeyrest.com 으로 연락 주세요.</p>
                        </div>
                      </body>
                    </html>
            """.formatted(link);

            sendHtmlMail(email, "HoneyRest 이메일 인증", htmlContent);
        } catch (Exception e) {
            log.error("이메일 인증 메일 전송 실패", e);
        }
    }

    @Async
    public void sendEmailChangeToken(String email, String link) {
        try {
            String htmlContent = """
        <html>
          <body style="font-family: 'Pretendard', sans-serif; background-color: #f9f9f9; padding: 40px;">
            <div style="max-width: 600px; margin: auto; background-color: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); padding: 32px;">
              <h2 style="color: #ffb300; margin-bottom: 16px;">HoneyRest 이메일 변경 인증</h2>
              <p style="font-size: 16px; color: #333;">안녕하세요, <strong>HoneyRest</strong>를 이용해 주셔서 감사합니다.<br>아래 버튼을 클릭하여 이메일 변경 인증을 완료해 주세요.</p>
              <div style="margin: 24px 0;">
                <a href="%s" style="display: inline-block; background-color: #ffb300; color: #fff; padding: 14px 28px; border-radius: 8px; text-decoration: none; font-weight: bold;">
                  이메일 인증하기
                </a>
              </div>
              <p style="font-size: 14px; color: #999;">이 링크는 24시간 동안 유효합니다.<br>문의사항은 support@honeyrest.com 으로 연락 주세요.</p>
            </div>
          </body>
        </html>
        """.formatted(link);

            sendHtmlMail(email, "HoneyRest 이메일 변경 인증", htmlContent);
        } catch (Exception e) {
            log.error("이메일 변경 인증 메일 전송 실패", e);
        }
    }

    private void sendHtmlMail(String to, String subject, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
        log.info("📧 이메일 발송 성공: {} / {}", to, subject);
    }



}
