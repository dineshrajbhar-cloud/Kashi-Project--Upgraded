package com.kashi.portal.donation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentVerificationRequest {

    @NotBlank
    @JsonProperty("razorpay_order_id")
    private String razorpayOrderId;

    @NotBlank
    @JsonProperty("razorpay_payment_id")
    private String razorpayPaymentId;

    @NotBlank
    @JsonProperty("razorpay_signature")
    private String razorpaySignature;
}