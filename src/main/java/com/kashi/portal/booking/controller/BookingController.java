package com.kashi.portal.booking.controller;

import com.kashi.portal.booking.dto.BookingCreationResponse;
import com.kashi.portal.booking.dto.BookingPaymentVerificationRequest;
import com.kashi.portal.booking.dto.BookingRequest;
import com.kashi.portal.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.kashi.portal.booking.dto.BookingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // Create a new booking
    @PostMapping
    public ResponseEntity<BookingCreationResponse> createBooking(
            @Valid @RequestBody BookingRequest request) {

        BookingCreationResponse response =
                bookingService.createBooking(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Verify Razorpay payment
    @PostMapping("/verify-payment")
    public ResponseEntity<String> verifyPayment(
            @Valid @RequestBody BookingPaymentVerificationRequest request) {

        bookingService.verifyPayment(request);

        return ResponseEntity.ok(
                "Booking payment verified successfully"
        );
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
            @PageableDefault(size = 10, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(bookingService.getMyBookings(pageable));
    }
}
