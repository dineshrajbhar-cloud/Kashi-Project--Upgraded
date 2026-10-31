package com.kashi.portal.donation.service;

import com.kashi.portal.donation.dto.DonationOrderResponse;
import com.kashi.portal.donation.dto.DonationRequest;
import com.kashi.portal.donation.entity.Donation;
import com.kashi.portal.donation.entity.Donor;
import com.kashi.portal.donation.entity.PaymentStatus;
import com.kashi.portal.donation.repository.DonationRepository;
import com.kashi.portal.donation.repository.DonorRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.kashi.portal.donation.dto.PaymentVerificationRequest;
import com.razorpay.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.kashi.portal.donation.dto.DonationHistoryResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import com.kashi.portal.notification.entity.NotificationType;
import com.kashi.portal.notification.service.NotificationService;
import com.kashi.portal.user.Role;
import com.kashi.portal.user.User;
import com.kashi.portal.user.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationServiceImpl implements DonationService {

    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final RazorpayClient razorpayClient;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Override
    @Transactional
    public DonationOrderResponse createDonationOrder(DonationRequest request) {

        // 1. Find existing donor or create a new donor
        Donor donor = donorRepository.findByEmail(request.getDonorEmail())
                .orElseGet(() -> {
                    Donor newDonor = Donor.builder()
                            .name(request.getDonorName())
                            .email(request.getDonorEmail())
                            .phone(request.getDonorPhone())
                            .build();

                    return donorRepository.save(newDonor);
                });

        // 2. Convert rupees into paise
        BigDecimal amount = request.getAmount()
                .setScale(2, RoundingMode.UNNECESSARY);

        int amountInPaise = amount
                .multiply(BigDecimal.valueOf(100))
                .intValueExact();

        // 3. Create Razorpay order
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "donation_" + System.currentTimeMillis());

        try {
            Order order = razorpayClient.orders.create(orderRequest);

            String orderId = order.get("id");

            // 4. Save donation in database
            Donation donation = Donation.builder()
                    .donor(donor)
                    .amount(amount)
                    .currency("INR")
                    .razorpayOrderId(orderId)
                    .paymentStatus(PaymentStatus.CREATED)
                    .build();

            Donation savedDonation = donationRepository.save(donation);

            // 5. Return order details to frontend
            return DonationOrderResponse.builder()
                    .donationId(savedDonation.getId())
                    .razorpayOrderId(orderId)
                    .razorpayKeyId(razorpayKeyId)
                    .currency("INR")
                    .amount(amountInPaise)
                    .build();

        } catch (RazorpayException exception) {
            throw new IllegalStateException(
                    "Unable to create Razorpay order", exception
            );
        }
    }

    @Override
    @Transactional
    public void verifyPayment(PaymentVerificationRequest request) {

        // Find donation using Razorpay Order ID
        Donation donation = donationRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Donation order not found")
                );

        // Prevent duplicate payment verification
        if (donation.getPaymentStatus() == PaymentStatus.SUCCESS) {
            if (request.getRazorpayPaymentId()
                    .equals(donation.getRazorpayPaymentId())) {
                return;
            }

            throw new IllegalStateException("Donation already completed");
        }

        try {
            // Generate signature payload
            String payload = request.getRazorpayOrderId()
                    + "|"
                    + request.getRazorpayPaymentId();

            // Verify Razorpay signature
            boolean isValid = Utils.verifySignature(
                    payload,
                    request.getRazorpaySignature(),
                    razorpayKeySecret
            );

            if (!isValid) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid Razorpay payment signature"
                );
            }

            // Update donation record
            donation.setRazorpayPaymentId(request.getRazorpayPaymentId());
            donation.setPaymentStatus(PaymentStatus.SUCCESS);

            Donation savedDonation = donationRepository.save(donation);

            // Notify all Admin users
            List<User> admins = userRepository.findAllByRole(Role.ADMIN);

            for (User admin : admins) {

                notificationService.createNotification(
                        admin.getId(),
                        "New Donation Received",
                        donation.getDonor().getName()
                                + " donated ₹" + donation.getAmount(),
                        NotificationType.DONATION,
                        "DONATION",
                        savedDonation.getId()
                );
            }


            // Notify registered Donor
            userRepository.findByEmail(donation.getDonor().getEmail())
                    .ifPresent(user -> notificationService.createNotification(
                            user.getId(),
                            "Donation Successful",
                            "Your donation of ₹" + donation.getAmount()
                                    + " has been received successfully. Thank you!",
                            NotificationType.DONATION,
                            "DONATION",
                            savedDonation.getId()
                    ));

        } catch (RazorpayException e) {
            throw new IllegalStateException(
                    "Payment verification failed", e
            );
        }
    }

//    @Override
//    @Transactional
//    public void verifyPayment(PaymentVerificationRequest request) {
//
//
//        // Find donation using Razorpay Order ID
//        Donation donation = donationRepository
//                .findByRazorpayOrderId(request.getRazorpayOrderId())
//                .orElseThrow(() ->
//                        new IllegalArgumentException("Donation order not found")
//                );
//
//        // Prevent duplicate payment verification
//        if (donation.getPaymentStatus() == PaymentStatus.SUCCESS) {
//            if (request.getRazorpayPaymentId()
//                    .equals(donation.getRazorpayPaymentId())) {
//                return;
//            }
//
//            throw new IllegalStateException("Donation already completed");
//        }
//
//        try {
//            // Generate signature payload
//            String payload = request.getRazorpayOrderId()
//                    + "|"
//                    + request.getRazorpayPaymentId();
//
//            // Verify Razorpay signature
//            boolean isValid = Utils.verifySignature(
//                    payload,
//                    request.getRazorpaySignature(),
//                    razorpayKeySecret
//            );
//
//            if (!isValid) {
//                throw new ResponseStatusException(
//                        HttpStatus.BAD_REQUEST,
//                        "Invalid Razorpay payment signature"
//                );
//            }
//
//            // Update donation record
//            donation.setRazorpayPaymentId(request.getRazorpayPaymentId());
//            donation.setPaymentStatus(PaymentStatus.SUCCESS);
//
//            donationRepository.save(donation);
//
//        } catch (RazorpayException e) {
//            throw new IllegalStateException(
//                    "Payment verification failed", e
//            );
//        }
//    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationHistoryResponse> getAllDonations() {

        List<Donation> donations =
                donationRepository.findAllByOrderByCreatedAtDesc();

        return donations.stream()
                .map(donation -> DonationHistoryResponse.builder()

                        .donationId(donation.getId())

                        .donorName(donation.getDonor().getName())

                        .donorEmail(donation.getDonor().getEmail())

                        .donorPhone(donation.getDonor().getPhone())

                        .amount(donation.getAmount())

                        .currency(donation.getCurrency())

                        .razorpayOrderId(donation.getRazorpayOrderId())

                        .razorpayPaymentId(donation.getRazorpayPaymentId())

                        .paymentStatus(donation.getPaymentStatus())

                        .createdAt(donation.getCreatedAt())

                        .build())

                .collect(Collectors.toList());
    }
}
