package com.kashi.portal.donation.repository;

import com.kashi.portal.donation.entity.Donation;
import com.kashi.portal.donation.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    Optional<Donation> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Donation> findByRazorpayPaymentId(String razorpayPaymentId);

    List<Donation> findByDonorIdOrderByCreatedAtDesc(Long donorId);

    List<Donation> findByPaymentStatus(PaymentStatus paymentStatus);

    @EntityGraph(attributePaths = {"donor"})
    List<Donation> findAllByOrderByCreatedAtDesc();
}