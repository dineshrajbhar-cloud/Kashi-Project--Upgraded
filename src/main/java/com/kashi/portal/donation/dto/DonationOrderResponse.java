package com.kashi.portal.donation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DonationOrderResponse {

    private Long donationId;
    private String razorpayOrderId;
    private String razorpayKeyId;
    private String currency;
    private Integer amount;
}