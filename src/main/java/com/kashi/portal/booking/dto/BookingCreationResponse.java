package com.kashi.portal.booking.dto;

import com.kashi.portal.booking.entity.BookingStatus;
import com.kashi.portal.booking.entity.PaymentStatus;
import com.kashi.portal.place.BookingType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingCreationResponse(
        Long bookingId,
        String placeName,
        LocalDate bookingDate,
        Integer visitors,
        BookingType bookingType,
        BigDecimal totalAmount,
        BookingStatus bookingStatus,
        PaymentStatus paymentStatus,

        // Razorpay details (PAID booking only)
        String razorpayOrderId,
        String razorpayKeyId,
        String currency,
        Integer amountInPaise
) {
}