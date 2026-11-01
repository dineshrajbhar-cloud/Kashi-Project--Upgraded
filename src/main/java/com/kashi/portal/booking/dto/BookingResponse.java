package com.kashi.portal.booking.dto;

import com.kashi.portal.booking.entity.Booking;
import com.kashi.portal.booking.entity.BookingStatus;
import com.kashi.portal.booking.entity.PaymentStatus;
import com.kashi.portal.place.BookingType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(

        Long id,

        Long placeId,

        String placeName,

        LocalDate bookingDate,

        Integer visitors,

        BookingType bookingType,

        BigDecimal totalAmount,

        BookingStatus bookingStatus,

        PaymentStatus paymentStatus,

        String specialRequest,

        LocalDateTime createdAt

) {

    public static BookingResponse from(Booking booking) {

        return new BookingResponse(
                booking.getId(),
                booking.getPlace().getId(),
                booking.getPlace().getName(),
                booking.getBookingDate(),
                booking.getVisitors(),
                booking.getBookingType(),
                booking.getTotalAmount(),
                booking.getBookingStatus(),
                booking.getPaymentStatus(),
                booking.getSpecialRequest(),
                booking.getCreatedAt()
        );
    }
}