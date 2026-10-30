package com.kashi.portal.donation.service;

import com.kashi.portal.donation.dto.DonationOrderResponse;
import com.kashi.portal.donation.dto.DonationRequest;
import com.kashi.portal.donation.dto.PaymentVerificationRequest;
import com.kashi.portal.donation.dto.DonationHistoryResponse;

import java.util.List;

public interface DonationService {

    DonationOrderResponse createDonationOrder(DonationRequest request);

    void verifyPayment(PaymentVerificationRequest request);

    List<DonationHistoryResponse> getAllDonations();
}