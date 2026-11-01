package com.kashi.portal.booking.service;

import com.kashi.portal.booking.dto.BookingCreationResponse;
import com.kashi.portal.booking.dto.BookingRequest;
import com.kashi.portal.booking.dto.BookingPaymentVerificationRequest;
import com.kashi.portal.booking.dto.BookingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {

    BookingCreationResponse createBooking(BookingRequest request);

    void verifyPayment(BookingPaymentVerificationRequest request);

    Page<BookingResponse> getMyBookings(Pageable pageable);

}