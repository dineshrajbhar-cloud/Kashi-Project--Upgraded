package com.kashi.portal.donation.controller;

import com.kashi.portal.donation.dto.DonationHistoryResponse;
import com.kashi.portal.donation.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/donations")
@RequiredArgsConstructor
public class DonationHistoryController {

    private final DonationService donationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DonationHistoryResponse>> getAllDonations() {

        return ResponseEntity.ok(
                donationService.getAllDonations()
        );
    }
}