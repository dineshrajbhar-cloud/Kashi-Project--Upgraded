package com.kashi.portal.booking.repository;

import com.kashi.portal.booking.entity.Booking;
import com.kashi.portal.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find all bookings of a particular user
    Page<Booking> findByUserId(Long userId, Pageable pageable);

    // Find booking by ID and user ID
    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    // Find bookings by status
    Page<Booking> findByBookingStatus(
            BookingStatus bookingStatus,
            Pageable pageable
    );

    // Find all bookings for a particular destination
    List<Booking> findByPlaceId(Long placeId);

    // Count bookings by status
    long countByBookingStatus(BookingStatus bookingStatus);
}