package com.kashi.portal.donation.dto;

import com.kashi.portal.donation.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class DonationHistoryResponse {

    private Long donationId;

    private String donorName;

    private String donorEmail;

    private String donorPhone;

    private BigDecimal amount;

    private String currency;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private PaymentStatus paymentStatus;

    private LocalDateTime createdAt;
}