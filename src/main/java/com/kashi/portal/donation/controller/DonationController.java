package com.kashi.portal.donation.controller;

import com.kashi.portal.donation.dto.DonationOrderResponse;
import com.kashi.portal.donation.dto.DonationRequest;
import com.kashi.portal.donation.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.kashi.portal.donation.dto.PaymentVerificationRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    @PostMapping("/create-order")
    public ResponseEntity<DonationOrderResponse> createDonationOrder(
            @Valid @RequestBody DonationRequest request) {

        DonationOrderResponse response =
                donationService.createDonationOrder(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<String> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) {

        donationService.verifyPayment(request);

        return ResponseEntity.ok("Donation payment verified successfully");
    }
}