package com.kashi.portal.booking.service;

import com.kashi.portal.booking.dto.BookingCreationResponse;
import com.kashi.portal.booking.dto.BookingPaymentVerificationRequest;
import com.kashi.portal.booking.dto.BookingRequest;
import com.kashi.portal.booking.dto.BookingResponse;
import com.kashi.portal.booking.entity.Booking;
import com.kashi.portal.booking.entity.BookingStatus;
import com.kashi.portal.booking.entity.PaymentStatus;
import com.kashi.portal.booking.repository.BookingRepository;
import com.kashi.portal.place.BookingType;
import com.kashi.portal.place.Place;
import com.kashi.portal.place.PlaceRepository;
import com.kashi.portal.user.User;
import com.kashi.portal.user.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kashi.portal.common.BadRequestException;
import com.kashi.portal.common.ResourceNotFoundException;
import com.kashi.portal.common.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Override
    @Transactional
    public BookingCreationResponse createBooking(BookingRequest request) {

        // 1. Get authenticated user
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("User is not authenticated");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Find destination
        Place place = placeRepository.findById(request.placeId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found"));

        // 3. Check booking availability
        if (place.getBookingType() == BookingType.PAID
                && place.getPricePerPerson().compareTo(BigDecimal.ZERO) <= 0) {

            throw new BadRequestException(
                    "Paid destination must have a valid price"
            );
        }

        // 4. Calculate total amount on server
        BigDecimal totalAmount;

        if (place.getBookingType() == BookingType.FREE) {
            totalAmount = BigDecimal.ZERO.setScale(2);
        } else {
            totalAmount = place.getPricePerPerson()
                    .multiply(BigDecimal.valueOf(request.visitors()))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 5. Build booking
        Booking.BookingBuilder bookingBuilder = Booking.builder()
                .user(user)
                .place(place)
                .bookingDate(request.bookingDate())
                .visitors(request.visitors())
                .bookingType(place.getBookingType())
                .totalAmount(totalAmount)
                .bookingStatus(BookingStatus.PENDING)
                .specialRequest(request.specialRequest());

        // 6. FREE booking
        if (place.getBookingType() == BookingType.FREE) {

            bookingBuilder.paymentStatus(PaymentStatus.NOT_REQUIRED);

            Booking booking = bookingRepository.save(bookingBuilder.build());

            return new BookingCreationResponse(
                    booking.getId(),
                    place.getName(),
                    booking.getBookingDate(),
                    booking.getVisitors(),
                    booking.getBookingType(),
                    booking.getTotalAmount(),
                    booking.getBookingStatus(),
                    booking.getPaymentStatus(),
                    null,
                    null,
                    null,
                    null
            );
        }

        // 7. PAID booking - Create Razorpay order
        try {
            int amountInPaise = totalAmount
                    .multiply(BigDecimal.valueOf(100))
                    .intValueExact();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "booking_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(orderRequest);

            String orderId = order.get("id");

            bookingBuilder
                    .paymentStatus(PaymentStatus.PENDING)
                    .razorpayOrderId(orderId);

            Booking booking = bookingRepository.save(bookingBuilder.build());

            return new BookingCreationResponse(
                    booking.getId(),
                    place.getName(),
                    booking.getBookingDate(),
                    booking.getVisitors(),
                    booking.getBookingType(),
                    booking.getTotalAmount(),
                    booking.getBookingStatus(),
                    booking.getPaymentStatus(),
                    orderId,
                    razorpayKeyId,
                    "INR",
                    amountInPaise
            );

        } catch (RazorpayException | ArithmeticException exception) {
            throw new IllegalStateException(
                    "Unable to create Razorpay order", exception
            );
        }
    }

    @Override
    @Transactional
    public void verifyPayment(BookingPaymentVerificationRequest request) {

        // 1. Get authenticated user
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("User is not authenticated");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Find booking belonging to logged-in user
        Booking booking = bookingRepository
                .findByIdAndUserId(request.bookingId(), user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // 3. Validate booking payment details
        if (booking.getBookingType() != BookingType.PAID) {
            throw new BadRequestException("This booking does not require payment");
        }

        if (booking.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("Payment already verified");
        }

        System.out.println("DB Order ID: " + booking.getRazorpayOrderId());

        System.out.println("Request Order ID: " + request.razorpayOrderId());

        System.out.println("Match: " +
                booking.getRazorpayOrderId().equals(request.razorpayOrderId()));

        if (!booking.getRazorpayOrderId().equals(request.razorpayOrderId())) {
            throw new BadRequestException("Invalid Razorpay order ID");
        }

        // 4. Verify Razorpay signature
        try {
            String payload = request.razorpayOrderId()
                    + "|" + request.razorpayPaymentId();

            boolean isValid = com.razorpay.Utils.verifySignature(
                    payload,
                    request.razorpaySignature(),
                    razorpayKeySecret
            );

            if (!isValid) {
                throw new BadRequestException("Invalid payment signature");
            }

            // 5. Update booking
            booking.setPaymentStatus(PaymentStatus.SUCCESS);
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            booking.setRazorpayPaymentId(request.razorpayPaymentId());

            bookingRepository.save(booking);

        } catch (RazorpayException exception) {
            throw new IllegalStateException("Payment verification failed", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(Pageable pageable) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return bookingRepository.findByUserId(user.getId(), pageable)
                .map(BookingResponse::from);
    }


}
